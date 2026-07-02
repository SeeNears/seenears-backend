package com.seenears.dailyrecords.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.UserStatus;
import com.seenears.auth.repository.AppUserRepository;
import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.domain.QuestionSource;
import com.seenears.dailyrecords.dto.response.DailyRecordDetailResponse;
import com.seenears.dailyrecords.dto.response.MonthlyDailyRecordsResponse;
import com.seenears.dailyrecords.repository.DailyRecordRepository;
import com.seenears.global.domain.MoodType;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.letters.domain.Letter;
import com.seenears.letters.domain.LetterStatus;
import com.seenears.letters.repository.LetterRepository;
import com.seenears.questions.service.QuestionsService;
import com.seenears.voicerecords.repository.VoiceRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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

    @Mock
    private LetterRepository letterRepository;

    private DailyRecordsService dailyRecordsService;

    @BeforeEach
    void setUp() {
        dailyRecordsService = new DailyRecordsService(
                appUserRepository,
                dailyRecordRepository,
                questionsService,
                voiceRecordRepository,
                letterRepository
        );
    }

    @Test
    void getDailyRecordDetailReturnsMyRecord() {
        AppUser appUser = appUser(USER_ID);
        DailyRecord dailyRecord = dailyRecord(DAILY_RECORD_ID, appUser);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(voiceRecordRepository.existsByDailyRecord(dailyRecord)).willReturn(true);
        given(letterRepository.findByDailyRecordId(DAILY_RECORD_ID)).willReturn(Optional.empty());

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
    void getDailyRecordDetailReturnsLetterWhenLetterExists() {
        AppUser appUser = appUser(USER_ID);
        DailyRecord dailyRecord = dailyRecord(DAILY_RECORD_ID, appUser);
        Letter letter = generatedLetter(20L, dailyRecord, "오늘도 잘 견뎌낸 하루였어요.", true);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(voiceRecordRepository.existsByDailyRecord(dailyRecord)).willReturn(true);
        given(letterRepository.findByDailyRecordId(DAILY_RECORD_ID)).willReturn(Optional.of(letter));

        DailyRecordDetailResponse response = dailyRecordsService.getDailyRecordDetail(
                String.valueOf(USER_ID),
                DAILY_RECORD_ID
        );

        assertThat(response.letter()).isNotNull();
        assertThat(response.letter().letterId()).isEqualTo(20L);
        assertThat(response.letter().status()).isEqualTo(LetterStatus.GENERATED.name());
        assertThat(response.letter().content()).isEqualTo("오늘도 잘 견뎌낸 하루였어요.");
        assertThat(response.letter().isRead()).isTrue();
        assertThat(response.letter().fallbackUsed()).isFalse();
    }

    @Test
    void getDailyRecordDetailReturnsHasVoiceFalseWhenVoiceRecordDoesNotExist() {
        AppUser appUser = appUser(USER_ID);
        DailyRecord dailyRecord = dailyRecord(DAILY_RECORD_ID, appUser);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(voiceRecordRepository.existsByDailyRecord(dailyRecord)).willReturn(false);
        given(letterRepository.findByDailyRecordId(DAILY_RECORD_ID)).willReturn(Optional.empty());

        DailyRecordDetailResponse response = dailyRecordsService.getDailyRecordDetail(
                String.valueOf(USER_ID),
                DAILY_RECORD_ID
        );

        assertThat(response.hasVoice()).isFalse();
        assertThat(response.letter()).isNull();
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
        then(letterRepository).should(never()).findByDailyRecordId(DAILY_RECORD_ID);
    }

    @Test
    void dailyRecordDetailResponseDoesNotExposeVoiceDetailFields() throws JsonProcessingException {
        AppUser appUser = appUser(USER_ID);
        DailyRecord dailyRecord = dailyRecord(DAILY_RECORD_ID, appUser);
        DailyRecordDetailResponse response = DailyRecordDetailResponse.of(dailyRecord, true, null);

        String json = new ObjectMapper()
                .findAndRegisterModules()
                .writeValueAsString(response);

        assertThat(json).contains("\"hasVoice\":true");
        assertThat(json).doesNotContain("questionUsed");
        assertThat(json).doesNotContain("audioUrl");
        assertThat(json).doesNotContain("sttText");
        assertThat(json).doesNotContain("voiceRecord");
    }

    @Test
    void getMonthlyDailyRecordsReturnsRecordsOrderedByRecordDate() {
        AppUser appUser = appUser(USER_ID);
        DailyRecord firstRecord = dailyRecord(DAILY_RECORD_ID, appUser, LocalDate.of(2026, 7, 1));
        DailyRecord secondRecord = dailyRecord(DAILY_RECORD_ID + 1, appUser, LocalDate.of(2026, 7, 3));
        secondRecord.submitVoice();
        Letter secondLetter = generatedLetter(21L, secondRecord, "좋은 기억을 오래 간직해 보세요.", false);
        List<DailyRecord> dailyRecords = List.of(firstRecord, secondRecord);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(dailyRecordRepository.findByAppUserAndRecordDateGreaterThanEqualAndRecordDateLessThanOrderByRecordDateAsc(
                appUser,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 1)
        )).willReturn(dailyRecords);
        given(voiceRecordRepository.findDailyRecordIdsByDailyRecordIn(dailyRecords)).willReturn(Set.of(secondRecord.getId()));
        given(letterRepository.findByDailyRecordIdIn(List.of(firstRecord.getId(), secondRecord.getId())))
                .willReturn(List.of(secondLetter));

        MonthlyDailyRecordsResponse response = dailyRecordsService.getMonthlyDailyRecords(
                String.valueOf(USER_ID),
                2026,
                7
        );

        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.month()).isEqualTo(7);
        assertThat(response.records()).hasSize(2);
        assertThat(response.records()).extracting(MonthlyDailyRecordsResponse.RecordResponse::recordDate)
                .containsExactly(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3));
        assertThat(response.records().get(0).hasVoice()).isFalse();
        assertThat(response.records().get(1).hasVoice()).isTrue();
        assertThat(response.records().get(1).status()).isEqualTo(secondRecord.getStatus());
        assertThat(response.records().get(0).hasLetter()).isFalse();
        assertThat(response.records().get(0).letterId()).isNull();
        assertThat(response.records().get(0).letterStatus()).isNull();
        assertThat(response.records().get(0).letterRead()).isNull();
        assertThat(response.records().get(1).hasLetter()).isTrue();
        assertThat(response.records().get(1).letterId()).isEqualTo(21L);
        assertThat(response.records().get(1).letterStatus()).isEqualTo(LetterStatus.GENERATED.name());
        assertThat(response.records().get(1).letterRead()).isFalse();
        then(letterRepository).should().findByDailyRecordIdIn(List.of(firstRecord.getId(), secondRecord.getId()));
        then(letterRepository).should(never()).findByDailyRecordId(firstRecord.getId());
        then(letterRepository).should(never()).findByDailyRecordId(secondRecord.getId());
    }

    @Test
    void getMonthlyDailyRecordsReturnsEmptyRecordsWhenMonthlyRecordDoesNotExist() {
        AppUser appUser = appUser(USER_ID);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(dailyRecordRepository.findByAppUserAndRecordDateGreaterThanEqualAndRecordDateLessThanOrderByRecordDateAsc(
                appUser,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 1)
        )).willReturn(List.of());

        MonthlyDailyRecordsResponse response = dailyRecordsService.getMonthlyDailyRecords(
                String.valueOf(USER_ID),
                2026,
                7
        );

        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.month()).isEqualTo(7);
        assertThat(response.records()).isEmpty();
        then(voiceRecordRepository).should(never()).findDailyRecordIdsByDailyRecordIn(List.of());
        then(letterRepository).should(never()).findByDailyRecordIdIn(anyCollection());
    }

    @Test
    void getMonthlyDailyRecordsThrowsInvalidInputWhenMonthIsInvalid() {
        assertThatThrownBy(() -> dailyRecordsService.getMonthlyDailyRecords(
                String.valueOf(USER_ID),
                2026,
                13
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void getMonthlyDailyRecordsQueriesOnlyAuthenticatedUsersRecords() {
        AppUser appUser = appUser(USER_ID);
        given(appUserRepository.findById(USER_ID)).willReturn(Optional.of(appUser));
        given(dailyRecordRepository.findByAppUserAndRecordDateGreaterThanEqualAndRecordDateLessThanOrderByRecordDateAsc(
                appUser,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 1)
        )).willReturn(List.of());

        dailyRecordsService.getMonthlyDailyRecords(String.valueOf(USER_ID), 2026, 7);

        then(dailyRecordRepository).should()
                .findByAppUserAndRecordDateGreaterThanEqualAndRecordDateLessThanOrderByRecordDateAsc(
                        appUser,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 8, 1)
                );
    }

    @Test
    void monthlyDailyRecordsResponseDoesNotExposeDetailFields() throws JsonProcessingException {
        AppUser appUser = appUser(USER_ID);
        DailyRecord dailyRecord = dailyRecord(DAILY_RECORD_ID, appUser);
        MonthlyDailyRecordsResponse response = MonthlyDailyRecordsResponse.of(
                2026,
                7,
                List.of(dailyRecord),
                Set.of(),
                Map.of()
        );

        String json = new ObjectMapper()
                .findAndRegisterModules()
                .writeValueAsString(response);

        assertThat(json).contains("\"hasVoice\":false");
        assertThat(json).contains("\"hasLetter\":false");
        assertThat(json).doesNotContain("questionText");
        assertThat(json).doesNotContain("questionUsed");
        assertThat(json).doesNotContain("audioUrl");
        assertThat(json).doesNotContain("sttText");
        assertThat(json).doesNotContain("voiceRecord");
        assertThat(json).doesNotContain("letterContent");
    }

    private AppUser appUser(Long id) {
        AppUser appUser = new AppUser("테스터", "01000000000" + id, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(appUser, "id", id);
        return appUser;
    }

    private DailyRecord dailyRecord(Long id, AppUser appUser) {
        return dailyRecord(id, appUser, LocalDate.of(2026, 7, 1));
    }

    private DailyRecord dailyRecord(Long id, AppUser appUser, LocalDate recordDate) {
        DailyRecord dailyRecord = DailyRecord.create(
                appUser,
                recordDate,
                MoodType.SUNNY,
                "오늘 기분이 좋으셨던 이유가 있을까요?",
                QuestionSource.DEFAULT
        );
        ReflectionTestUtils.setField(dailyRecord, "id", id);
        return dailyRecord;
    }

    private Letter generatedLetter(Long id, DailyRecord dailyRecord, String content, boolean read) {
        Letter letter = Letter.create(dailyRecord);
        ReflectionTestUtils.setField(letter, "id", id);
        letter.saveGenerated(content, false);
        ReflectionTestUtils.setField(letter, "read", read);
        return letter;
    }
}
