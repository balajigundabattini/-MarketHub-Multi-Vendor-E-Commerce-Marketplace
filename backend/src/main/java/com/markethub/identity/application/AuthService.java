package com.markethub.identity.application;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

import com.markethub.identity.api.AuthResponse;
import com.markethub.identity.api.LoginRequest;
import com.markethub.identity.api.RegisterRequest;
import com.markethub.identity.domain.MarketHubPrincipal;
import com.markethub.identity.infrastructure.jpa.RoleEntity;
import com.markethub.identity.infrastructure.jpa.RoleRepository;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import com.markethub.identity.infrastructure.jpa.UserRepository;
import com.markethub.identity.infrastructure.jpa.UserRoleEntity;
import com.markethub.identity.infrastructure.jpa.UserRoleRepository;
import com.markethub.security.JwtTokenService;
import com.markethub.security.SignedCsrfTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final SignedCsrfTokenService csrfTokenService;
    private final Clock clock;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            SignedCsrfTokenService csrfTokenService,
            Clock clock) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.csrfTokenService = csrfTokenService;
        this.clock = clock;
    }

    @Transactional
    public AuthSession register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes");
        }

        RoleEntity customerRole = roleRepository.findByCode("CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("CUSTOMER role is missing from the database"));
        UserEntity user = userRepository.saveAndFlush(new UserEntity(
                email, passwordEncoder.encode(request.password()), request.displayName().trim()));
        userRoleRepository.save(new UserRoleEntity(user, customerRole));

        MarketHubPrincipal principal = new MarketHubPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(),
                user.getDisplayName(), user.getStatus(), Set.of("CUSTOMER"));
        return createSession(principal, user);
    }

    public AuthSession login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        MarketHubPrincipal principal = (MarketHubPrincipal) authentication.getPrincipal();
        UserEntity user = userRepository.findByIdWithRoles(principal.getUserId())
                .orElseThrow(() -> new IllegalStateException("Authenticated account was not found"));
        return createSession(principal, user);
    }

    public AuthSession refresh(String rawRefreshToken) {
        RefreshTokenService.RotationResult result = refreshTokenService.rotate(rawRefreshToken);
        if (result.state() != RefreshTokenService.RotationResult.State.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired");
        }
        return createSession(result.principal(), result.refreshToken());
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private AuthSession createSession(MarketHubPrincipal principal, UserEntity user) {
        JwtTokenService.IssuedAccessToken accessToken = jwtTokenService.issue(principal);
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.create(user);
        return session(principal, accessToken, refreshToken);
    }

    private AuthSession createSession(
            MarketHubPrincipal principal,
            RefreshTokenService.IssuedRefreshToken refreshToken) {
        JwtTokenService.IssuedAccessToken accessToken = jwtTokenService.issue(principal);
        return session(principal, accessToken, refreshToken);
    }

    private AuthSession session(
            MarketHubPrincipal principal,
            JwtTokenService.IssuedAccessToken accessToken,
            RefreshTokenService.IssuedRefreshToken refreshToken) {
        AuthResponse response = new AuthResponse(
                accessToken.value(),
                "Bearer",
                accessToken.expiresInSeconds(),
                new AuthResponse.UserSummary(principal.getUserId(), principal.getEmail(),
                        principal.getDisplayName(), principal.getRoleCodes()));
        return new AuthSession(response, refreshToken.value(), refreshToken.lifetime(), csrfTokenService.create());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record AuthSession(
            AuthResponse response,
            String refreshToken,
            java.time.Duration refreshLifetime,
            String csrfToken) {
    }
}
