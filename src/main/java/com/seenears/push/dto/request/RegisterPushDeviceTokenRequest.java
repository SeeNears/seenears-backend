package com.seenears.push.dto.request;

import com.seenears.push.domain.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterPushDeviceTokenRequest(
        @NotBlank(message = "deviceToken은 필수입니다.")
        String deviceToken,

        @NotNull(message = "deviceType은 필수입니다.")
        DeviceType deviceType
) {
}
