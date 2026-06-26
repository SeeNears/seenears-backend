package com.seenears.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SignupOtpRequest(
        @NotBlank String name,
        @NotBlank String phoneNumber
) {
}
