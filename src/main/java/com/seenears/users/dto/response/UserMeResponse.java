package com.seenears.users.dto.response;

import com.seenears.auth.domain.AppUser;
import com.seenears.auth.domain.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserMeResponse(
        Long userId,
        String name,
        String phoneNumber,
        UserStatus status,
        int currentStreakDays,
        LocalDate lastRecordedDate,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {

    public static UserMeResponse from(AppUser appUser) {
        return new UserMeResponse(
                appUser.getId(),
                appUser.getName(),
                appUser.getPhoneNumber(),
                appUser.getStatus(),
                appUser.getCurrentStreakDays(),
                appUser.getLastRecordedDate(),
                appUser.getLastLoginAt(),
                appUser.getCreatedAt()
        );
    }
}
