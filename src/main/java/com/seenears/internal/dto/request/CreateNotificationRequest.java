package com.seenears.internal.dto.request;

import com.seenears.notifications.domain.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateNotificationRequest(
        @NotNull
        Long userId,

        Long letterId,

        @NotNull
        NotificationType type,

        @NotBlank
        String title,

        @NotBlank
        String body,

        @NotNull
        LocalDateTime scheduledAt
) {
}
