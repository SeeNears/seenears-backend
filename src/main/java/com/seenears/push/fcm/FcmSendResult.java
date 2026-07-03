package com.seenears.push.fcm;

import com.seenears.push.domain.PushDeviceToken;

import java.util.List;

public record FcmSendResult(
        int successCount,
        int failureCount,
        List<PushDeviceToken> invalidTokens
) {

    public static FcmSendResult success() {
        return new FcmSendResult(1, 0, List.of());
    }

    public static FcmSendResult failure(List<PushDeviceToken> failedTokens) {
        return new FcmSendResult(0, failedTokens.size(), List.of());
    }

    public boolean hasSuccess() {
        return successCount > 0;
    }
}
