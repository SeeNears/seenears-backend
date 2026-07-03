package com.seenears.push.dto.response;

import com.seenears.push.domain.DeviceType;
import com.seenears.push.domain.PushDeviceToken;

import java.time.LocalDateTime;

public record RegisterPushDeviceTokenResponse(
        Long deviceTokenId,
        DeviceType deviceType,
        boolean isActive,
        LocalDateTime updatedAt
) {

    public static RegisterPushDeviceTokenResponse from(PushDeviceToken pushDeviceToken) {
        return new RegisterPushDeviceTokenResponse(
                pushDeviceToken.getId(),
                pushDeviceToken.getDeviceType(),
                pushDeviceToken.isActive(),
                pushDeviceToken.getUpdatedAt()
        );
    }
}
