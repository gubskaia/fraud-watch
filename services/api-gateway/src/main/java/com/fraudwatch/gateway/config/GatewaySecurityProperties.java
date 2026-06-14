package com.fraudwatch.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.security.jwt")
public record GatewaySecurityProperties(
    String issuer,
    String secret
) {
}

