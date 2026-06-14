package com.fraudwatch.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.jwt")
public record AuthProperties(
    String issuer,
    String secret,
    Duration accessTokenTtl,
    Duration refreshTokenTtl
) {
}

