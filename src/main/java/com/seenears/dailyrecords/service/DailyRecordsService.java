package com.seenears.dailyrecords.service;

import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.UserStatus;
import com.seenears.auth.repository.AppUserRepository;
import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.dto.request.CreateDailyRecordRequest;
import com.seenears.dailyrecords.dto.response.CreateDailyRecordResponse;
import com.seenears.dailyrecords.dto.response.DailyRecordDetailResponse;
import com.seenears.dailyrecords.dto.response.MonthlyDailyRecordsResponse;
import com.seenears.dailyrecords.repository.DailyRecordRepository;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.letters.domain.Letter;
import com.seenears.letters.repository.LetterRepository;
import com.seenears.questions.service.QuestionsService;
import com.seenears.voicerecords.repository.VoiceRecordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DailyRecordsService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalTime CREATION_START_TIME = LocalTime.of(18, 0);
    private static final String DAILY_RECORD_UNIQUE_CONSTRAINT = "uk_daily_records_user_record_date";
    private static final int MIN_QUERY_YEAR = 2000;
    private static final int MAX_QUERY_YEAR = 2100;

    private final AppUserRepository appUserRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final QuestionsService questionsService;
    private final VoiceRecordRepository voiceRecordRepository;
    private final LetterRepository letterRepository;

    public DailyRecordsService(
            AppUserRepository appUserRepository,
            DailyRecordRepository dailyRecordRepository,
            QuestionsService questionsService,
            VoiceRecordRepository voiceRecordRepository,
            LetterRepository letterRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.dailyRecordRepository = dailyRecordRepository;
        this.questionsService = questionsService;
        this.voiceRecordRepository = voiceRecordRepository;
        this.letterRepository = letterRepository;
    }

    @Transactional
    public CreateDailyRecordResponse createDailyRecord(
            String authenticatedUserId,
            CreateDailyRecordRequest request
    ) {
        AppUser appUser = getAuthenticatedUser(authenticatedUserId);
        LocalDate today = LocalDate.now(SERVICE_ZONE);
        validateCreationTime();

        dailyRecordRepository.findByAppUserAndRecordDate(appUser, today)
                .ifPresent(dailyRecord -> {
                    throw new BusinessException(ErrorCode.DAILY_RECORD_ALREADY_EXISTS);
                });

        QuestionsService.QuestionCandidates candidates = questionsService.resolveQuestionCandidates(appUser, today);
        String questionText = candidates.questions().get(request.moodType());

        if (questionText == null || questionText.isBlank()) {
            throw new BusinessException(ErrorCode.DEFAULT_QUESTION_NOT_FOUND);
        }

        DailyRecord dailyRecord = DailyRecord.create(
                appUser,
                today,
                request.moodType(),
                questionText,
                candidates.source()
        );

        try {
            return CreateDailyRecordResponse.from(dailyRecordRepository.saveAndFlush(dailyRecord));
        } catch (DataIntegrityViolationException exception) {
            if (isDailyRecordDuplicateCreation(exception)) {
                throw new BusinessException(ErrorCode.DAILY_RECORD_ALREADY_EXISTS);
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public DailyRecordDetailResponse getDailyRecordDetail(
            String authenticatedUserId,
            Long dailyRecordId
    ) {
        AppUser appUser = getAuthenticatedUser(authenticatedUserId);
        DailyRecord dailyRecord = dailyRecordRepository.findById(dailyRecordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DAILY_RECORD_NOT_FOUND));

        validateOwner(appUser, dailyRecord);

        boolean hasVoice = voiceRecordRepository.existsByDailyRecord(dailyRecord);
        Letter letter = letterRepository.findByDailyRecordId(dailyRecordId).orElse(null);
        return DailyRecordDetailResponse.of(dailyRecord, hasVoice, letter);
    }

    @Transactional(readOnly = true)
    public MonthlyDailyRecordsResponse getMonthlyDailyRecords(
            String authenticatedUserId,
            int year,
            int month
    ) {
        validateYearMonth(year, month);

        AppUser appUser = getAuthenticatedUser(authenticatedUserId);
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1);

        List<DailyRecord> dailyRecords =
                dailyRecordRepository.findByAppUserAndRecordDateGreaterThanEqualAndRecordDateLessThanOrderByRecordDateAsc(
                        appUser,
                        startDate,
                        endDate
                );

        Set<Long> voiceSubmittedDailyRecordIds = dailyRecords.isEmpty()
                ? Collections.emptySet()
                : voiceRecordRepository.findDailyRecordIdsByDailyRecordIn(dailyRecords);

        Map<Long, Letter> lettersByDailyRecordId = dailyRecords.isEmpty()
                ? Collections.emptyMap()
                : letterRepository.findByDailyRecordIdIn(
                                dailyRecords.stream()
                                        .map(DailyRecord::getId)
                                        .toList()
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                letter -> letter.getDailyRecord().getId(),
                                Function.identity()
                        ));

        return MonthlyDailyRecordsResponse.of(
                year,
                month,
                dailyRecords,
                voiceSubmittedDailyRecordIds,
                lettersByDailyRecordId
        );
    }

    private void validateYearMonth(int year, int month) {
        if (year < MIN_QUERY_YEAR || year > MAX_QUERY_YEAR || month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void validateOwner(AppUser appUser, DailyRecord dailyRecord) {
        if (!dailyRecord.getAppUser().getId().equals(appUser.getId())) {
            throw new BusinessException(ErrorCode.DAILY_RECORD_ACCESS_DENIED);
        }
    }

    private void validateCreationTime() {
        LocalTime now = LocalTime.now(SERVICE_ZONE);
        if (now.isBefore(CREATION_START_TIME)) {
            throw new BusinessException(ErrorCode.DAILY_RECORD_TIME_NOT_ALLOWED);
        }
    }

    private boolean isDailyRecordDuplicateCreation(DataIntegrityViolationException exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && isDailyRecordUniqueConstraintMessage(message)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isDailyRecordUniqueConstraintMessage(String message) {
        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        return normalizedMessage.contains(DAILY_RECORD_UNIQUE_CONSTRAINT)
                || (normalizedMessage.contains("daily_records")
                && normalizedMessage.contains("user_id")
                && normalizedMessage.contains("record_date"));
    }

    private AppUser getAuthenticatedUser(String authenticatedUserId) {
        Long userId = parseUserId(authenticatedUserId);
        AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (appUser.getStatus() == UserStatus.WITHDRAW_REQUESTED) {
            throw new BusinessException(ErrorCode.USER_WITHDRAW_REQUESTED);
        }

        if (appUser.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return appUser;
    }

    private Long parseUserId(String authenticatedUserId) {
        try {
            return Long.valueOf(authenticatedUserId);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
