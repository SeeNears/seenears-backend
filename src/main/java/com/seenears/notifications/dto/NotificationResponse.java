package com.seenears.notifications.dto;

import com.seenears.letters.domain.Letter;
import com.seenears.notifications.domain.Notification;
import com.seenears.notifications.domain.NotificationStatus;
import com.seenears.notifications.domain.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        String title,
        String body,
        NotificationStatus status,
        Long letterId,
        Long dailyRecordId,
        LocalDateTime scheduledAt,
        LocalDateTime sentAt
) {

    public static NotificationResponse from(Notification notification) {
        Letter letter = notification.getLetter();
        Long letterId = letter == null ? null : letter.getId();
        Long dailyRecordId = letter == null ? null : letter.getDailyRecord().getId();

        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getStatus(),
                letterId,
                dailyRecordId,
                notification.getScheduledAt(),
                notification.getSentAt()
        );
    }
}
