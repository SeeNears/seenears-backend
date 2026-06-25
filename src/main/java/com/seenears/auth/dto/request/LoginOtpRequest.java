package com.seenears.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginOtpRequest(
        @NotBlank String phoneNumber
) {
}
