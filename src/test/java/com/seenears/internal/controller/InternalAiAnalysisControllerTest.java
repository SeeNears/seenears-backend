package com.seenears.internal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seenears.aianalysis.domain.AiAnalysisStatus;
import com.seenears.global.security.SecurityConfig;
import com.seenears.global.security.jwt.JwtTokenProvider;
import com.seenears.internal.dto.request.SaveAiAnalysisResultRequest;
import com.seenears.internal.dto.response.SaveAiAnalysisResultResponse;
import com.seenears.internal.service.InternalAiAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalAiAnalysisController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.internal-api-key=test-internal-key",
        "jwt.secret=01234567890123456789012345678901"
})
class InternalAiAnalysisControllerTest {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InternalAiAnalysisService internalAiAnalysisService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void saveAiAnalysisResultFailsWhenApiKeyIsMissing() throws Exception {
        mockMvc.perform(patch("/api/internal/daily-records/{dailyRecordId}/ai-analysis-result", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completedRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        verifyNoInteractions(internalAiAnalysisService);
    }

    @Test
    void saveAiAnalysisResultSucceedsWithValidApiKey() throws Exception {
        given(internalAiAnalysisService.saveAiAnalysisResult(eq(10L), any(SaveAiAnalysisResultRequest.class)))
                .willReturn(new SaveAiAnalysisResultResponse(
                        3L,
                        10L,
                        AiAnalysisStatus.COMPLETED,
                        "산책을 통해 긍정적인 감정을 느낀 하루였습니다.",
                        List.of("산책", "기분 좋음", "평온함"),
                        LocalDateTime.of(2026, 7, 2, 21, 30)
                ));

        mockMvc.perform(patch("/api/internal/daily-records/{dailyRecordId}/ai-analysis-result", 10L)
                        .header(INTERNAL_API_KEY_HEADER, "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completedRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("AI 분석 결과가 저장되었습니다."))
                .andExpect(jsonPath("$.data.aiAnalysisResultId").value(3))
                .andExpect(jsonPath("$.data.dailyRecordId").value(10))
                .andExpect(jsonPath("$.data.analysisStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.summary").value("산책을 통해 긍정적인 감정을 느낀 하루였습니다."))
                .andExpect(jsonPath("$.data.keywords[0]").value("산책"))
                .andExpect(jsonPath("$.data.analyzedAt").value("2026-07-02T21:30:00"));

        verify(internalAiAnalysisService).saveAiAnalysisResult(eq(10L), any(SaveAiAnalysisResultRequest.class));
    }

    @Test
    void saveAiAnalysisResultFailsWhenStatusIsMissing() throws Exception {
        mockMvc.perform(patch("/api/internal/daily-records/{dailyRecordId}/ai-analysis-result", 10L)
                        .header(INTERNAL_API_KEY_HEADER, "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"summary\":\"요약\",\"keywords\":[\"산책\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    private SaveAiAnalysisResultRequest completedRequest() {
        return new SaveAiAnalysisResultRequest(
                AiAnalysisStatus.COMPLETED,
                "산책을 통해 긍정적인 감정을 느낀 하루였습니다.",
                List.of("산책", "기분 좋음", "평온함")
        );
    }
}
