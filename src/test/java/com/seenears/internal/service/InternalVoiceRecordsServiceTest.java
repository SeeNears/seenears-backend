package com.seenears.internal.service;

import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.UserStatus;
import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.domain.DailyRecordStatus;
import com.seenears.dailyrecords.domain.QuestionSource;
import com.seenears.global.domain.MoodType;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.internal.dto.request.SaveSttResultRequest;
import com.seenears.internal.dto.response.PendingSttVoiceRecordsResponse;
import com.seenears.internal.dto.response.SaveSttResultResponse;
import com.seenears.letters.domain.Letter;
import com.seenears.letters.domain.LetterStatus;
import com.seenears.letters.repository.LetterRepository;
import com.seenears.voicerecords.domain.SttStatus;
import com.seenears.voicerecords.domain.VoiceRecord;
import com.seenears.voicerecords.repository.VoiceRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InternalVoiceRecordsServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long DAILY_RECORD_ID = 10L;
    private static final Long VOICE_RECORD_ID = 5L;
    private static final Long LETTER_ID = 2L;

    @Mock
    private VoiceRecordRepository voiceRecordRepository;

    @Mock
    private LetterRepository letterRepository;

    private InternalVoiceRecordsService internalVoiceRecordsService;

    @BeforeEach
    void setUp() {
        internalVoiceRecordsService = new InternalVoiceRecordsService(voiceRecordRepository, letterRepository);
    }

    @Test
    void getPendingSttVoiceRecordsQueriesPendingAndVoiceSubmittedWithDefaultLimit() {
        VoiceRecord voiceRecord = voiceRecord(SttStatus.PENDING, true);
        given(voiceRecordRepository.findPendingSttVoiceRecords(
                eq(SttStatus.PENDING),
                eq(DailyRecordStatus.VOICE_SUBMITTED),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).willReturn(List.of(voiceRecord));
        given(letterRepository.findByDailyRecordIdIn(List.of(DAILY_RECORD_ID)))
                .willReturn(List.of(letter(voiceRecord.getDailyRecord())));

        PendingSttVoiceRecordsResponse response = internalVoiceRecordsService.getPendingSttVoiceRecords(10);

        assertThat(response.voiceRecords()).hasSize(1);
        assertThat(response.voiceRecords().get(0).voiceRecordId()).isEqualTo(VOICE_RECORD_ID);
        assertThat(response.voiceRecords().get(0).dailyRecordId()).isEqualTo(DAILY_RECORD_ID);
        assertThat(response.voiceRecords().get(0).userId()).isEqualTo(USER_ID);
        assertThat(response.voiceRecords().get(0).audioUrl()).isEqualTo("uploads/voice.aac");
        assertThat(response.voiceRecords().get(0).durationSeconds()).isEqualTo(120);
        assertThat(response.voiceRecords().get(0).recordDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.voiceRecords().get(0).moodType()).isEqualTo(MoodType.SUNNY);
        assertThat(response.voiceRecords().get(0).questionText()).isEqualTo("오늘 기분이 좋으셨던 이유가 있을까요?");
        assertThat(response.voiceRecords().get(0).letterId()).isEqualTo(LETTER_ID);
        assertThat(response.voiceRecords().get(0).letterStatus()).isEqualTo(LetterStatus.PENDING);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(voiceRecordRepository).findPendingSttVoiceRecords(
                eq(SttStatus.PENDING),
                eq(DailyRecordStatus.VOICE_SUBMITTED),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void getPendingSttVoiceRecordsReturnsNullLetterFieldsWhenLetterDoesNotExist() {
        VoiceRecord voiceRecord = voiceRecord(SttStatus.PENDING, true);
        given(voiceRecordRepository.findPendingSttVoiceRecords(
                eq(SttStatus.PENDING),
                eq(DailyRecordStatus.VOICE_SUBMITTED),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).willReturn(List.of(voiceRecord));
        given(letterRepository.findByDailyRecordIdIn(List.of(DAILY_RECORD_ID)))
                .willReturn(List.of());

        PendingSttVoiceRecordsResponse response = internalVoiceRecordsService.getPendingSttVoiceRecords(10);

        assertThat(response.voiceRecords()).hasSize(1);
        assertThat(response.voiceRecords().get(0).voiceRecordId()).isEqualTo(VOICE_RECORD_ID);
        assertThat(response.voiceRecords().get(0).dailyRecordId()).isEqualTo(DAILY_RECORD_ID);
        assertThat(response.voiceRecords().get(0).letterId()).isNull();
        assertThat(response.voiceRecords().get(0).letterStatus()).isNull();
    }

    @Test
    void getPendingSttVoiceRecordsThrowsInvalidInputWhenLimitIsTooSmall() {
        assertThatThrownBy(() -> internalVoiceRecordsService.getPendingSttVoiceRecords(0))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void getPendingSttVoiceRecordsThrowsInvalidInputWhenLimitIsTooLarge() {
        assertThatThrownBy(() -> internalVoiceRecordsService.getPendingSttVoiceRecords(51))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void saveSttResultStoresSuccess() {
        VoiceRecord voiceRecord = voiceRecord(SttStatus.PENDING, true);
        given(voiceRecordRepository.findById(VOICE_RECORD_ID)).willReturn(Optional.of(voiceRecord));

        SaveSttResultResponse response = internalVoiceRecordsService.saveSttResult(
                VOICE_RECORD_ID,
                new SaveSttResultRequest(SttStatus.SUCCESS, " 오늘은 산책을 다녀왔습니다. ")
        );

        assertThat(response.voiceRecordId()).isEqualTo(VOICE_RECORD_ID);
        assertThat(response.sttStatus()).isEqualTo(SttStatus.SUCCESS);
        assertThat(voiceRecord.getSttStatus()).isEqualTo(SttStatus.SUCCESS);
        assertThat(voiceRecord.getSttText()).isEqualTo("오늘은 산책을 다녀왔습니다.");
    }

    @Test
    void saveSttResultThrowsInvalidInputWhenSuccessTextIsBlank() {
        assertThatThrownBy(() -> internalVoiceRecordsService.saveSttResult(
                VOICE_RECORD_ID,
                new SaveSttResultRequest(SttStatus.SUCCESS, " ")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void saveSttResultStoresFailureAndClearsText() {
        VoiceRecord voiceRecord = voiceRecord(SttStatus.PENDING, true);
        voiceRecord.saveSttSuccess("이전 STT");
        ReflectionTestUtils.setField(voiceRecord, "sttStatus", SttStatus.PENDING);
        given(voiceRecordRepository.findById(VOICE_RECORD_ID)).willReturn(Optional.of(voiceRecord));

        SaveSttResultResponse response = internalVoiceRecordsService.saveSttResult(
                VOICE_RECORD_ID,
                new SaveSttResultRequest(SttStatus.FAILED, null)
        );

        assertThat(response.sttStatus()).isEqualTo(SttStatus.FAILED);
        assertThat(voiceRecord.getSttStatus()).isEqualTo(SttStatus.FAILED);
        assertThat(voiceRecord.getSttText()).isNull();
    }

    @Test
    void saveSttResultThrowsNotFoundWhenVoiceRecordDoesNotExist() {
        given(voiceRecordRepository.findById(VOICE_RECORD_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> internalVoiceRecordsService.saveSttResult(
                VOICE_RECORD_ID,
                new SaveSttResultRequest(SttStatus.FAILED, null)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VOICE_RECORD_NOT_FOUND);
    }

    @Test
    void saveSttResultThrowsConflictWhenAlreadySuccess() {
        VoiceRecord voiceRecord = voiceRecord(SttStatus.SUCCESS, true);
        given(voiceRecordRepository.findById(VOICE_RECORD_ID)).willReturn(Optional.of(voiceRecord));

        assertThatThrownBy(() -> internalVoiceRecordsService.saveSttResult(
                VOICE_RECORD_ID,
                new SaveSttResultRequest(SttStatus.FAILED, null)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VOICE_RECORD_STT_ALREADY_COMPLETED);
    }

    private VoiceRecord voiceRecord(SttStatus sttStatus, boolean voiceSubmitted) {
        AppUser appUser = new AppUser("테스터", "01000000000", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(appUser, "id", USER_ID);

        DailyRecord dailyRecord = DailyRecord.create(
                appUser,
                LocalDate.of(2026, 7, 1),
                MoodType.SUNNY,
                "오늘 기분이 좋으셨던 이유가 있을까요?",
                QuestionSource.DEFAULT
        );
        ReflectionTestUtils.setField(dailyRecord, "id", DAILY_RECORD_ID);
        if (voiceSubmitted) {
            dailyRecord.submitVoice();
        }

        VoiceRecord voiceRecord = VoiceRecord.create(
                dailyRecord,
                appUser,
                "uploads/voice.aac",
                120
        );
        ReflectionTestUtils.setField(voiceRecord, "id", VOICE_RECORD_ID);
        ReflectionTestUtils.setField(voiceRecord, "sttStatus", sttStatus);
        ReflectionTestUtils.setField(voiceRecord, "createdAt", LocalDateTime.of(2026, 7, 1, 18, 10));
        return voiceRecord;
    }

    private Letter letter(DailyRecord dailyRecord) {
        Letter letter = Letter.create(dailyRecord);
        ReflectionTestUtils.setField(letter, "id", LETTER_ID);
        return letter;
    }
}
