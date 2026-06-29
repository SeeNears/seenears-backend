package com.seenears.questions.service;

import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.UserStatus;
import com.seenears.auth.repository.AppUserRepository;
import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.domain.QuestionGenerationStatus;
import com.seenears.dailyrecords.domain.QuestionSource;
import com.seenears.dailyrecords.repository.DailyRecordRepository;
import com.seenears.global.domain.MoodType;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.questions.domain.DefaultQuestion;
import com.seenears.questions.dto.response.TodayQuestionsResponse;
import com.seenears.questions.repository.DefaultQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;

@Service
public class QuestionsService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final AppUserRepository appUserRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final DefaultQuestionRepository defaultQuestionRepository;

    public QuestionsService(
            AppUserRepository appUserRepository,
            DailyRecordRepository dailyRecordRepository,
            DefaultQuestionRepository defaultQuestionRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.dailyRecordRepository = dailyRecordRepository;
        this.defaultQuestionRepository = defaultQuestionRepository;
    }

    @Transactional(readOnly = true)
    public TodayQuestionsResponse getTodayQuestions(String authenticatedUserId) {
        AppUser appUser = getAuthenticatedUser(authenticatedUserId);
        LocalDate today = LocalDate.now(SERVICE_ZONE);

        return dailyRecordRepository.findByAppUserAndRecordDate(appUser, today)
                .map(TodayQuestionsResponse::recorded)
                .orElseGet(() -> getCandidateQuestions(appUser, today));
    }

    private TodayQuestionsResponse getCandidateQuestions(AppUser appUser, LocalDate today) {
        QuestionCandidates candidates = resolveQuestionCandidates(appUser, today);

        if (candidates.source() == QuestionSource.AI) {
            return TodayQuestionsResponse.aiGenerated(candidates.baseRecordDate(), candidates.questions());
        }

        return TodayQuestionsResponse.defaults(candidates.questions());
    }

    @Transactional(readOnly = true)
    public QuestionCandidates resolveQuestionCandidates(AppUser appUser, LocalDate today) {
        return dailyRecordRepository
                .findTopByAppUserAndRecordDateLessThanAndQuestionGenerationStatusOrderByRecordDateDescCreatedAtDesc(
                        appUser,
                        today,
                        QuestionGenerationStatus.SUCCESS
                )
                .filter(DailyRecord::hasAllNextQuestions)
                .map(dailyRecord -> new QuestionCandidates(
                        QuestionSource.AI,
                        dailyRecord.getRecordDate(),
                        dailyRecord.getNextQuestions()
                ))
                .orElseGet(() -> new QuestionCandidates(QuestionSource.DEFAULT, null, getDefaultQuestions()));
    }

    private Map<MoodType, String> getDefaultQuestions() {
        Map<MoodType, String> questions = new EnumMap<>(MoodType.class);

        for (MoodType moodType : MoodType.values()) {
            DefaultQuestion defaultQuestion = defaultQuestionRepository
                    .findFirstByMoodTypeAndActiveTrueOrderByDisplayOrderAscIdAsc(moodType)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DEFAULT_QUESTION_NOT_FOUND));
            questions.put(moodType, defaultQuestion.getQuestionText());
        }

        return questions;
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

    public record QuestionCandidates(
            QuestionSource source,
            LocalDate baseRecordDate,
            Map<MoodType, String> questions
    ) {
    }
}
