package com.seenears.internal.controller;

import com.seenears.global.response.ApiResponse;
import com.seenears.internal.dto.request.SaveAiAnalysisResultRequest;
import com.seenears.internal.dto.request.SaveNextQuestionsRequest;
import com.seenears.internal.dto.response.SaveAiAnalysisResultResponse;
import com.seenears.internal.dto.response.SaveNextQuestionsResponse;
import com.seenears.internal.service.InternalAiAnalysisService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/daily-records")
public class InternalAiAnalysisController {

    private static final String AI_ANALYSIS_RESULT_SUCCESS_MESSAGE = "AI 분석 결과가 저장되었습니다.";
    private static final String NEXT_QUESTIONS_SUCCESS_MESSAGE = "다음 질문이 저장되었습니다.";

    private final InternalAiAnalysisService internalAiAnalysisService;

    public InternalAiAnalysisController(InternalAiAnalysisService internalAiAnalysisService) {
        this.internalAiAnalysisService = internalAiAnalysisService;
    }

    @PatchMapping("/{dailyRecordId}/ai-analysis-result")
    public ApiResponse<SaveAiAnalysisResultResponse> saveAiAnalysisResult(
            @PathVariable Long dailyRecordId,
            @Valid @RequestBody SaveAiAnalysisResultRequest request
    ) {
        SaveAiAnalysisResultResponse response = internalAiAnalysisService.saveAiAnalysisResult(
                dailyRecordId,
                request
        );

        return ApiResponse.success(AI_ANALYSIS_RESULT_SUCCESS_MESSAGE, response);
    }

    @PatchMapping("/{dailyRecordId}/next-questions")
    public ApiResponse<SaveNextQuestionsResponse> saveNextQuestions(
            @PathVariable Long dailyRecordId,
            @Valid @RequestBody SaveNextQuestionsRequest request
    ) {
        SaveNextQuestionsResponse response = internalAiAnalysisService.saveNextQuestions(
                dailyRecordId,
                request
        );

        return ApiResponse.success(NEXT_QUESTIONS_SUCCESS_MESSAGE, response);
    }
}
