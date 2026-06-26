package com.seenears.auth.dto.response;

public record OtpSendResponse(
        String phoneNumber,
        int expiresInSeconds
) {
}
