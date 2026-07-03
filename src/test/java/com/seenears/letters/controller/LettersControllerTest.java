package com.seenears.letters.controller;

import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.global.security.SecurityConfig;
import com.seenears.global.security.jwt.JwtTokenProvider;
import com.seenears.letters.domain.LetterStatus;
import com.seenears.letters.dto.response.ReadLetterResponse;
import com.seenears.letters.dto.response.TodayLetterResponse;
import com.seenears.letters.service.LettersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LettersController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.internal-api-key=test-internal-key",
        "jwt.secret=01234567890123456789012345678901"
})
class LettersControllerTest {

    private static final String ACCESS_TOKEN = "access-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LettersService lettersService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        given(jwtTokenProvider.validateAccessToken(ACCESS_TOKEN)).willReturn(true);
        given(jwtTokenProvider.getAuthentication(ACCESS_TOKEN))
                .willReturn(UsernamePasswordAuthenticationToken.authenticated("1", null, List.of()));
    }

    @Test
    void getTodayLetterReturnsSuccessResponse() throws Exception {
        given(lettersService.getTodayLetter(eq("1")))
                .willReturn(new TodayLetterResponse(
                        2L,
                        10L,
                        LocalDate.of(2026, 6, 23),
                        LocalDate.of(2026, 6, 24),
                        LetterStatus.GENERATED,
                        "오늘 산책을 다녀오셨군요.",
                        false,
                        null,
                        false,
                        LocalDateTime.of(2026, 6, 23, 18, 30)
                ));

        mockMvc.perform(get("/api/letters/today")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("오늘의 편지 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.letterId").value(2))
                .andExpect(jsonPath("$.data.dailyRecordId").value(10))
                .andExpect(jsonPath("$.data.recordDate").value("2026-06-23"))
                .andExpect(jsonPath("$.data.letterDate").value("2026-06-24"))
                .andExpect(jsonPath("$.data.status").value("GENERATED"))
                .andExpect(jsonPath("$.data.content").value("오늘 산책을 다녀오셨군요."))
                .andExpect(jsonPath("$.data.isRead").value(false))
                .andExpect(jsonPath("$.data.readAt").value(nullValue()))
                .andExpect(jsonPath("$.data.fallbackUsed").value(false))
                .andExpect(jsonPath("$.data.generatedAt").value("2026-06-23T18:30:00"))
                .andExpect(jsonPath("$.data.voiceRecordId").doesNotExist());

        verify(lettersService).getTodayLetter("1");
    }

    @Test
    void getTodayLetterReturnsPendingMessageWhenLetterIsPending() throws Exception {
        given(lettersService.getTodayLetter(eq("1")))
                .willReturn(new TodayLetterResponse(
                        2L,
                        10L,
                        LocalDate.of(2026, 6, 23),
                        LocalDate.of(2026, 6, 24),
                        LetterStatus.PENDING,
                        null,
                        false,
                        null,
                        false,
                        null
                ));

        mockMvc.perform(get("/api/letters/today")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("편지가 아직 생성 중입니다."))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.content").value(nullValue()))
                .andExpect(jsonPath("$.data.generatedAt").value(nullValue()));
    }

    @Test
    void getTodayLetterReturnsNotFoundWhenTodayLetterDoesNotExist() throws Exception {
        given(lettersService.getTodayLetter(eq("1")))
                .willThrow(new BusinessException(
                        ErrorCode.LETTER_NOT_FOUND,
                        "오늘 도착한 편지가 없습니다."
                ));

        mockMvc.perform(get("/api/letters/today")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("오늘 도착한 편지가 없습니다."))
                .andExpect(jsonPath("$.code").value("LETTER_001"));

        verify(lettersService).getTodayLetter("1");
    }

    @Test
    void getTodayLetterFailsWhenAuthorizationHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/api/letters/today"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        verifyNoInteractions(lettersService);
    }

    @Test
    void getTodayLetterFailsWhenAccessTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/api/letters/today")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        verifyNoInteractions(lettersService);
    }

    @Test
    void markAsReadReturnsSuccessResponse() throws Exception {
        given(lettersService.markAsRead(eq("1"), eq(2L)))
                .willReturn(new ReadLetterResponse(
                        2L,
                        true,
                        LocalDateTime.of(2026, 6, 23, 19, 0)
                ));

        mockMvc.perform(patch("/api/letters/{letterId}/read", 2L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("편지가 읽음 처리되었습니다."))
                .andExpect(jsonPath("$.data.letterId").value(2))
                .andExpect(jsonPath("$.data.isRead").value(true))
                .andExpect(jsonPath("$.data.readAt").value("2026-06-23T19:00:00"));

        verify(lettersService).markAsRead("1", 2L);
    }

    @Test
    void markAsReadFailsWhenAuthorizationHeaderIsMissing() throws Exception {
        mockMvc.perform(patch("/api/letters/{letterId}/read", 2L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        verifyNoInteractions(lettersService);
    }

    @Test
    void markAsReadFailsWhenAccessTokenIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/letters/{letterId}/read", 2L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        verifyNoInteractions(lettersService);
    }

    @Test
    void markAsReadReturnsCommonErrorResponseWhenServiceThrowsBusinessException() throws Exception {
        given(lettersService.markAsRead(eq("1"), eq(2L)))
                .willThrow(new BusinessException(ErrorCode.LETTER_STATUS_NOT_ALLOWED));

        mockMvc.perform(patch("/api/letters/{letterId}/read", 2L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("LETTER_005"))
                .andExpect(jsonPath("$.message").value("현재 상태에서는 편지를 읽음 처리할 수 없습니다."));
    }
}
