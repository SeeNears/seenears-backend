package com.seenears.internal.dto.response;

import com.seenears.notifications.domain.Notification;
import com.seenears.notifications.domain.NotificationStatus;

public record SaveNotificationResultResponse(
        Long notificationId,
        NotificationStatus status
) {

    public static SaveNotificationResultResponse from(Notification notification) {
        return new SaveNotificationResultResponse(
                notification.getId(),
                notification.getStatus()
        );
    }
}
