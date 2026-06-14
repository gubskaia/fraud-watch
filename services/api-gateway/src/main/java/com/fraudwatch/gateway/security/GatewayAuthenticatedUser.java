package com.fraudwatch.gateway.security;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

public record GatewayAuthenticatedUser(
    Long id,
    String username,
    String email,
    Collection<? extends GrantedAuthority> authorities
) {
}

