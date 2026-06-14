package com.fraudwatch.auth.service;

import com.fraudwatch.auth.config.AuthProperties;
import com.fraudwatch.auth.domain.Permission;
import com.fraudwatch.auth.domain.Role;
import com.fraudwatch.auth.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final AuthProperties authProperties;
    private final SecretKey signingKey;

    public JwtService(AuthProperties authProperties) {
        this.authProperties = authProperties;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(authProperties.secret()));
    }

    public TokenDetails generateAccessToken(User user) {
        Instant expiresAt = Instant.now().plus(authProperties.accessTokenTtl());
        Set<String> authorities = extractAuthorities(user);
        String token = Jwts.builder()
            .subject(user.getUsername())
            .issuer(authProperties.issuer())
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(expiresAt))
            .claim("userId", user.getId())
            .claim("email", user.getEmail())
            .claim("authorities", authorities)
            .signWith(signingKey)
            .compact();

        return new TokenDetails(token, expiresAt);
    }

    public JwtClaims parseAccessToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        @SuppressWarnings("unchecked")
        Set<String> authorities = new LinkedHashSet<>((java.util.List<String>) claims.get("authorities", java.util.List.class));
        Number userId = claims.get("userId", Number.class);

        return new JwtClaims(
            userId == null ? null : userId.longValue(),
            claims.getSubject(),
            claims.get("email", String.class),
            authorities
        );
    }

    private Set<String> extractAuthorities(User user) {
        Set<String> authorities = new LinkedHashSet<>();
        for (Role role : user.getRoles()) {
            authorities.add(role.getCode());
            for (Permission permission : role.getPermissions()) {
                authorities.add(permission.getCode());
            }
        }
        return authorities;
    }

    public record JwtClaims(Long userId, String username, String email, Set<String> authorities) {
    }

    public record TokenDetails(String token, Instant expiresAt) {
    }
}
