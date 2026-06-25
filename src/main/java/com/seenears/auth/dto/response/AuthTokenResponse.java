package com.seenears.auth.dto.response;

public record AuthTokenResponse(
        Long userId,
        String name,
        String phoneNumber,
        String accessToken,
        String refreshToken
) {
}
