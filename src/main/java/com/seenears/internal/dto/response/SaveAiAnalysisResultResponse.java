package com.seenears.internal.dto.response;

import com.seenears.aianalysis.domain.AiAnalysisResult;
import com.seenears.aianalysis.domain.AiAnalysisStatus;

import java.time.LocalDateTime;
import java.util.List;

public record SaveAiAnalysisResultResponse(
        Long aiAnalysisResultId,
        Long dailyRecordId,
        AiAnalysisStatus analysisStatus,
        String summary,
        List<String> keywords,
        LocalDateTime analyzedAt
) {

    public SaveAiAnalysisResultResponse {
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
    }

    public static SaveAiAnalysisResultResponse from(AiAnalysisResult aiAnalysisResult) {
        return new SaveAiAnalysisResultResponse(
                aiAnalysisResult.getId(),
                aiAnalysisResult.getDailyRecord().getId(),
                aiAnalysisResult.getAnalysisStatus(),
                aiAnalysisResult.getSummary(),
                aiAnalysisResult.getKeywords(),
                aiAnalysisResult.getAnalyzedAt()
        );
    }
}
