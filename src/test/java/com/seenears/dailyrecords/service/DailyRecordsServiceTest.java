package com.seenears.dailyrecords.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.UserStatus;
import com.seenears.auth.repository.AppUserRepository;
import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.domain.QuestionSource;
import com.seenears.dailyrecords.dto.response.DailyRecordDetailResponse;
import com.seenears.dailyrecords.repository.DailyRecordRepository;
import com.seenears.global.domain.MoodType;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.questions.service.QuestionsService;
import com.seenears.voicerecords.repository.VoiceRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DailyRecordsServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long DAILY_RECORD_ID = 10L;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private DailyRecordRepository dailyRecordRepository;

    @Mock
    private QuestionsService questionsService;

    @Mock
    private VoiceRecordRepository voiceRecordRepository;

    private DailyRecordsService dailyRecordsService;

    @BeforeEach
    void setUp() {
        dailyRecordsService = new DailyRecordsService(
                appUserRepository,
                dailyRecordRepository,
                questionsService,
                voiceRecordRepository
        );
    }

    @Test
    void getDailyRecordDetailReturnsMyRecord() {
        AppUser appUser = appUser(USER_ID);
        DailyRecord dailyRecord = dailyRecord(DAILY_RECORD_ID, appUser);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(voiceRecordRepository.existsByDailyRecord(dailyRecord)).willReturn(true);

        DailyRecordDetailResponse response = dailyRecordsService.getDailyRecordDetail(
                String.valueOf(USER_ID),
                DAILY_RECORD_ID
        );

        assertThat(response.dailyRecordId()).isEqualTo(DAILY_RECORD_ID);
        assertThat(response.recordDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.moodType()).isEqualTo(MoodType.SUNNY);
        assertThat(response.questionText()).isEqualTo("오늘 기분이 좋으셨던 이유가 있을까요?");
        assertThat(response.status()).isEqualTo(dailyRecord.getStatus());
        assertThat(response.hasVoice()).isTrue();
        assertThat(response.letter()).isNull();
    }

    @Test
    void getDailyRecordDetailReturnsHasVoiceFalseWhenVoiceRecordDoesNotExist() {
        AppUser appUser = appUser(USER_ID);
        DailyRecord dailyRecord = dailyRecord(DAILY_RECORD_ID, appUser);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(voiceRecordRepository.existsByDailyRecord(dailyRecord)).willReturn(false);

        DailyRecordDetailResponse response = dailyRecordsService.getDailyRecordDetail(
                String.valueOf(USER_ID),
                DAILY_RECORD_ID
        );

        assertThat(response.hasVoice()).isFalse();
    }

    @Test
    void getDailyRecordDetailThrowsNotFoundWhenDailyRecordDoesNotExist() {
        AppUser appUser = appUser(USER_ID);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> dailyRecordsService.getDailyRecordDetail(
                String.valueOf(USER_ID),
                DAILY_RECORD_ID
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DAILY_RECORD_NOT_FOUND);
    }

    @Test
    void getDailyRecordDetailThrowsAccessDeniedWhenRecordOwnerIsDifferent() {
        AppUser appUser = appUser(USER_ID);
        AppUser otherUser = appUser(OTHER_USER_ID);
        DailyRecord dailyRecord = dailyRecord(DAILY_RECORD_ID, otherUser);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));

        assertThatThrownBy(() -> dailyRecordsService.getDailyRecordDetail(
                String.valueOf(USER_ID),
                DAILY_RECORD_ID
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DAILY_RECORD_ACCESS_DENIED);
    }

    @Test
    void dailyRecordDetailResponseDoesNotExposeVoiceDetailFields() throws JsonProcessingException {
        AppUser appUser = appUser(USER_ID);
        DailyRecord dailyRecord = dailyRecord(DAILY_RECORD_ID, appUser);
        DailyRecordDetailResponse response = DailyRecordDetailResponse.of(dailyRecord, true);

        String json = new ObjectMapper()
                .findAndRegisterModules()
                .writeValueAsString(response);

        assertThat(json).contains("\"hasVoice\":true");
        assertThat(json).doesNotContain("questionUsed");
        assertThat(json).doesNotContain("audioUrl");
        assertThat(json).doesNotContain("sttText");
        assertThat(json).doesNotContain("voiceRecord");
    }

    private AppUser appUser(Long id) {
        AppUser appUser = new AppUser("테스터", "01000000000" + id, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(appUser, "id", id);
        return appUser;
    }

    private DailyRecord dailyRecord(Long id, AppUser appUser) {
        DailyRecord dailyRecord = DailyRecord.create(
                appUser,
                LocalDate.of(2026, 7, 1),
                MoodType.SUNNY,
                "오늘 기분이 좋으셨던 이유가 있을까요?",
                QuestionSource.DEFAULT
        );
        ReflectionTestUtils.setField(dailyRecord, "id", id);
        return dailyRecord;
    }
}
