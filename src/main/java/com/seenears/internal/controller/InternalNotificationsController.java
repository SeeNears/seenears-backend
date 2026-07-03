package com.seenears.internal.controller;

import com.seenears.global.response.ApiResponse;
import com.seenears.internal.dto.request.CreateNotificationRequest;
import com.seenears.internal.dto.request.SaveNotificationResultRequest;
import com.seenears.internal.dto.response.CreateNotificationResponse;
import com.seenears.internal.dto.response.SaveNotificationResultResponse;
import com.seenears.internal.service.InternalNotificationsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/notifications")
public class InternalNotificationsController {

    private static final String CREATE_SUCCESS_MESSAGE = "알림이 예약되었습니다.";
    private static final String RESULT_SUCCESS_MESSAGE = "알림 발송 결과가 저장되었습니다.";

    private final InternalNotificationsService internalNotificationsService;

    public InternalNotificationsController(InternalNotificationsService internalNotificationsService) {
        this.internalNotificationsService = internalNotificationsService;
    }

    @PostMapping
    public ApiResponse<CreateNotificationResponse> createNotification(
            @Valid @RequestBody CreateNotificationRequest request
    ) {
        CreateNotificationResponse response = internalNotificationsService.createNotification(request);

        return ApiResponse.success(CREATE_SUCCESS_MESSAGE, response);
    }

    @PatchMapping("/{notificationId}/result")
    public ApiResponse<SaveNotificationResultResponse> saveNotificationResult(
            @PathVariable Long notificationId,
            @Valid @RequestBody SaveNotificationResultRequest request
    ) {
        SaveNotificationResultResponse response =
                internalNotificationsService.saveNotificationResult(notificationId, request);

        return ApiResponse.success(RESULT_SUCCESS_MESSAGE, response);
    }
}
