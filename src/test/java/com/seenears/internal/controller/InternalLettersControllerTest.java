package com.seenears.internal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.global.security.SecurityConfig;
import com.seenears.global.security.jwt.JwtTokenProvider;
import com.seenears.internal.dto.request.SaveLetterResultRequest;
import com.seenears.internal.dto.response.SaveLetterResultResponse;
import com.seenears.internal.service.InternalLettersService;
import com.seenears.letters.domain.LetterStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalLettersController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.internal-api-key=test-internal-key",
        "jwt.secret=01234567890123456789012345678901"
})
class InternalLettersControllerTest {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InternalLettersService internalLettersService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void saveLetterResultFailsWhenApiKeyIsMissing() throws Exception {
        mockMvc.perform(patch("/api/internal/letters/{letterId}", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(generatedRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        verifyNoInteractions(internalLettersService);
    }

    @Test
    void saveLetterResultSucceedsWithGenerated() throws Exception {
        given(internalLettersService.saveLetterResult(eq(2L), any(SaveLetterResultRequest.class)))
                .willReturn(new SaveLetterResultResponse(
                        2L,
                        5L,
                        LetterStatus.GENERATED,
                        false,
                        LocalDateTime.of(2026, 6, 23, 18, 30)
                ));

        mockMvc.perform(patch("/api/internal/letters/{letterId}", 2L)
                        .header(INTERNAL_API_KEY_HEADER, "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(generatedRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("편지 생성 결과가 저장되었습니다."))
                .andExpect(jsonPath("$.data.letterId").value(2))
                .andExpect(jsonPath("$.data.voiceRecordId").value(5))
                .andExpect(jsonPath("$.data.status").value("GENERATED"))
                .andExpect(jsonPath("$.data.fallbackUsed").value(false))
                .andExpect(jsonPath("$.data.generatedAt").value("2026-06-23T18:30:00"));

        verify(internalLettersService).saveLetterResult(eq(2L), any(SaveLetterResultRequest.class));
    }

    @Test
    void saveLetterResultSucceedsWithFallbackGenerated() throws Exception {
        given(internalLettersService.saveLetterResult(eq(2L), any(SaveLetterResultRequest.class)))
                .willReturn(new SaveLetterResultResponse(
                        2L,
                        5L,
                        LetterStatus.FALLBACK_GENERATED,
                        true,
                        LocalDateTime.of(2026, 6, 23, 18, 31)
                ));

        mockMvc.perform(patch("/api/internal/letters/{letterId}", 2L)
                        .header(INTERNAL_API_KEY_HEADER, "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SaveLetterResultRequest(
                                5L,
                                LetterStatus.FALLBACK_GENERATED,
                                "편지 본문",
                                true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FALLBACK_GENERATED"))
                .andExpect(jsonPath("$.data.fallbackUsed").value(true))
                .andExpect(jsonPath("$.data.generatedAt").value("2026-06-23T18:31:00"));
    }

    @Test
    void saveLetterResultSucceedsWithFailed() throws Exception {
        given(internalLettersService.saveLetterResult(eq(2L), any(SaveLetterResultRequest.class)))
                .willReturn(new SaveLetterResultResponse(
                        2L,
                        5L,
                        LetterStatus.FAILED,
                        false,
                        LocalDateTime.of(2026, 6, 23, 18, 32)
                ));

        mockMvc.perform(patch("/api/internal/letters/{letterId}", 2L)
                        .header(INTERNAL_API_KEY_HEADER, "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SaveLetterResultRequest(
                                5L,
                                LetterStatus.FAILED,
                                null,
                                false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.fallbackUsed").value(false))
                .andExpect(jsonPath("$.data.generatedAt").value("2026-06-23T18:32:00"));
    }

    @Test
    void saveLetterResultFailsWhenRequestBodyIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/internal/letters/{letterId}", 2L)
                        .header(INTERNAL_API_KEY_HEADER, "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"GENERATED\",\"content\":\"편지 본문\",\"fallbackUsed\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"));

        verifyNoInteractions(internalLettersService);
    }

    @Test
    void saveLetterResultReturnsBadRequestWhenServiceThrowsInvalidInputValue() throws Exception {
        given(internalLettersService.saveLetterResult(eq(2L), any(SaveLetterResultRequest.class)))
                .willThrow(new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        mockMvc.perform(patch("/api/internal/letters/{letterId}", 2L)
                        .header(INTERNAL_API_KEY_HEADER, "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SaveLetterResultRequest(
                                5L,
                                LetterStatus.GENERATED,
                                "편지 본문",
                                true
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
    }

    @Test
    void saveLetterResultReturnsErrorResponseWhenServiceThrowsBusinessException() throws Exception {
        given(internalLettersService.saveLetterResult(eq(2L), any(SaveLetterResultRequest.class)))
                .willThrow(new BusinessException(ErrorCode.LETTER_VOICE_RECORD_MISMATCH));

        mockMvc.perform(patch("/api/internal/letters/{letterId}", 2L)
                        .header(INTERNAL_API_KEY_HEADER, "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(generatedRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("LETTER_003"))
                .andExpect(jsonPath("$.message").value("편지와 음성 기록이 일치하지 않습니다."));
    }

    private SaveLetterResultRequest generatedRequest() {
        return new SaveLetterResultRequest(
                5L,
                LetterStatus.GENERATED,
                "편지 본문",
                false
        );
    }
}
