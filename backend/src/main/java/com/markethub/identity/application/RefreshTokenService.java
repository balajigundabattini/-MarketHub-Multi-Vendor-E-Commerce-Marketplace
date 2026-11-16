package com.markethub.identity.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.markethub.identity.domain.MarketHubPrincipal;
import com.markethub.identity.domain.UserStatus;
import com.markethub.identity.infrastructure.jpa.RefreshTokenEntity;
import com.markethub.identity.infrastructure.jpa.RefreshTokenRepository;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import com.markethub.identity.infrastructure.jpa.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final Duration refreshLifetime;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            Clock clock,
            @Value("${markethub.security.refresh-token-days}") long refreshTokenDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.refreshLifetime = Duration.ofDays(refreshTokenDays);
    }

    @Transactional
    public IssuedRefreshToken create(UserEntity user) {
        String rawToken = newOpaqueToken();
        String familyId = UUID.randomUUID().toString();
        refreshTokenRepository.save(new RefreshTokenEntity(
                user, hash(rawToken), familyId, clock.instant().plus(refreshLifetime)));
        return new IssuedRefreshToken(rawToken, refreshLifetime);
    }

    @Transactional
    public RotationResult rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return RotationResult.invalid();
        var existing = refreshTokenRepository.findByHashForUpdate(hash(rawToken));
        if (existing.isEmpty()) return RotationResult.invalid();

        RefreshTokenEntity token = existing.get();
        Instant now = clock.instant();
        if (token.getRevokedAt() != null) {
            refreshTokenRepository.revokeFamily(token.getFamilyId(), now);
            return RotationResult.reuseDetected();
        }
        if (!token.getExpiresAt().isAfter(now) || token.getUser().getStatus() != UserStatus.ACTIVE) {
            token.revoke(now);
            return RotationResult.invalid();
        }

        token.revoke(now);
        UserEntity user = userRepository.findByIdWithRoles(token.getUser().getId()).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            refreshTokenRepository.revokeFamily(token.getFamilyId(), now);
            return RotationResult.invalid();
        }

        String nextRawToken = newOpaqueToken();
        refreshTokenRepository.save(new RefreshTokenEntity(
                user, hash(nextRawToken), token.getFamilyId(), now.plus(refreshLifetime)));
        Set<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getCode())
                .collect(Collectors.toUnmodifiableSet());
        MarketHubPrincipal principal = new MarketHubPrincipal(
                user.getId(), user.getEmail(), user.getPasswordHash(), user.getDisplayName(), user.getStatus(), roles);
        return RotationResult.success(principal, new IssuedRefreshToken(nextRawToken, refreshLifetime));
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        refreshTokenRepository.findByHashForUpdate(hash(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) token.revoke(clock.instant());
        });
    }

    private String newOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).toLowerCase(Locale.ROOT);
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record IssuedRefreshToken(String value, Duration lifetime) {
    }

    public record RotationResult(State state, MarketHubPrincipal principal, IssuedRefreshToken refreshToken) {
        public enum State { SUCCESS, INVALID, REUSE_DETECTED }

        static RotationResult invalid() { return new RotationResult(State.INVALID, null, null); }
        static RotationResult reuseDetected() { return new RotationResult(State.REUSE_DETECTED, null, null); }
        static RotationResult success(MarketHubPrincipal principal, IssuedRefreshToken refreshToken) {
            return new RotationResult(State.SUCCESS, principal, refreshToken);
        }
    }
}
