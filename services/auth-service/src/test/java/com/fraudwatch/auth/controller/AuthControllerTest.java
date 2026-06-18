package com.fraudwatch.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudwatch.auth.config.SecurityConfig;
import com.fraudwatch.auth.dto.AuthResponse;
import com.fraudwatch.auth.dto.LoginRequest;
import com.fraudwatch.auth.dto.RefreshTokenRequest;
import com.fraudwatch.auth.dto.RegisterRequest;
import com.fraudwatch.auth.dto.UserResponse;
import com.fraudwatch.auth.exception.AuthBusinessException;
import com.fraudwatch.auth.exception.AuthExceptionHandler;
import com.fraudwatch.auth.security.JwtAuthenticationFilter;
import com.fraudwatch.auth.service.AuthService;
import com.fraudwatch.auth.service.JwtService;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AuthController.class, UserController.class})
@Import({SecurityConfig.class, AuthExceptionHandler.class, AuthControllerTest.SecurityTestConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldRegisterUser() throws Exception {
        when(authService.register(any(RegisterRequest.class), any())).thenReturn(authResponse("alice", "ROLE_USER"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(
                    "alice",
                    "alice@example.com",
                    "very-secret-password",
                    "Alice",
                    "Doe"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.username").value("alice"))
            .andExpect(jsonPath("$.user.roles[0]").value("ROLE_USER"));
    }

    @Test
    void shouldLoginUser() throws Exception {
        when(authService.login(any(LoginRequest.class), any())).thenReturn(authResponse("analyst", "ROLE_ANALYST"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(
                    "analyst@example.com",
                    "password-123"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.user.username").value("analyst"));
    }

    @Test
    void shouldRefreshTokenPair() throws Exception {
        when(authService.refresh(any(RefreshTokenRequest.class), any())).thenReturn(authResponse("analyst", "ROLE_ANALYST"));

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshTokenRequest("refresh-token"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
            .andExpect(jsonPath("$.user.email").value("analyst@example.com"));
    }

    @Test
    void shouldRejectInvalidRegisterRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(
                    "ab",
                    "not-an-email",
                    "short",
                    "",
                    ""
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.details.fields.username").exists())
            .andExpect(jsonPath("$.details.fields.email").exists())
            .andExpect(jsonPath("$.details.fields.password").exists());
    }

    @Test
    void shouldTranslateBusinessErrorOnLogin() throws Exception {
        when(authService.login(any(LoginRequest.class), any()))
            .thenThrow(new AuthBusinessException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(
                    "analyst@example.com",
                    "wrong-password"
                ))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid credentials"))
            .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void shouldRequireAuthenticationForCurrentUser() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnCurrentUserForAuthenticatedRequest() throws Exception {
        when(authService.getCurrentUser(any())).thenReturn(userResponse("analyst", "ROLE_ANALYST"));

        mockMvc.perform(get("/api/users/me").with(user("analyst").roles("ANALYST")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("analyst"))
            .andExpect(jsonPath("$.roles[0]").value("ROLE_ANALYST"));
    }

    private AuthResponse authResponse(String username, String role) {
        return new AuthResponse(
            "access-token",
            "refresh-token",
            "Bearer",
            Instant.parse("2026-06-18T13:00:00Z"),
            Instant.parse("2026-06-25T13:00:00Z"),
            userResponse(username, role)
        );
    }

    private UserResponse userResponse(String username, String role) {
        return new UserResponse(
            17L,
            username,
            username + "@example.com",
            "Alice",
            "Doe",
            "ACTIVE",
            Set.of(role),
            Set.of("fraud:review")
        );
    }

    @TestConfiguration
    static class SecurityTestConfig {

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
            return new JwtAuthenticationFilter(jwtService);
        }
    }
}
