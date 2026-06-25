package com.seenears.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginOtpVerifyRequest(
        @NotBlank String phoneNumber,
        @NotBlank String otpCode
) {
}
