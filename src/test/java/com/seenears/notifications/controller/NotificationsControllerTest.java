package com.seenears.notifications.controller;

import com.seenears.global.exception.BusinessException;
import com.seenears.global.exception.ErrorCode;
import com.seenears.global.security.SecurityConfig;
import com.seenears.global.security.jwt.JwtTokenProvider;
import com.seenears.notifications.domain.NotificationStatus;
import com.seenears.notifications.domain.NotificationType;
import com.seenears.notifications.dto.NotificationPageResponse;
import com.seenears.notifications.dto.NotificationResponse;
import com.seenears.notifications.service.NotificationsService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationsController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.internal-api-key=test-internal-key",
        "jwt.secret=01234567890123456789012345678901"
})
class NotificationsControllerTest {

    private static final String ACCESS_TOKEN = "access-token";
    private static final LocalDateTime SCHEDULED_AT = LocalDateTime.of(2026, 7, 4, 8, 0);
    private static final LocalDateTime SENT_AT = LocalDateTime.of(2026, 7, 4, 8, 0, 1);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationsService notificationsService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        given(jwtTokenProvider.validateAccessToken(ACCESS_TOKEN)).willReturn(true);
        given(jwtTokenProvider.getAuthentication(ACCESS_TOKEN))
                .willReturn(UsernamePasswordAuthenticationToken.authenticated("1", null, List.of()));
    }

    @Test
    void getNotificationsReturnsSuccessResponse() throws Exception {
        given(notificationsService.getNotifications(eq("1"), eq(0), eq(20)))
                .willReturn(new NotificationPageResponse(
                        List.of(letterArrivedResponse()),
                        0,
                        20,
                        1,
                        1
                ));

        mockMvc.perform(get("/api/notifications")
                        .param("page", "0")
                        .param("size", "20")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("알림 목록 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.content[0].notificationId").value(1))
                .andExpect(jsonPath("$.data.content[0].type").value("LETTER_ARRIVED"))
                .andExpect(jsonPath("$.data.content[0].title").value("편지가 도착했어요"))
                .andExpect(jsonPath("$.data.content[0].body").value("오늘의 편지를 확인해보세요."))
                .andExpect(jsonPath("$.data.content[0].status").value("SENT"))
                .andExpect(jsonPath("$.data.content[0].letterId").value(10))
                .andExpect(jsonPath("$.data.content[0].dailyRecordId").value(20))
                .andExpect(jsonPath("$.data.content[0].scheduledAt").value("2026-07-04T08:00:00"))
                .andExpect(jsonPath("$.data.content[0].sentAt").value("2026-07-04T08:00:01"))
                .andExpect(jsonPath("$.data.content[0].isRead").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].readAt").doesNotExist())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));

        verify(notificationsService).getNotifications("1", 0, 20);
    }

    @Test
    void getNotificationsUsesDefaultPagingParameters() throws Exception {
        given(notificationsService.getNotifications(eq("1"), eq(0), eq(20)))
                .willReturn(new NotificationPageResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20));

        verify(notificationsService).getNotifications("1", 0, 20);
    }

    @Test
    void getNotificationsFailsWhenPageIsNegative() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .param("page", "-1")
                        .param("size", "20")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"));

        verifyNoInteractions(notificationsService);
    }

    @Test
    void getNotificationsFailsWhenSizeIsLessThanOne() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .param("page", "0")
                        .param("size", "0")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"));

        verifyNoInteractions(notificationsService);
    }

    @Test
    void getNotificationReturnsSuccessResponse() throws Exception {
        given(notificationsService.getNotification(eq("1"), eq(1L)))
                .willReturn(letterArrivedResponse());

        mockMvc.perform(get("/api/notifications/{notificationId}", 1L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("알림 상세 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.notificationId").value(1))
                .andExpect(jsonPath("$.data.type").value("LETTER_ARRIVED"))
                .andExpect(jsonPath("$.data.letterId").value(10))
                .andExpect(jsonPath("$.data.dailyRecordId").value(20))
                .andExpect(jsonPath("$.data.scheduledAt").value("2026-07-04T08:00:00"))
                .andExpect(jsonPath("$.data.sentAt").value("2026-07-04T08:00:01"))
                .andExpect(jsonPath("$.data.createdAt").doesNotExist())
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist())
                .andExpect(jsonPath("$.data.isRead").doesNotExist());

        verify(notificationsService).getNotification("1", 1L);
    }

    @Test
    void getNotificationReturnsNullLetterAndDailyRecordIds() throws Exception {
        given(notificationsService.getNotification(eq("1"), eq(2L)))
                .willReturn(new NotificationResponse(
                        2L,
                        NotificationType.RECORD_REMINDER,
                        "기록 시간이에요",
                        "오늘의 마음을 남겨주세요.",
                        NotificationStatus.PENDING,
                        null,
                        null,
                        SCHEDULED_AT,
                        null
                ));

        mockMvc.perform(get("/api/notifications/{notificationId}", 2L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.letterId").value(nullValue()))
                .andExpect(jsonPath("$.data.dailyRecordId").value(nullValue()))
                .andExpect(jsonPath("$.data.sentAt").value(nullValue()));
    }

    @Test
    void getNotificationReturnsNotFoundWhenServiceThrowsNotificationNotFound() throws Exception {
        given(notificationsService.getNotification(eq("1"), eq(999L)))
                .willThrow(new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        mockMvc.perform(get("/api/notifications/{notificationId}", 999L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("NOTIFICATION_001"))
                .andExpect(jsonPath("$.message").value("알림을 찾을 수 없습니다."));
    }

    @Test
    void getNotificationsFailsWhenAuthorizationHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        verifyNoInteractions(notificationsService);
    }

    @Test
    void getNotificationFailsWhenAccessTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/api/notifications/{notificationId}", 1L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        verifyNoInteractions(notificationsService);
    }

    private NotificationResponse letterArrivedResponse() {
        return new NotificationResponse(
                1L,
                NotificationType.LETTER_ARRIVED,
                "편지가 도착했어요",
                "오늘의 편지를 확인해보세요.",
                NotificationStatus.SENT,
                10L,
                20L,
                SCHEDULED_AT,
                SENT_AT
        );
    }
}
