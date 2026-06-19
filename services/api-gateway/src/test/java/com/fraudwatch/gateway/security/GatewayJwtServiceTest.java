package com.fraudwatch.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fraudwatch.gateway.config.GatewaySecurityProperties;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

class GatewayJwtServiceTest {

    private static final String SECRET = "c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0";

    @Test
    void shouldParseValidTokenClaims() {
        GatewayJwtService service = new GatewayJwtService(new GatewaySecurityProperties("fraudwatch", SECRET));
        String token = Jwts.builder()
            .subject("gateway-user")
            .issuer("fraudwatch")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(900)))
            .claim("userId", 42L)
            .claim("email", "gateway@example.com")
            .claim("authorities", List.of("ROLE_USER", "transactions:read"))
            .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(SECRET)))
            .compact();

        GatewayJwtService.JwtPrincipalClaims claims = service.parse(token);

        assertThat(claims.userId()).isEqualTo(42L);
        assertThat(claims.username()).isEqualTo("gateway-user");
        assertThat(claims.email()).isEqualTo("gateway@example.com");
        assertThat(claims.authorities()).containsExactlyInAnyOrder("ROLE_USER", "transactions:read");
    }

    @Test
    void shouldRejectTokenWithUnexpectedIssuer() {
        GatewayJwtService service = new GatewayJwtService(new GatewaySecurityProperties("fraudwatch", SECRET));
        String token = Jwts.builder()
            .subject("gateway-user")
            .issuer("another-issuer")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(900)))
            .claim("userId", 42L)
            .claim("email", "gateway@example.com")
            .claim("authorities", List.of("ROLE_USER"))
            .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(SECRET)))
            .compact();

        assertThatThrownBy(() -> service.parse(token))
            .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }
}
