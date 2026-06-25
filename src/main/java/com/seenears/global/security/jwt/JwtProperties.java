package com.seenears.global.security.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotNull Duration accessTokenExpiration,
        @NotNull Duration refreshTokenExpiration
) {

    private static final int MIN_SECRET_LENGTH_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.isBlank()
                || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_LENGTH_BYTES) {
            throw new IllegalArgumentException("jwt.secret must be at least 32 bytes");
        }
        if (accessTokenExpiration != null
                && (accessTokenExpiration.isZero() || accessTokenExpiration.isNegative())) {
            throw new IllegalArgumentException("jwt.access-token-expiration must be positive");
        }
        if (refreshTokenExpiration != null
                && (refreshTokenExpiration.isZero() || refreshTokenExpiration.isNegative())) {
            throw new IllegalArgumentException("jwt.refresh-token-expiration must be positive");
        }
    }
}
