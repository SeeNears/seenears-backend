package com.seenears.auth.dto.response;

public record TokenRefreshResponse(
        String accessToken,
        String refreshToken
) {
}
