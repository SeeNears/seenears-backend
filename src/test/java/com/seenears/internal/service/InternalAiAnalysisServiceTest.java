package com.seenears.internal.service;

import com.seenears.aianalysis.domain.AiAnalysisResult;
import com.seenears.aianalysis.domain.AiAnalysisStatus;
import com.seenears.aianalysis.repository.AiAnalysisResultRepository;
import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.UserStatus;
import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.domain.QuestionGenerationStatus;
import com.seenears.dailyrecords.domain.QuestionSource;
import com.seenears.dailyrecords.repository.DailyRecordRepository;
import com.seenears.global.domain.MoodType;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.internal.dto.request.SaveAiAnalysisResultRequest;
import com.seenears.internal.dto.request.SaveNextQuestionsRequest;
import com.seenears.internal.dto.response.SaveAiAnalysisResultResponse;
import com.seenears.internal.dto.response.SaveNextQuestionsResponse;
import com.seenears.voicerecords.domain.SttStatus;
import com.seenears.voicerecords.domain.VoiceRecord;
import com.seenears.voicerecords.repository.VoiceRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class InternalAiAnalysisServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long DAILY_RECORD_ID = 10L;
    private static final Long VOICE_RECORD_ID = 5L;
    private static final Long AI_ANALYSIS_RESULT_ID = 3L;

    @Mock
    private DailyRecordRepository dailyRecordRepository;

    @Mock
    private VoiceRecordRepository voiceRecordRepository;

    @Mock
    private AiAnalysisResultRepository aiAnalysisResultRepository;

    private InternalAiAnalysisService internalAiAnalysisService;

    @BeforeEach
    void setUp() {
        internalAiAnalysisService = new InternalAiAnalysisService(
                dailyRecordRepository,
                voiceRecordRepository,
                aiAnalysisResultRepository
        );
    }

    @Test
    void saveAiAnalysisResultStoresCompleted() {
        DailyRecord dailyRecord = dailyRecord();
        VoiceRecord voiceRecord = voiceRecord(dailyRecord, SttStatus.SUCCESS);
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(voiceRecordRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.of(voiceRecord));
        given(aiAnalysisResultRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.empty());
        given(aiAnalysisResultRepository.save(any(AiAnalysisResult.class))).willAnswer(invocation -> {
            AiAnalysisResult aiAnalysisResult = invocation.getArgument(0);
            ReflectionTestUtils.setField(aiAnalysisResult, "id", AI_ANALYSIS_RESULT_ID);
            return aiAnalysisResult;
        });

        SaveAiAnalysisResultResponse response = internalAiAnalysisService.saveAiAnalysisResult(
                DAILY_RECORD_ID,
                new SaveAiAnalysisResultRequest(
                        AiAnalysisStatus.COMPLETED,
                        " 산책을 통해 긍정적인 감정을 느낀 하루였습니다. ",
                        List.of(" 산책 ", "기분 좋음", "평온함")
                )
        );

        assertThat(response.aiAnalysisResultId()).isEqualTo(AI_ANALYSIS_RESULT_ID);
        assertThat(response.dailyRecordId()).isEqualTo(DAILY_RECORD_ID);
        assertThat(response.analysisStatus()).isEqualTo(AiAnalysisStatus.COMPLETED);
        assertThat(response.summary()).isEqualTo("산책을 통해 긍정적인 감정을 느낀 하루였습니다.");
        assertThat(response.keywords()).containsExactly("산책", "기분 좋음", "평온함");
        assertThat(response.analyzedAt()).isNotNull();
    }

    @Test
    void saveAiAnalysisResultStoresFailedAndClearsAnalysisFields() {
        DailyRecord dailyRecord = dailyRecord();
        VoiceRecord voiceRecord = voiceRecord(dailyRecord, SttStatus.SUCCESS);
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(voiceRecordRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.of(voiceRecord));
        given(aiAnalysisResultRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.empty());
        given(aiAnalysisResultRepository.save(any(AiAnalysisResult.class))).willAnswer(invocation -> {
            AiAnalysisResult aiAnalysisResult = invocation.getArgument(0);
            ReflectionTestUtils.setField(aiAnalysisResult, "id", AI_ANALYSIS_RESULT_ID);
            return aiAnalysisResult;
        });

        SaveAiAnalysisResultResponse response = internalAiAnalysisService.saveAiAnalysisResult(
                DAILY_RECORD_ID,
                new SaveAiAnalysisResultRequest(
                        AiAnalysisStatus.FAILED,
                        null,
                        List.of()
                )
        );

        assertThat(response.analysisStatus()).isEqualTo(AiAnalysisStatus.FAILED);
        assertThat(response.summary()).isNull();
        assertThat(response.keywords()).isEmpty();
        assertThat(response.analyzedAt()).isNull();
    }

    @Test
    void saveAiAnalysisResultThrowsNotFoundWhenDailyRecordDoesNotExist() {
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> internalAiAnalysisService.saveAiAnalysisResult(
                DAILY_RECORD_ID,
                completedRequest()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DAILY_RECORD_NOT_FOUND);
    }

    @Test
    void saveAiAnalysisResultThrowsNotFoundWhenVoiceRecordDoesNotExist() {
        DailyRecord dailyRecord = dailyRecord();
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(voiceRecordRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.empty());

        assertThatThrownBy(() -> internalAiAnalysisService.saveAiAnalysisResult(
                DAILY_RECORD_ID,
                completedRequest()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VOICE_RECORD_NOT_FOUND);
    }

    @Test
    void saveAiAnalysisResultThrowsConflictWhenSttIsNotSuccess() {
        DailyRecord dailyRecord = dailyRecord();
        VoiceRecord voiceRecord = voiceRecord(dailyRecord, SttStatus.PENDING);
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(voiceRecordRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.of(voiceRecord));

        assertThatThrownBy(() -> internalAiAnalysisService.saveAiAnalysisResult(
                DAILY_RECORD_ID,
                completedRequest()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_ANALYSIS_STT_NOT_COMPLETED);
    }

    @Test
    void saveAiAnalysisResultThrowsInvalidInputWhenCompletedSummaryIsBlank() {
        assertThatThrownBy(() -> internalAiAnalysisService.saveAiAnalysisResult(
                DAILY_RECORD_ID,
                new SaveAiAnalysisResultRequest(AiAnalysisStatus.COMPLETED, " ", List.of("산책"))
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void saveAiAnalysisResultStoresCompletedWhenKeywordsAreEmpty() {
        DailyRecord dailyRecord = dailyRecord();
        VoiceRecord voiceRecord = voiceRecord(dailyRecord, SttStatus.SUCCESS);
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(voiceRecordRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.of(voiceRecord));
        given(aiAnalysisResultRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.empty());
        given(aiAnalysisResultRepository.save(any(AiAnalysisResult.class))).willAnswer(invocation -> {
            AiAnalysisResult aiAnalysisResult = invocation.getArgument(0);
            ReflectionTestUtils.setField(aiAnalysisResult, "id", AI_ANALYSIS_RESULT_ID);
            return aiAnalysisResult;
        });

        SaveAiAnalysisResultResponse response = internalAiAnalysisService.saveAiAnalysisResult(
                DAILY_RECORD_ID,
                new SaveAiAnalysisResultRequest(AiAnalysisStatus.COMPLETED, "요약", List.of())
        );

        assertThat(response.analysisStatus()).isEqualTo(AiAnalysisStatus.COMPLETED);
        assertThat(response.summary()).isEqualTo("요약");
        assertThat(response.keywords()).isEmpty();
        assertThat(response.analyzedAt()).isNotNull();
    }

    @Test
    void saveAiAnalysisResultStoresCompletedWhenKeywordsAreNull() {
        DailyRecord dailyRecord = dailyRecord();
        VoiceRecord voiceRecord = voiceRecord(dailyRecord, SttStatus.SUCCESS);
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(voiceRecordRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.of(voiceRecord));
        given(aiAnalysisResultRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.empty());
        given(aiAnalysisResultRepository.save(any(AiAnalysisResult.class))).willAnswer(invocation -> {
            AiAnalysisResult aiAnalysisResult = invocation.getArgument(0);
            ReflectionTestUtils.setField(aiAnalysisResult, "id", AI_ANALYSIS_RESULT_ID);
            return aiAnalysisResult;
        });

        SaveAiAnalysisResultResponse response = internalAiAnalysisService.saveAiAnalysisResult(
                DAILY_RECORD_ID,
                new SaveAiAnalysisResultRequest(AiAnalysisStatus.COMPLETED, "요약", null)
        );

        assertThat(response.analysisStatus()).isEqualTo(AiAnalysisStatus.COMPLETED);
        assertThat(response.summary()).isEqualTo("요약");
        assertThat(response.keywords()).isEmpty();
        assertThat(response.analyzedAt()).isNotNull();
    }

    @Test
    void saveAiAnalysisResultThrowsConflictWhenAlreadyCompletedAndRequestCompleted() {
        DailyRecord dailyRecord = dailyRecord();
        VoiceRecord voiceRecord = voiceRecord(dailyRecord, SttStatus.SUCCESS);
        AiAnalysisResult aiAnalysisResult = completedAiAnalysisResult(dailyRecord);
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(voiceRecordRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.of(voiceRecord));
        given(aiAnalysisResultRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.of(aiAnalysisResult));

        assertThatThrownBy(() -> internalAiAnalysisService.saveAiAnalysisResult(
                DAILY_RECORD_ID,
                completedRequest()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_ANALYSIS_ALREADY_COMPLETED);
    }

    @Test
    void saveAiAnalysisResultThrowsConflictWhenAlreadyCompletedAndRequestFailed() {
        DailyRecord dailyRecord = dailyRecord();
        VoiceRecord voiceRecord = voiceRecord(dailyRecord, SttStatus.SUCCESS);
        AiAnalysisResult aiAnalysisResult = completedAiAnalysisResult(dailyRecord);
        LocalDateTime analyzedAt = aiAnalysisResult.getAnalyzedAt();
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(voiceRecordRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.of(voiceRecord));
        given(aiAnalysisResultRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.of(aiAnalysisResult));

        assertThatThrownBy(() -> internalAiAnalysisService.saveAiAnalysisResult(
                DAILY_RECORD_ID,
                new SaveAiAnalysisResultRequest(AiAnalysisStatus.FAILED, null, null)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_ANALYSIS_ALREADY_COMPLETED);

        assertThat(aiAnalysisResult.getAnalysisStatus()).isEqualTo(AiAnalysisStatus.COMPLETED);
        assertThat(aiAnalysisResult.getSummary()).isEqualTo("이전 요약");
        assertThat(aiAnalysisResult.getKeywords()).containsExactly("이전");
        assertThat(aiAnalysisResult.getAnalyzedAt()).isEqualTo(analyzedAt);
        then(aiAnalysisResultRepository).should(never()).save(any(AiAnalysisResult.class));
    }

    @Test
    void saveAiAnalysisResultAllowsFailedToCompletedUpdate() {
        DailyRecord dailyRecord = dailyRecord();
        VoiceRecord voiceRecord = voiceRecord(dailyRecord, SttStatus.SUCCESS);
        AiAnalysisResult aiAnalysisResult = AiAnalysisResult.create(dailyRecord);
        aiAnalysisResult.saveFailed();
        ReflectionTestUtils.setField(aiAnalysisResult, "id", AI_ANALYSIS_RESULT_ID);
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(voiceRecordRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.of(voiceRecord));
        given(aiAnalysisResultRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.of(aiAnalysisResult));
        given(aiAnalysisResultRepository.save(aiAnalysisResult)).willReturn(aiAnalysisResult);

        SaveAiAnalysisResultResponse response = internalAiAnalysisService.saveAiAnalysisResult(
                DAILY_RECORD_ID,
                completedRequest()
        );

        assertThat(response.analysisStatus()).isEqualTo(AiAnalysisStatus.COMPLETED);
        assertThat(response.summary()).isEqualTo("요약");
        assertThat(response.keywords()).containsExactly("키워드");
        assertThat(response.analyzedAt()).isNotNull();
    }

    @Test
    void saveAiAnalysisResultConvertsDataIntegrityViolationToAlreadyCompletedConflict() {
        DailyRecord dailyRecord = dailyRecord();
        VoiceRecord voiceRecord = voiceRecord(dailyRecord, SttStatus.SUCCESS);
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(voiceRecordRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.of(voiceRecord));
        given(aiAnalysisResultRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.empty());
        given(aiAnalysisResultRepository.save(any(AiAnalysisResult.class)))
                .willThrow(new DataIntegrityViolationException("uk_ai_analysis_results_daily_record_id"));

        assertThatThrownBy(() -> internalAiAnalysisService.saveAiAnalysisResult(
                DAILY_RECORD_ID,
                completedRequest()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_ANALYSIS_ALREADY_COMPLETED);
    }

    @Test
    void saveNextQuestionsStoresSuccessQuestions() {
        DailyRecord dailyRecord = dailyRecord();
        AiAnalysisResult aiAnalysisResult = completedAiAnalysisResult(dailyRecord);
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(aiAnalysisResultRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.of(aiAnalysisResult));

        SaveNextQuestionsResponse response = internalAiAnalysisService.saveNextQuestions(
                DAILY_RECORD_ID,
                new SaveNextQuestionsRequest(
                        QuestionGenerationStatus.SUCCESS,
                        " 내일 기분이 좋다면 어떤 이야기를 들려주고 싶으세요? ",
                        "내일 마음이 흐리다면 어떤 순간이 떠오를까요?",
                        "내일 힘든 마음이 있다면 편하게 말해주실 수 있을까요?"
                )
        );

        assertThat(response.dailyRecordId()).isEqualTo(DAILY_RECORD_ID);
        assertThat(response.questionGenerationStatus()).isEqualTo(QuestionGenerationStatus.SUCCESS);
        assertThat(response.questionGeneratedAt()).isNotNull();
        assertThat(dailyRecord.getQuestionGenerationStatus()).isEqualTo(QuestionGenerationStatus.SUCCESS);
        assertThat(dailyRecord.getNextQuestions())
                .containsEntry(MoodType.SUNNY, "내일 기분이 좋다면 어떤 이야기를 들려주고 싶으세요?")
                .containsEntry(MoodType.CLOUDY, "내일 마음이 흐리다면 어떤 순간이 떠오를까요?")
                .containsEntry(MoodType.RAINY, "내일 힘든 마음이 있다면 편하게 말해주실 수 있을까요?");
    }

    @Test
    void saveNextQuestionsStoresFailedWithoutQuestions() {
        DailyRecord dailyRecord = dailyRecord();
        AiAnalysisResult aiAnalysisResult = completedAiAnalysisResult(dailyRecord);
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(aiAnalysisResultRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.of(aiAnalysisResult));

        SaveNextQuestionsResponse response = internalAiAnalysisService.saveNextQuestions(
                DAILY_RECORD_ID,
                new SaveNextQuestionsRequest(
                        QuestionGenerationStatus.FAILED,
                        null,
                        null,
                        null
                )
        );

        assertThat(response.questionGenerationStatus()).isEqualTo(QuestionGenerationStatus.FAILED);
        assertThat(response.questionGeneratedAt()).isNotNull();
        assertThat(dailyRecord.getNextQuestions())
                .containsEntry(MoodType.SUNNY, null)
                .containsEntry(MoodType.CLOUDY, null)
                .containsEntry(MoodType.RAINY, null);
    }

    @Test
    void saveNextQuestionsThrowsNotFoundWhenDailyRecordDoesNotExist() {
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> internalAiAnalysisService.saveNextQuestions(
                DAILY_RECORD_ID,
                successNextQuestionsRequest()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DAILY_RECORD_NOT_FOUND);
    }

    @Test
    void saveNextQuestionsThrowsConflictWhenAiAnalysisResultDoesNotExist() {
        DailyRecord dailyRecord = dailyRecord();
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(aiAnalysisResultRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.empty());

        assertThatThrownBy(() -> internalAiAnalysisService.saveNextQuestions(
                DAILY_RECORD_ID,
                successNextQuestionsRequest()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.QUESTION_GENERATION_AI_ANALYSIS_NOT_COMPLETED);
    }

    @Test
    void saveNextQuestionsThrowsConflictWhenAiAnalysisIsNotCompleted() {
        DailyRecord dailyRecord = dailyRecord();
        AiAnalysisResult aiAnalysisResult = AiAnalysisResult.create(dailyRecord);
        aiAnalysisResult.saveFailed();
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(aiAnalysisResultRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.of(aiAnalysisResult));

        assertThatThrownBy(() -> internalAiAnalysisService.saveNextQuestions(
                DAILY_RECORD_ID,
                successNextQuestionsRequest()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.QUESTION_GENERATION_AI_ANALYSIS_NOT_COMPLETED);
    }

    @Test
    void saveNextQuestionsThrowsConflictWhenAlreadySucceeded() {
        DailyRecord dailyRecord = dailyRecord();
        dailyRecord.saveNextQuestions("맑음 질문", "흐림 질문", "비 질문");
        AiAnalysisResult aiAnalysisResult = completedAiAnalysisResult(dailyRecord);
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(aiAnalysisResultRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.of(aiAnalysisResult));

        assertThatThrownBy(() -> internalAiAnalysisService.saveNextQuestions(
                DAILY_RECORD_ID,
                new SaveNextQuestionsRequest(QuestionGenerationStatus.FAILED, null, null, null)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.QUESTION_GENERATION_ALREADY_COMPLETED);
    }

    @Test
    void saveNextQuestionsAllowsFailedToSuccessRetry() {
        DailyRecord dailyRecord = dailyRecord();
        dailyRecord.saveNextQuestionsFailed();
        AiAnalysisResult aiAnalysisResult = completedAiAnalysisResult(dailyRecord);
        given(dailyRecordRepository.findById(DAILY_RECORD_ID)).willReturn(Optional.of(dailyRecord));
        given(aiAnalysisResultRepository.findByDailyRecord(dailyRecord)).willReturn(Optional.of(aiAnalysisResult));

        SaveNextQuestionsResponse response = internalAiAnalysisService.saveNextQuestions(
                DAILY_RECORD_ID,
                successNextQuestionsRequest()
        );

        assertThat(response.questionGenerationStatus()).isEqualTo(QuestionGenerationStatus.SUCCESS);
        assertThat(dailyRecord.hasAllNextQuestions()).isTrue();
    }

    @Test
    void saveNextQuestionsThrowsInvalidInputWhenSuccessQuestionIsBlank() {
        assertThatThrownBy(() -> internalAiAnalysisService.saveNextQuestions(
                DAILY_RECORD_ID,
                new SaveNextQuestionsRequest(
                        QuestionGenerationStatus.SUCCESS,
                        "맑음 질문",
                        " ",
                        "비 질문"
                )
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void saveNextQuestionsThrowsInvalidInputWhenSuccessSunnyQuestionExceedsMaxLength() {
        String tooLongQuestion = "가".repeat(501);

        assertThatThrownBy(() -> internalAiAnalysisService.saveNextQuestions(
                DAILY_RECORD_ID,
                new SaveNextQuestionsRequest(
                        QuestionGenerationStatus.SUCCESS,
                        tooLongQuestion,
                        "흐림 질문",
                        "비 질문"
                )
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void saveNextQuestionsThrowsInvalidInputWhenAnySuccessQuestionExceedsMaxLengthAfterTrim() {
        String tooLongQuestion = " 가".repeat(501);

        assertInvalidSuccessNextQuestionsRequest(new SaveNextQuestionsRequest(
                QuestionGenerationStatus.SUCCESS,
                "맑음 질문",
                tooLongQuestion,
                "비 질문"
        ));
        assertInvalidSuccessNextQuestionsRequest(new SaveNextQuestionsRequest(
                QuestionGenerationStatus.SUCCESS,
                "맑음 질문",
                "흐림 질문",
                tooLongQuestion
        ));
    }

    private SaveAiAnalysisResultRequest completedRequest() {
        return new SaveAiAnalysisResultRequest(
                AiAnalysisStatus.COMPLETED,
                "요약",
                List.of("키워드")
        );
    }

    private SaveNextQuestionsRequest successNextQuestionsRequest() {
        return new SaveNextQuestionsRequest(
                QuestionGenerationStatus.SUCCESS,
                "맑음 질문",
                "흐림 질문",
                "비 질문"
        );
    }

    private void assertInvalidSuccessNextQuestionsRequest(SaveNextQuestionsRequest request) {
        assertThatThrownBy(() -> internalAiAnalysisService.saveNextQuestions(DAILY_RECORD_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    private AiAnalysisResult completedAiAnalysisResult(DailyRecord dailyRecord) {
        AiAnalysisResult aiAnalysisResult = AiAnalysisResult.create(dailyRecord);
        aiAnalysisResult.saveCompleted("이전 요약", List.of("이전"));
        ReflectionTestUtils.setField(aiAnalysisResult, "id", AI_ANALYSIS_RESULT_ID);
        return aiAnalysisResult;
    }

    private DailyRecord dailyRecord() {
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
        dailyRecord.submitVoice();
        return dailyRecord;
    }

    private VoiceRecord voiceRecord(DailyRecord dailyRecord, SttStatus sttStatus) {
        VoiceRecord voiceRecord = VoiceRecord.create(
                dailyRecord,
                dailyRecord.getAppUser(),
                "uploads/voice.aac",
                120
        );
        ReflectionTestUtils.setField(voiceRecord, "id", VOICE_RECORD_ID);
        ReflectionTestUtils.setField(voiceRecord, "sttStatus", sttStatus);
        return voiceRecord;
    }
}
