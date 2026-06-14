package com.fraudwatch.auth.service;

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
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String DEFAULT_ROLE = "ROLE_CUSTOMER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthMapper authMapper;
    private final com.fraudwatch.auth.config.AuthProperties authProperties;

    public AuthService(
        UserRepository userRepository,
        RoleRepository roleRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        AuthMapper authMapper,
        com.fraudwatch.auth.config.AuthProperties authProperties
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authMapper = authMapper;
        this.authProperties = authProperties;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByUsername(request.username())) {
            throw new AuthBusinessException(HttpStatus.CONFLICT, "Username is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new AuthBusinessException(HttpStatus.CONFLICT, "Email is already registered");
        }

        Role defaultRole = roleRepository.findByCode(DEFAULT_ROLE)
            .orElseThrow(() -> new AuthBusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Default role is not configured"));

        User user = new User();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setStatus(UserStatus.ACTIVE);
        user.getRoles().add(defaultRole);

        User savedUser = userRepository.save(user);
        return issueTokens(savedUser, httpRequest);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = findByUsernameOrEmail(request.usernameOrEmail())
            .orElseThrow(() -> new AuthBusinessException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthBusinessException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        if (user.getStatus() != UserStatus.ACTIVE || !user.isEnabled()) {
            throw new AuthBusinessException(HttpStatus.FORBIDDEN, "User account is not active");
        }

        return issueTokens(user, httpRequest);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
            .orElseThrow(() -> new AuthBusinessException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid"));

        if (refreshToken.getRevokedAt() != null || refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthBusinessException(HttpStatus.UNAUTHORIZED, "Refresh token is expired or revoked");
        }

        refreshToken.setRevokedAt(Instant.now());
        User user = refreshToken.getUser();
        return issueTokens(user, httpRequest);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new AuthBusinessException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        User user = userRepository.findDetailedById(authenticatedUser.id())
            .orElseThrow(() -> new AuthBusinessException(HttpStatus.NOT_FOUND, "User was not found"));
        return authMapper.toUserResponse(user);
    }

    private Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        String normalized = usernameOrEmail.trim();
        if (normalized.contains("@")) {
            return userRepository.findByEmail(normalized.toLowerCase());
        }
        return userRepository.findByUsername(normalized);
    }

    private AuthResponse issueTokens(User user, HttpServletRequest request) {
        JwtService.TokenDetails accessToken = jwtService.generateAccessToken(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(Instant.now().plus(authProperties.refreshTokenTtl()));
        refreshToken.setDeviceId(request.getHeader("X-Device-Id"));
        refreshToken.setIpAddress(extractIpAddress(request));

        RefreshToken savedRefreshToken = refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
            accessToken.token(),
            savedRefreshToken.getToken(),
            "Bearer",
            accessToken.expiresAt(),
            savedRefreshToken.getExpiresAt(),
            authMapper.toUserResponse(user)
        );
    }

    private String extractIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
