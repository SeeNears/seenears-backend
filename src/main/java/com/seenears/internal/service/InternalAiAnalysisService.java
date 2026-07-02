package com.seenears.internal.service;

import com.seenears.aianalysis.domain.AiAnalysisResult;
import com.seenears.aianalysis.domain.AiAnalysisStatus;
import com.seenears.aianalysis.repository.AiAnalysisResultRepository;
import com.seenears.dailyrecords.domain.DailyRecord;
import com.seenears.dailyrecords.repository.DailyRecordRepository;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.internal.dto.request.SaveAiAnalysisResultRequest;
import com.seenears.internal.dto.response.SaveAiAnalysisResultResponse;
import com.seenears.voicerecords.domain.SttStatus;
import com.seenears.voicerecords.domain.VoiceRecord;
import com.seenears.voicerecords.repository.VoiceRecordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InternalAiAnalysisService {

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
}
