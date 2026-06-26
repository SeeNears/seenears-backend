package com.seenears.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginOtpRequest(
        @NotBlank
        @Pattern(regexp = "^01[016789]\\d{7,8}$")
        String phoneNumber
) {
}
