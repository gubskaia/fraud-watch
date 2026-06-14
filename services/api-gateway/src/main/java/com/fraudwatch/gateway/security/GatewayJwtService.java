package com.fraudwatch.gateway.security;

import com.fraudwatch.gateway.config.GatewaySecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class GatewayJwtService {

    private final GatewaySecurityProperties properties;
    private final SecretKey secretKey;

    public GatewayJwtService(GatewaySecurityProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
    }

    public JwtPrincipalClaims parse(String token) throws JwtException {
        Claims claims = Jwts.parser()
            .requireIssuer(properties.issuer())
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        Number userId = claims.get("userId", Number.class);
        @SuppressWarnings("unchecked")
        Set<String> authorities = new LinkedHashSet<>((List<String>) claims.get("authorities", List.class));

        return new JwtPrincipalClaims(
            userId == null ? null : userId.longValue(),
            claims.getSubject(),
            claims.get("email", String.class),
            authorities
        );
    }

    public record JwtPrincipalClaims(
        Long userId,
        String username,
        String email,
        Set<String> authorities
    ) {
    }
}

