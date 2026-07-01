package com.seenears.dailyrecords.service;

import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.UserStatus;
import com.seenears.auth.repository.AppUserRepository;
import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.dto.request.CreateDailyRecordRequest;
import com.seenears.dailyrecords.dto.response.CreateDailyRecordResponse;
import com.seenears.dailyrecords.dto.response.DailyRecordDetailResponse;
import com.seenears.dailyrecords.repository.DailyRecordRepository;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.questions.service.QuestionsService;
import com.seenears.voicerecords.repository.VoiceRecordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;

@Service
public class DailyRecordsService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalTime CREATION_START_TIME = LocalTime.of(18, 0);
    private static final String DAILY_RECORD_UNIQUE_CONSTRAINT = "uk_daily_records_user_record_date";

    private final AppUserRepository appUserRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final QuestionsService questionsService;
    private final VoiceRecordRepository voiceRecordRepository;

    public DailyRecordsService(
            AppUserRepository appUserRepository,
            DailyRecordRepository dailyRecordRepository,
            QuestionsService questionsService,
            VoiceRecordRepository voiceRecordRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.dailyRecordRepository = dailyRecordRepository;
        this.questionsService = questionsService;
        this.voiceRecordRepository = voiceRecordRepository;
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
        return DailyRecordDetailResponse.of(dailyRecord, hasVoice);
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
