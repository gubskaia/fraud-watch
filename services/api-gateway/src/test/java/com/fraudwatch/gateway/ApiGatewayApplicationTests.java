package com.fraudwatch.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fraudwatch.gateway.config.GatewaySecurityProperties;
import com.fraudwatch.gateway.filter.CorrelationIdFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "spring.cloud.gateway.server.webmvc.routes[0].id=test-protected-route",
    "spring.cloud.gateway.server.webmvc.routes[0].uri=forward:/internal/info",
    "spring.cloud.gateway.server.webmvc.routes[0].predicates[0]=Path=/api/test/**"
})
@AutoConfigureMockMvc
class ApiGatewayApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GatewaySecurityProperties securityProperties;

    @Test
    void contextLoads() {
    }

    @Test
    void publicEndpointGeneratesCorrelationId() throws Exception {
        MvcResult result = mockMvc.perform(get("/internal/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.service").value("api-gateway"))
            .andExpect(header().exists(CorrelationIdFilter.HEADER_NAME))
            .andReturn();

        assertThat(result.getResponse().getHeader(CorrelationIdFilter.HEADER_NAME)).isNotBlank();
    }

    @Test
    void publicEndpointReusesProvidedCorrelationId() throws Exception {
        mockMvc.perform(get("/internal/info").header(CorrelationIdFilter.HEADER_NAME, "corr-123"))
            .andExpect(status().isOk())
            .andExpect(header().string(CorrelationIdFilter.HEADER_NAME, "corr-123"));
    }

    @Test
    void protectedRouteRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/test/transactions"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Authentication is required"))
            .andExpect(header().exists(CorrelationIdFilter.HEADER_NAME));
    }

    @Test
    void protectedRouteRejectsInvalidToken() throws Exception {
        mockMvc.perform(get("/api/test/transactions").header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRouteAllowsValidJwt() throws Exception {
        mockMvc.perform(get("/api/test/transactions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken())
                .header(CorrelationIdFilter.HEADER_NAME, "corr-auth"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/internal/info"))
            .andExpect(header().string(CorrelationIdFilter.HEADER_NAME, "corr-auth"));
    }

    private String validToken() {
        SecretKey secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(securityProperties.secret()));
        return Jwts.builder()
            .issuer(securityProperties.issuer())
            .subject("analyst")
            .claim("userId", 42L)
            .claim("email", "analyst@fraudwatch.local")
            .claim("authorities", List.of("ROLE_ANALYST"))
            .signWith(secretKey)
            .compact();
    }
}
