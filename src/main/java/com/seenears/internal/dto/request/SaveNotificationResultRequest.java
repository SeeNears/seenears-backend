package com.seenears.internal.dto.request;

import com.seenears.notifications.domain.NotificationStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SaveNotificationResultRequest(
        @NotNull
        NotificationStatus status,

        LocalDateTime sentAt
) {
}
