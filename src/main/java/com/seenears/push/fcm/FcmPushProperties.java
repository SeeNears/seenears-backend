package com.seenears.push.fcm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.push.fcm")
public record FcmPushProperties(
        boolean enabled,
        boolean schedulerEnabled,
        int batchSize,
        long fixedDelayMs,
        String serviceAccountPath
) {

    private static final int DEFAULT_BATCH_SIZE = 20;
    private static final long DEFAULT_FIXED_DELAY_MS = 60_000L;

    public int resolvedBatchSize() {
        return batchSize > 0 ? batchSize : DEFAULT_BATCH_SIZE;
    }

    public long resolvedFixedDelayMs() {
        return fixedDelayMs > 0 ? fixedDelayMs : DEFAULT_FIXED_DELAY_MS;
    }

    public boolean hasServiceAccountPath() {
        return serviceAccountPath != null && !serviceAccountPath.isBlank();
    }
}
