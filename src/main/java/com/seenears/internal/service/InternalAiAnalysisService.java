package com.seenears.internal.service;

import com.seenears.aianalysis.domain.AiAnalysisResult;
import com.seenears.aianalysis.domain.AiAnalysisStatus;
import com.seenears.aianalysis.repository.AiAnalysisResultRepository;
import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.domain.QuestionGenerationStatus;
import com.seenears.dailyrecords.repository.DailyRecordRepository;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.internal.dto.request.SaveAiAnalysisResultRequest;
import com.seenears.internal.dto.request.SaveNextQuestionsRequest;
import com.seenears.internal.dto.response.SaveAiAnalysisResultResponse;
import com.seenears.internal.dto.response.SaveNextQuestionsResponse;
import com.seenears.voicerecords.domain.SttStatus;
import com.seenears.voicerecords.domain.VoiceRecord;
import com.seenears.voicerecords.repository.VoiceRecordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InternalAiAnalysisService {

    private static final int NEXT_QUESTION_MAX_LENGTH = 500;

    private final DailyRecordRepository dailyRecordRepository;
    private final VoiceRecordRepository voiceRecordRepository;
    private final AiAnalysisResultRepository aiAnalysisResultRepository;

    public InternalAiAnalysisService(
            DailyRecordRepository dailyRecordRepository,
            VoiceRecordRepository voiceRecordRepository,
            AiAnalysisResultRepository aiAnalysisResultRepository
    ) {
        this.dailyRecordRepository = dailyRecordRepository;
        this.voiceRecordRepository = voiceRecordRepository;
        this.aiAnalysisResultRepository = aiAnalysisResultRepository;
    }

    @Transactional
    public SaveAiAnalysisResultResponse saveAiAnalysisResult(
            Long dailyRecordId,
            SaveAiAnalysisResultRequest request
    ) {
        validateAiAnalysisResultRequest(request);

        DailyRecord dailyRecord = dailyRecordRepository.findById(dailyRecordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DAILY_RECORD_NOT_FOUND));

        VoiceRecord voiceRecord = voiceRecordRepository.findByDailyRecord(dailyRecord)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOICE_RECORD_NOT_FOUND));

        if (voiceRecord.getSttStatus() != SttStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.AI_ANALYSIS_STT_NOT_COMPLETED);
        }

        AiAnalysisResult aiAnalysisResult = aiAnalysisResultRepository.findByDailyRecord(dailyRecord)
                .orElseGet(() -> AiAnalysisResult.create(dailyRecord));

        if (aiAnalysisResult.getAnalysisStatus() == AiAnalysisStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.AI_ANALYSIS_ALREADY_COMPLETED);
        }

        if (request.analysisStatus() == AiAnalysisStatus.COMPLETED) {
            aiAnalysisResult.saveCompleted(
                    request.summary().trim(),
                    normalizedKeywords(request.keywords())
            );
        } else {
            aiAnalysisResult.saveFailed();
        }

        AiAnalysisResult savedAiAnalysisResult;
        try {
            savedAiAnalysisResult = aiAnalysisResultRepository.save(aiAnalysisResult);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.AI_ANALYSIS_ALREADY_COMPLETED);
        }
        return SaveAiAnalysisResultResponse.from(savedAiAnalysisResult);
    }

    @Transactional
    public SaveNextQuestionsResponse saveNextQuestions(
            Long dailyRecordId,
            SaveNextQuestionsRequest request
    ) {
        validateNextQuestionsRequest(request);

        DailyRecord dailyRecord = dailyRecordRepository.findById(dailyRecordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DAILY_RECORD_NOT_FOUND));

        AiAnalysisResult aiAnalysisResult = aiAnalysisResultRepository.findByDailyRecord(dailyRecord)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_GENERATION_AI_ANALYSIS_NOT_COMPLETED));

        if (aiAnalysisResult.getAnalysisStatus() != AiAnalysisStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.QUESTION_GENERATION_AI_ANALYSIS_NOT_COMPLETED);
        }

        if (dailyRecord.getQuestionGenerationStatus() == QuestionGenerationStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.QUESTION_GENERATION_ALREADY_COMPLETED);
        }

        if (request.questionGenerationStatus() == QuestionGenerationStatus.SUCCESS) {
            dailyRecord.saveNextQuestions(
                    request.nextQuestionSunny().trim(),
                    request.nextQuestionCloudy().trim(),
                    request.nextQuestionRainy().trim()
            );
        } else {
            dailyRecord.saveNextQuestionsFailed();
        }

        return SaveNextQuestionsResponse.from(dailyRecord);
    }

    private void validateAiAnalysisResultRequest(SaveAiAnalysisResultRequest request) {
        if (request == null || request.analysisStatus() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (request.analysisStatus() != AiAnalysisStatus.COMPLETED
                && request.analysisStatus() != AiAnalysisStatus.FAILED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (request.analysisStatus() == AiAnalysisStatus.COMPLETED) {
            if (request.summary() == null || request.summary().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }
    }

    private List<String> normalizedKeywords(List<String> keywords) {
        if (keywords == null) {
            return List.of();
        }

        return keywords.stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .map(String::trim)
                .toList();
    }

    private void validateNextQuestionsRequest(SaveNextQuestionsRequest request) {
        if (request == null || request.questionGenerationStatus() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (request.questionGenerationStatus() != QuestionGenerationStatus.SUCCESS
                && request.questionGenerationStatus() != QuestionGenerationStatus.FAILED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (request.questionGenerationStatus() == QuestionGenerationStatus.SUCCESS) {
            if (!hasText(request.nextQuestionSunny())
                    || !hasText(request.nextQuestionCloudy())
                    || !hasText(request.nextQuestionRainy())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }

            if (exceedsNextQuestionMaxLength(request.nextQuestionSunny())
                    || exceedsNextQuestionMaxLength(request.nextQuestionCloudy())
                    || exceedsNextQuestionMaxLength(request.nextQuestionRainy())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean exceedsNextQuestionMaxLength(String value) {
        return value.trim().length() > NEXT_QUESTION_MAX_LENGTH;
    }
}
