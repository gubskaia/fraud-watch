package com.fraudwatch.auth.security;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

public record AuthenticatedUser(
    Long id,
    String username,
    String email,
    Collection<? extends GrantedAuthority> authorities
) {
}

