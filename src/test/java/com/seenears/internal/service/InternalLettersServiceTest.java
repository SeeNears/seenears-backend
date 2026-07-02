package com.seenears.internal.service;

import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.UserStatus;
import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.domain.QuestionSource;
import com.seenears.global.domain.MoodType;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.internal.dto.request.SaveLetterResultRequest;
import com.seenears.internal.dto.response.SaveLetterResultResponse;
import com.seenears.letters.domain.Letter;
import com.seenears.letters.domain.LetterStatus;
import com.seenears.letters.repository.LetterRepository;
import com.seenears.voicerecords.domain.VoiceRecord;
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
class InternalLettersServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long DAILY_RECORD_ID = 10L;
    private static final Long OTHER_DAILY_RECORD_ID = 11L;
    private static final Long VOICE_RECORD_ID = 5L;
    private static final Long LETTER_ID = 2L;

    @Mock
    private LetterRepository letterRepository;

    @Mock
    private VoiceRecordRepository voiceRecordRepository;

    private InternalLettersService internalLettersService;

    @BeforeEach
    void setUp() {
        internalLettersService = new InternalLettersService(letterRepository, voiceRecordRepository);
    }

    @Test
    void saveLetterResultStoresGenerated() {
        Letter letter = pendingLetter();
        VoiceRecord voiceRecord = voiceRecord(letter.getDailyRecord(), VOICE_RECORD_ID);
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));
        given(voiceRecordRepository.findById(VOICE_RECORD_ID)).willReturn(Optional.of(voiceRecord));

        SaveLetterResultResponse response = internalLettersService.saveLetterResult(
                LETTER_ID,
                new SaveLetterResultRequest(
                        VOICE_RECORD_ID,
                        LetterStatus.GENERATED,
                        " 편지 본문 ",
                        false
                )
        );

        assertThat(response.letterId()).isEqualTo(LETTER_ID);
        assertThat(response.voiceRecordId()).isEqualTo(VOICE_RECORD_ID);
        assertThat(response.status()).isEqualTo(LetterStatus.GENERATED);
        assertThat(response.fallbackUsed()).isFalse();
        assertThat(response.generatedAt()).isNotNull();
        assertThat(letter.getContent()).isEqualTo("편지 본문");
        assertThat(letter.getStatus()).isEqualTo(LetterStatus.GENERATED);
        assertThat(letter.isFallbackUsed()).isFalse();
    }

    @Test
    void saveLetterResultStoresFallbackGenerated() {
        Letter letter = pendingLetter();
        VoiceRecord voiceRecord = voiceRecord(letter.getDailyRecord(), VOICE_RECORD_ID);
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));
        given(voiceRecordRepository.findById(VOICE_RECORD_ID)).willReturn(Optional.of(voiceRecord));

        SaveLetterResultResponse response = internalLettersService.saveLetterResult(
                LETTER_ID,
                new SaveLetterResultRequest(
                        VOICE_RECORD_ID,
                        LetterStatus.FALLBACK_GENERATED,
                        " 대체 편지 본문 ",
                        true
                )
        );

        assertThat(response.status()).isEqualTo(LetterStatus.FALLBACK_GENERATED);
        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.generatedAt()).isNotNull();
        assertThat(letter.getContent()).isEqualTo("대체 편지 본문");
        assertThat(letter.getStatus()).isEqualTo(LetterStatus.FALLBACK_GENERATED);
        assertThat(letter.isFallbackUsed()).isTrue();
    }

    @Test
    void saveLetterResultStoresFailedWithNullContent() {
        Letter letter = pendingLetter();
        VoiceRecord voiceRecord = voiceRecord(letter.getDailyRecord(), VOICE_RECORD_ID);
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));
        given(voiceRecordRepository.findById(VOICE_RECORD_ID)).willReturn(Optional.of(voiceRecord));

        SaveLetterResultResponse response = internalLettersService.saveLetterResult(
                LETTER_ID,
                new SaveLetterResultRequest(VOICE_RECORD_ID, LetterStatus.FAILED, null, false)
        );

        assertThat(response.status()).isEqualTo(LetterStatus.FAILED);
        assertThat(response.fallbackUsed()).isFalse();
        assertThat(response.generatedAt()).isNotNull();
        assertThat(letter.getContent()).isNull();
        assertThat(letter.getStatus()).isEqualTo(LetterStatus.FAILED);
        assertThat(letter.isFallbackUsed()).isFalse();
    }

    @Test
    void saveLetterResultThrowsNotFoundWhenLetterDoesNotExist() {
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> internalLettersService.saveLetterResult(LETTER_ID, generatedRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LETTER_NOT_FOUND);
    }

    @Test
    void saveLetterResultThrowsNotFoundWhenVoiceRecordDoesNotExist() {
        Letter letter = pendingLetter();
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));
        given(voiceRecordRepository.findById(VOICE_RECORD_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> internalLettersService.saveLetterResult(LETTER_ID, generatedRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VOICE_RECORD_NOT_FOUND);
    }

    @Test
    void saveLetterResultThrowsConflictWhenDailyRecordDoesNotMatch() {
        Letter letter = pendingLetter();
        DailyRecord otherDailyRecord = dailyRecord(OTHER_DAILY_RECORD_ID);
        VoiceRecord voiceRecord = voiceRecord(otherDailyRecord, VOICE_RECORD_ID);
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));
        given(voiceRecordRepository.findById(VOICE_RECORD_ID)).willReturn(Optional.of(voiceRecord));

        assertThatThrownBy(() -> internalLettersService.saveLetterResult(LETTER_ID, generatedRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LETTER_VOICE_RECORD_MISMATCH);
    }

    @Test
    void saveLetterResultThrowsInvalidInputWhenGeneratedContentIsNullOrBlank() {
        assertInvalidInput(new SaveLetterResultRequest(VOICE_RECORD_ID, LetterStatus.GENERATED, null, false));
        assertInvalidInput(new SaveLetterResultRequest(VOICE_RECORD_ID, LetterStatus.GENERATED, " ", false));
    }

    @Test
    void saveLetterResultThrowsInvalidInputWhenFallbackGeneratedContentIsNullOrBlank() {
        assertInvalidInput(new SaveLetterResultRequest(VOICE_RECORD_ID, LetterStatus.FALLBACK_GENERATED, null, true));
        assertInvalidInput(new SaveLetterResultRequest(VOICE_RECORD_ID, LetterStatus.FALLBACK_GENERATED, " ", true));
    }

    @Test
    void saveLetterResultThrowsInvalidInputWhenGeneratedFallbackUsedIsTrue() {
        assertInvalidInput(new SaveLetterResultRequest(VOICE_RECORD_ID, LetterStatus.GENERATED, "본문", true));
    }

    @Test
    void saveLetterResultThrowsInvalidInputWhenFallbackGeneratedFallbackUsedIsFalse() {
        assertInvalidInput(new SaveLetterResultRequest(
                VOICE_RECORD_ID,
                LetterStatus.FALLBACK_GENERATED,
                "본문",
                false
        ));
    }

    @Test
    void saveLetterResultThrowsInvalidInputWhenFailedFallbackUsedIsTrue() {
        assertInvalidInput(new SaveLetterResultRequest(VOICE_RECORD_ID, LetterStatus.FAILED, null, true));
    }

    @Test
    void saveLetterResultThrowsConflictWhenLetterAlreadyGenerated() {
        Letter letter = generatedLetter();
        VoiceRecord voiceRecord = voiceRecord(letter.getDailyRecord(), VOICE_RECORD_ID);
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));
        given(voiceRecordRepository.findById(VOICE_RECORD_ID)).willReturn(Optional.of(voiceRecord));

        assertThatThrownBy(() -> internalLettersService.saveLetterResult(LETTER_ID, failedRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LETTER_ALREADY_GENERATED);
    }

    @Test
    void saveLetterResultThrowsConflictWhenLetterAlreadyFallbackGenerated() {
        Letter letter = fallbackGeneratedLetter();
        VoiceRecord voiceRecord = voiceRecord(letter.getDailyRecord(), VOICE_RECORD_ID);
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));
        given(voiceRecordRepository.findById(VOICE_RECORD_ID)).willReturn(Optional.of(voiceRecord));

        assertThatThrownBy(() -> internalLettersService.saveLetterResult(LETTER_ID, failedRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LETTER_ALREADY_GENERATED);
    }

    @Test
    void saveLetterResultAllowsFailedToGeneratedRetry() {
        Letter letter = failedLetter();
        VoiceRecord voiceRecord = voiceRecord(letter.getDailyRecord(), VOICE_RECORD_ID);
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));
        given(voiceRecordRepository.findById(VOICE_RECORD_ID)).willReturn(Optional.of(voiceRecord));

        SaveLetterResultResponse response = internalLettersService.saveLetterResult(LETTER_ID, generatedRequest());

        assertThat(response.status()).isEqualTo(LetterStatus.GENERATED);
        assertThat(letter.getStatus()).isEqualTo(LetterStatus.GENERATED);
        assertThat(letter.getContent()).isEqualTo("본문");
    }

    @Test
    void saveLetterResultAllowsFailedToFallbackGeneratedRetry() {
        Letter letter = failedLetter();
        VoiceRecord voiceRecord = voiceRecord(letter.getDailyRecord(), VOICE_RECORD_ID);
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));
        given(voiceRecordRepository.findById(VOICE_RECORD_ID)).willReturn(Optional.of(voiceRecord));

        SaveLetterResultResponse response = internalLettersService.saveLetterResult(
                LETTER_ID,
                new SaveLetterResultRequest(VOICE_RECORD_ID, LetterStatus.FALLBACK_GENERATED, "본문", true)
        );

        assertThat(response.status()).isEqualTo(LetterStatus.FALLBACK_GENERATED);
        assertThat(letter.getStatus()).isEqualTo(LetterStatus.FALLBACK_GENERATED);
        assertThat(letter.isFallbackUsed()).isTrue();
    }

    @Test
    void saveLetterResultAllowsFailedToFailedAgain() {
        Letter letter = failedLetter();
        VoiceRecord voiceRecord = voiceRecord(letter.getDailyRecord(), VOICE_RECORD_ID);
        given(letterRepository.findById(LETTER_ID)).willReturn(Optional.of(letter));
        given(voiceRecordRepository.findById(VOICE_RECORD_ID)).willReturn(Optional.of(voiceRecord));

        SaveLetterResultResponse response = internalLettersService.saveLetterResult(LETTER_ID, failedRequest());

        assertThat(response.status()).isEqualTo(LetterStatus.FAILED);
        assertThat(letter.getStatus()).isEqualTo(LetterStatus.FAILED);
        assertThat(letter.getContent()).isNull();
        assertThat(letter.isFallbackUsed()).isFalse();
    }

    private void assertInvalidInput(SaveLetterResultRequest request) {
        assertThatThrownBy(() -> internalLettersService.saveLetterResult(LETTER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    private SaveLetterResultRequest generatedRequest() {
        return new SaveLetterResultRequest(VOICE_RECORD_ID, LetterStatus.GENERATED, "본문", false);
    }

    private SaveLetterResultRequest failedRequest() {
        return new SaveLetterResultRequest(VOICE_RECORD_ID, LetterStatus.FAILED, null, false);
    }

    private Letter pendingLetter() {
        Letter letter = Letter.create(dailyRecord(DAILY_RECORD_ID));
        ReflectionTestUtils.setField(letter, "id", LETTER_ID);
        return letter;
    }

    private Letter generatedLetter() {
        Letter letter = pendingLetter();
        letter.saveGenerated("이전 본문", false);
        return letter;
    }

    private Letter fallbackGeneratedLetter() {
        Letter letter = pendingLetter();
        letter.saveGenerated("이전 대체 본문", true);
        return letter;
    }

    private Letter failedLetter() {
        Letter letter = pendingLetter();
        letter.saveFailed();
        return letter;
    }

    private VoiceRecord voiceRecord(DailyRecord dailyRecord, Long voiceRecordId) {
        VoiceRecord voiceRecord = VoiceRecord.create(
                dailyRecord,
                dailyRecord.getAppUser(),
                "uploads/voice.aac",
                120
        );
        ReflectionTestUtils.setField(voiceRecord, "id", voiceRecordId);
        return voiceRecord;
    }

    private DailyRecord dailyRecord(Long dailyRecordId) {
        AppUser appUser = new AppUser("테스터", "01000000000", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(appUser, "id", USER_ID);

        DailyRecord dailyRecord = DailyRecord.create(
                appUser,
                LocalDate.of(2026, 7, 1),
                MoodType.SUNNY,
                "오늘 기분이 좋으셨던 이유가 있을까요?",
                QuestionSource.DEFAULT
        );
        ReflectionTestUtils.setField(dailyRecord, "id", dailyRecordId);
        dailyRecord.submitVoice();
        return dailyRecord;
    }
}
