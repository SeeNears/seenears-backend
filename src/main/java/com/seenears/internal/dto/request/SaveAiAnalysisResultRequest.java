package com.seenears.internal.dto.request;

import com.seenears.aianalysis.domain.AiAnalysisStatus;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SaveAiAnalysisResultRequest(
        @NotNull
        AiAnalysisStatus analysisStatus,

        String summary,

        List<String> keywords
) {
}
