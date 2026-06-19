package com.fraudwatch.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fraudwatch.auth.config.AuthProperties;
import com.fraudwatch.auth.domain.Permission;
import com.fraudwatch.auth.domain.RefreshToken;
import com.fraudwatch.auth.domain.Role;
import com.fraudwatch.auth.domain.User;
import com.fraudwatch.auth.domain.UserStatus;
import com.fraudwatch.auth.dto.AuthResponse;
import com.fraudwatch.auth.dto.LoginRequest;
import com.fraudwatch.auth.dto.RefreshTokenRequest;
import com.fraudwatch.auth.dto.RegisterRequest;
import com.fraudwatch.auth.dto.UserResponse;
import com.fraudwatch.auth.exception.AuthBusinessException;
import com.fraudwatch.auth.mapper.AuthMapper;
import com.fraudwatch.auth.repository.RefreshTokenRepository;
import com.fraudwatch.auth.repository.RoleRepository;
import com.fraudwatch.auth.repository.UserRepository;
import com.fraudwatch.auth.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthMapper authMapper;

    @Mock
    private HttpServletRequest httpServletRequest;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            userRepository,
            roleRepository,
            refreshTokenRepository,
            passwordEncoder,
            jwtService,
            authMapper,
            new AuthProperties(
                "fraudwatch",
                "c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0",
                Duration.ofMinutes(15),
                Duration.ofDays(7)
            )
        );
    }

    @Test
    void shouldRegisterUserAndIssueTokens() {
        RegisterRequest request = new RegisterRequest(
            " alice ",
            " Alice@Example.com ",
            "very-secret-password",
            " Alice ",
            " Doe "
        );
        Role defaultRole = role("ROLE_CUSTOMER", "transactions:read");
        User savedUser = activeUser(7L, "alice", "alice@example.com", defaultRole);
        UserResponse userResponse = userResponse(savedUser, Set.of("ROLE_CUSTOMER"), Set.of("transactions:read"));
        JwtService.TokenDetails tokenDetails = new JwtService.TokenDetails(
            "access-token",
            Instant.parse("2026-06-19T10:15:00Z")
        );

        when(userRepository.existsByUsername(" alice ")).thenReturn(false);
        when(userRepository.existsByEmail(" Alice@Example.com ")).thenReturn(false);
        when(roleRepository.findByCode("ROLE_CUSTOMER")).thenReturn(Optional.of(defaultRole));
        when(passwordEncoder.encode("very-secret-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(savedUser)).thenReturn(tokenDetails);
        when(httpServletRequest.getHeader("X-Device-Id")).thenReturn("device-123");
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("198.51.100.10, 10.0.0.1");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setToken("refresh-token");
            return token;
        });
        when(authMapper.toUserResponse(savedUser)).thenReturn(userResponse);

        AuthResponse response = authService.register(request, httpServletRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User createdUser = userCaptor.getValue();
        assertThat(createdUser.getUsername()).isEqualTo("alice");
        assertThat(createdUser.getEmail()).isEqualTo("alice@example.com");
        assertThat(createdUser.getFirstName()).isEqualTo("Alice");
        assertThat(createdUser.getLastName()).isEqualTo("Doe");
        assertThat(createdUser.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(createdUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(createdUser.getRoles()).containsExactly(defaultRole);

        ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        RefreshToken refreshToken = refreshTokenCaptor.getValue();
        assertThat(refreshToken.getUser()).isEqualTo(savedUser);
        assertThat(refreshToken.getDeviceId()).isEqualTo("device-123");
        assertThat(refreshToken.getIpAddress()).isEqualTo("198.51.100.10");
        assertThat(refreshToken.getExpiresAt()).isAfter(Instant.now());

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user()).isEqualTo(userResponse);
    }

    @Test
    void shouldRejectDuplicateUsernameOnRegister() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
            new RegisterRequest("alice", "alice@example.com", "password", "Alice", "Doe"),
            httpServletRequest
        ))
            .isInstanceOf(AuthBusinessException.class)
            .extracting("status")
            .isEqualTo(HttpStatus.CONFLICT);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldRejectLoginWhenPasswordDoesNotMatch() {
        User user = activeUser(7L, "alice", "alice@example.com", role("ROLE_CUSTOMER", "transactions:read"));
        user.setPasswordHash("encoded-password");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
            new LoginRequest("alice@example.com", "wrong-password"),
            httpServletRequest
        ))
            .isInstanceOf(AuthBusinessException.class)
            .extracting("status")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldRejectLoginForInactiveUser() {
        User user = activeUser(7L, "alice", "alice@example.com", role("ROLE_CUSTOMER", "transactions:read"));
        user.setPasswordHash("encoded-password");
        user.setStatus(UserStatus.BLOCKED);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "password"), httpServletRequest))
            .isInstanceOf(AuthBusinessException.class)
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldRefreshAndRevokeExistingToken() {
        Role role = role("ROLE_CUSTOMER", "transactions:read");
        User user = activeUser(7L, "alice", "alice@example.com", role);
        RefreshToken existingToken = new RefreshToken();
        existingToken.setToken("refresh-old");
        existingToken.setUser(user);
        existingToken.setExpiresAt(Instant.now().plus(Duration.ofDays(1)));
        UserResponse userResponse = userResponse(user, Set.of("ROLE_CUSTOMER"), Set.of("transactions:read"));
        JwtService.TokenDetails tokenDetails = new JwtService.TokenDetails("access-new", Instant.parse("2026-06-19T12:00:00Z"));

        when(refreshTokenRepository.findByToken("refresh-old")).thenReturn(Optional.of(existingToken));
        when(jwtService.generateAccessToken(user)).thenReturn(tokenDetails);
        when(httpServletRequest.getHeader("X-Device-Id")).thenReturn("device-456");
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpServletRequest.getRemoteAddr()).thenReturn("203.0.113.77");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setToken("refresh-new");
            return token;
        });
        when(authMapper.toUserResponse(user)).thenReturn(userResponse);

        AuthResponse response = authService.refresh(new RefreshTokenRequest("refresh-old"), httpServletRequest);

        assertThat(existingToken.getRevokedAt()).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-new");
        assertThat(response.refreshToken()).isEqualTo("refresh-new");
    }

    @Test
    void shouldRejectExpiredRefreshToken() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setExpiresAt(Instant.now().minus(Duration.ofMinutes(1)));
        refreshToken.setUser(activeUser(7L, "alice", "alice@example.com", role("ROLE_CUSTOMER", "transactions:read")));

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("expired-token"), httpServletRequest))
            .isInstanceOf(AuthBusinessException.class)
            .extracting("status")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturnCurrentAuthenticatedUser() {
        Role role = role("ROLE_ANALYST", "fraud:review");
        User user = activeUser(11L, "analyst", "analyst@example.com", role);
        UserResponse userResponse = userResponse(user, Set.of("ROLE_ANALYST"), Set.of("fraud:review"));
        var authentication = new UsernamePasswordAuthenticationToken(
            new AuthenticatedUser(
                11L,
                "analyst",
                "analyst@example.com",
                List.of(new SimpleGrantedAuthority("ROLE_ANALYST"))
            ),
            null
        );

        when(userRepository.findDetailedById(11L)).thenReturn(Optional.of(user));
        when(authMapper.toUserResponse(user)).thenReturn(userResponse);

        assertThat(authService.getCurrentUser(authentication)).isEqualTo(userResponse);
    }

    @Test
    void shouldRejectCurrentUserWithoutAuthentication() {
        assertThatThrownBy(() -> authService.getCurrentUser(null))
            .isInstanceOf(AuthBusinessException.class)
            .extracting("status")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private User activeUser(Long id, String username, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName("Alice");
        user.setLastName("Doe");
        user.setStatus(UserStatus.ACTIVE);
        user.setEnabled(true);
        user.getRoles().add(role);
        return user;
    }

    private Role role(String code, String permissionCode) {
        Permission permission = new Permission();
        permission.setCode(permissionCode);
        permission.setDescription(permissionCode + " description");

        Role role = new Role();
        role.setCode(code);
        role.setDescription(code + " description");
        role.getPermissions().add(permission);
        return role;
    }

    private UserResponse userResponse(User user, Set<String> roles, Set<String> permissions) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getStatus().name(),
            roles,
            permissions
        );
    }
}
