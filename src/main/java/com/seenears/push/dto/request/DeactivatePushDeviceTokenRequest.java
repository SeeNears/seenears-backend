package com.seenears.push.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeactivatePushDeviceTokenRequest(
        @NotBlank(message = "deviceToken은 필수입니다.")
        String deviceToken
) {
}
