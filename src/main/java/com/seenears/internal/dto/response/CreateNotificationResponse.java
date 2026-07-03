package com.seenears.internal.dto.response;

import com.seenears.letters.domain.Letter;
import com.seenears.notifications.domain.Notification;
import com.seenears.notifications.domain.NotificationStatus;
import com.seenears.notifications.domain.NotificationType;

import java.time.LocalDateTime;

public record CreateNotificationResponse(
        Long notificationId,
        Long userId,
        NotificationType type,
        Long letterId,
        Long dailyRecordId,
        NotificationStatus status,
        LocalDateTime scheduledAt
) {

    public static CreateNotificationResponse from(Notification notification) {
        Letter letter = notification.getLetter();
        Long letterId = letter == null ? null : letter.getId();
        Long dailyRecordId = letter == null ? null : letter.getDailyRecord().getId();

        return new CreateNotificationResponse(
                notification.getId(),
                notification.getAppUser().getId(),
                notification.getType(),
                letterId,
                dailyRecordId,
                notification.getStatus(),
                notification.getScheduledAt()
        );
    }
}
