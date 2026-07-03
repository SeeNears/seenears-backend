package com.seenears.notifications.controller;

import com.seenears.global.response.ApiResponse;
import com.seenears.notifications.dto.NotificationPageResponse;
import com.seenears.notifications.dto.NotificationResponse;
import com.seenears.notifications.service.NotificationsService;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@Validated
public class NotificationsController {

    private static final String LIST_SUCCESS_MESSAGE = "알림 목록 조회에 성공했습니다.";
    private static final String DETAIL_SUCCESS_MESSAGE = "알림 상세 조회에 성공했습니다.";

    private final NotificationsService notificationsService;

    public NotificationsController(NotificationsService notificationsService) {
        this.notificationsService = notificationsService;
    }

    @GetMapping
    public ApiResponse<NotificationPageResponse> getNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size
    ) {
        NotificationPageResponse response = notificationsService.getNotifications(
                authentication.getName(),
                page,
                size
        );

        return ApiResponse.success(LIST_SUCCESS_MESSAGE, response);
    }

    @GetMapping("/{notificationId}")
    public ApiResponse<NotificationResponse> getNotification(
            Authentication authentication,
            @PathVariable Long notificationId
    ) {
        NotificationResponse response = notificationsService.getNotification(
                authentication.getName(),
                notificationId
        );

        return ApiResponse.success(DETAIL_SUCCESS_MESSAGE, response);
    }
}
