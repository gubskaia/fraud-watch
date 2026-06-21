package com.fraudwatch.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import java.util.Set;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "spring.cloud.gateway.server.webmvc.routes[0].id=auth-service",
    "spring.cloud.gateway.server.webmvc.routes[0].uri=forward:/internal/info",
    "spring.cloud.gateway.server.webmvc.routes[0].predicates[0]=Path=/api/auth/**,/api/users/**",
    "spring.cloud.gateway.server.webmvc.routes[1].id=transaction-service",
    "spring.cloud.gateway.server.webmvc.routes[1].uri=forward:/internal/info",
    "spring.cloud.gateway.server.webmvc.routes[1].predicates[0]=Path=/api/accounts/**,/api/transactions/**",
    "spring.cloud.gateway.server.webmvc.routes[2].id=review-service",
    "spring.cloud.gateway.server.webmvc.routes[2].uri=forward:/internal/info",
    "spring.cloud.gateway.server.webmvc.routes[2].predicates[0]=Path=/api/reviews/**",
    "spring.cloud.gateway.server.webmvc.routes[3].id=fraud-service",
    "spring.cloud.gateway.server.webmvc.routes[3].uri=forward:/internal/info",
    "spring.cloud.gateway.server.webmvc.routes[3].predicates[0]=Path=/api/fraud/**",
    "spring.cloud.gateway.server.webmvc.routes[4].id=audit-service",
    "spring.cloud.gateway.server.webmvc.routes[4].uri=forward:/internal/info",
    "spring.cloud.gateway.server.webmvc.routes[4].predicates[0]=Path=/api/audit/**",
    "spring.cloud.gateway.server.webmvc.routes[5].id=notification-service",
    "spring.cloud.gateway.server.webmvc.routes[5].uri=forward:/internal/info",
    "spring.cloud.gateway.server.webmvc.routes[5].predicates[0]=Path=/api/notifications/**",
    "spring.cloud.gateway.server.webmvc.routes[6].id=test-protected-route",
    "spring.cloud.gateway.server.webmvc.routes[6].uri=forward:/internal/info",
    "spring.cloud.gateway.server.webmvc.routes[6].predicates[0]=Path=/api/test/**"
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

    @Test
    void reviewEndpointsRequireAnalystPermission() throws Exception {
        mockMvc.perform(get("/api/reviews/cases")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken("ROLE_CUSTOMER", "TRANSACTION_READ")))
            .andExpect(status().isForbidden());
    }

    @Test
    void reviewEndpointsAllowAnalystPermission() throws Exception {
        mockMvc.perform(get("/api/reviews/cases")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken("ROLE_ANALYST", "REVIEW_CASE_READ")))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/internal/info"));
    }

    @Test
    void fraudRuleUpdatesRequireWritePermission() throws Exception {
        mockMvc.perform(post("/api/reviews/cases/10/block")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken("ROLE_ANALYST", "REVIEW_CASE_READ")))
            .andExpect(status().isForbidden());
    }

    @Test
    void auditEndpointsRequireAuditPermission() throws Exception {
        mockMvc.perform(get("/api/audit/records")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken("ROLE_CUSTOMER", "NOTIFICATION_READ")))
            .andExpect(status().isForbidden());
    }

    @Test
    void auditEndpointsAllowAuthorizedUser() throws Exception {
        mockMvc.perform(get("/api/audit/records")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken("ROLE_ANALYST", "AUDIT_READ")))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/internal/info"));
    }

    private String validToken(String... authorities) {
        SecretKey secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(securityProperties.secret()));
        return Jwts.builder()
            .issuer(securityProperties.issuer())
            .subject("analyst")
            .claim("userId", 42L)
            .claim("email", "analyst@fraudwatch.local")
            .claim("authorities", Set.of(authorities))
            .signWith(secretKey)
            .compact();
    }
}
