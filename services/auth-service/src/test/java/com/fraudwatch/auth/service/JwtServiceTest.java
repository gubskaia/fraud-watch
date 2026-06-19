package com.fraudwatch.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fraudwatch.auth.config.AuthProperties;
import com.fraudwatch.auth.domain.Permission;
import com.fraudwatch.auth.domain.Role;
import com.fraudwatch.auth.domain.User;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void shouldGenerateAndParseAccessToken() {
        JwtService jwtService = new JwtService(new AuthProperties(
            "fraudwatch",
            "c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0",
            Duration.ofMinutes(15),
            Duration.ofDays(7)
        ));

        User user = new User();
        user.setId(17L);
        user.setUsername("analyst");
        user.setEmail("analyst@example.com");

        Permission permission = new Permission();
        permission.setCode("fraud:review");

        Role role = new Role();
        role.setCode("ROLE_ANALYST");
        role.getPermissions().add(permission);
        user.getRoles().add(role);

        Instant beforeGeneration = Instant.now();
        JwtService.TokenDetails tokenDetails = jwtService.generateAccessToken(user);
        JwtService.JwtClaims claims = jwtService.parseAccessToken(tokenDetails.token());

        assertThat(tokenDetails.token()).isNotBlank();
        assertThat(tokenDetails.expiresAt()).isAfter(beforeGeneration);
        assertThat(claims.userId()).isEqualTo(17L);
        assertThat(claims.username()).isEqualTo("analyst");
        assertThat(claims.email()).isEqualTo("analyst@example.com");
        assertThat(claims.authorities()).containsExactlyInAnyOrder("ROLE_ANALYST", "fraud:review");
    }
}
