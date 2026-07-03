package com.seenears.internal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.global.security.SecurityConfig;
import com.seenears.global.security.jwt.JwtTokenProvider;
import com.seenears.internal.dto.request.CreateNotificationRequest;
import com.seenears.internal.dto.request.SaveNotificationResultRequest;
import com.seenears.internal.dto.response.CreateNotificationResponse;
import com.seenears.internal.dto.response.SaveNotificationResultResponse;
import com.seenears.internal.service.InternalNotificationsService;
import com.seenears.notifications.domain.NotificationStatus;
import com.seenears.notifications.domain.NotificationType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalNotificationsController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.internal-api-key=test-internal-key",
        "jwt.secret=01234567890123456789012345678901"
})
class InternalNotificationsControllerTest {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final LocalDateTime SCHEDULED_AT = LocalDateTime.of(2026, 6, 23, 18, 30);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InternalNotificationsService internalNotificationsService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void createNotificationFailsWhenApiKeyIsMissing() throws Exception {
        mockMvc.perform(post("/api/internal/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(letterArrivedRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        verifyNoInteractions(internalNotificationsService);
    }

    @Test
    void createNotificationFailsWhenApiKeyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/internal/notifications")
                        .header(INTERNAL_API_KEY_HEADER, "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(letterArrivedRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        verifyNoInteractions(internalNotificationsService);
    }

    @Test
    void createNotificationSucceeds() throws Exception {
        given(internalNotificationsService.createNotification(any(CreateNotificationRequest.class)))
                .willReturn(new CreateNotificationResponse(
                        2L,
                        1L,
                        NotificationType.LETTER_ARRIVED,
                        2L,
                        10L,
                        NotificationStatus.PENDING,
                        SCHEDULED_AT
                ));

        mockMvc.perform(post("/api/internal/notifications")
                        .header(INTERNAL_API_KEY_HEADER, "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(letterArrivedRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("알림이 예약되었습니다."))
                .andExpect(jsonPath("$.data.notificationId").value(2))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.type").value("LETTER_ARRIVED"))
                .andExpect(jsonPath("$.data.letterId").value(2))
                .andExpect(jsonPath("$.data.dailyRecordId").value(10))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.scheduledAt").value("2026-06-23T18:30:00"));

        verify(internalNotificationsService).createNotification(any(CreateNotificationRequest.class));
    }

    @Test
    void createNotificationFailsWhenRequestBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/internal/notifications")
                        .header(INTERNAL_API_KEY_HEADER, "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "letterId": 2,
                                  "type": "LETTER_ARRIVED",
                                  "body": "오늘의 기록을 바탕으로 편지가 도착했어요.",
                                  "scheduledAt": "2026-06-23T18:30:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"));

        verifyNoInteractions(internalNotificationsService);
    }

    @Test
    void createNotificationReturnsErrorResponseWhenServiceThrowsBusinessException() throws Exception {
        given(internalNotificationsService.createNotification(any(CreateNotificationRequest.class)))
                .willThrow(new BusinessException(ErrorCode.NOTIFICATION_LETTER_NOT_READY));

        mockMvc.perform(post("/api/internal/notifications")
                        .header(INTERNAL_API_KEY_HEADER, "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(letterArrivedRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("NOTIFICATION_003"));
    }

    @Test
    void saveNotificationResultFailsWhenApiKeyIsMissing() throws Exception {
        mockMvc.perform(patch("/api/internal/notifications/{notificationId}/result", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sentRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        verifyNoInteractions(internalNotificationsService);
    }

    @Test
    void saveNotificationResultFailsWhenApiKeyIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/internal/notifications/{notificationId}/result", 2L)
                        .header(INTERNAL_API_KEY_HEADER, "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sentRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        verifyNoInteractions(internalNotificationsService);
    }

    @Test
    void saveNotificationResultSucceedsWithSent() throws Exception {
        given(internalNotificationsService.saveNotificationResult(eq(2L), any(SaveNotificationResultRequest.class)))
                .willReturn(new SaveNotificationResultResponse(2L, NotificationStatus.SENT));

        mockMvc.perform(patch("/api/internal/notifications/{notificationId}/result", 2L)
                        .header(INTERNAL_API_KEY_HEADER, "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sentRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("알림 발송 결과가 저장되었습니다."))
                .andExpect(jsonPath("$.data.notificationId").value(2))
                .andExpect(jsonPath("$.data.status").value("SENT"));
    }

    @Test
    void saveNotificationResultSucceedsWithFailed() throws Exception {
        given(internalNotificationsService.saveNotificationResult(eq(2L), any(SaveNotificationResultRequest.class)))
                .willReturn(new SaveNotificationResultResponse(2L, NotificationStatus.FAILED));

        mockMvc.perform(patch("/api/internal/notifications/{notificationId}/result", 2L)
                        .header(INTERNAL_API_KEY_HEADER, "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SaveNotificationResultRequest(
                                NotificationStatus.FAILED,
                                null
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"));
    }

    @Test
    void saveNotificationResultFailsWhenRequestBodyIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/internal/notifications/{notificationId}/result", 2L)
                        .header(INTERNAL_API_KEY_HEADER, "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));

        verifyNoInteractions(internalNotificationsService);
    }

    @Test
    void saveNotificationResultReturnsBadRequestWhenStatusIsPending() throws Exception {
        given(internalNotificationsService.saveNotificationResult(eq(2L), any(SaveNotificationResultRequest.class)))
                .willThrow(new BusinessException(ErrorCode.INVALID_NOTIFICATION_STATUS));

        mockMvc.perform(patch("/api/internal/notifications/{notificationId}/result", 2L)
                        .header(INTERNAL_API_KEY_HEADER, "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SaveNotificationResultRequest(
                                NotificationStatus.PENDING,
                                null
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_002"));
    }

    private CreateNotificationRequest letterArrivedRequest() {
        return new CreateNotificationRequest(
                1L,
                2L,
                NotificationType.LETTER_ARRIVED,
                "마음 편지가 도착했어요",
                "오늘의 기록을 바탕으로 편지가 도착했어요.",
                SCHEDULED_AT
        );
    }

    private SaveNotificationResultRequest sentRequest() {
        return new SaveNotificationResultRequest(
                NotificationStatus.SENT,
                LocalDateTime.of(2026, 6, 23, 18, 30, 5)
        );
    }
}
