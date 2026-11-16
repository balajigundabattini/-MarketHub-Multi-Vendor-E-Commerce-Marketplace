package com.markethub.identity.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

import com.markethub.identity.infrastructure.jpa.IdentityActionTokenEntity;
import com.markethub.identity.infrastructure.jpa.IdentityActionTokenRepository;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class IdentityActionTokenService {
    private final IdentityActionTokenRepository repository;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public IdentityActionTokenService(IdentityActionTokenRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public String issue(UserEntity user, String purpose, Duration lifetime) {
        repository.findOpen(user.getId(), purpose).forEach(token -> token.consume(clock.instant()));
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.save(new IdentityActionTokenEntity(user, hash(raw), purpose, clock.instant().plus(lifetime)));
        return raw;
    }

    @Transactional
    public UserEntity consume(String raw, String purpose) {
        if (raw == null || raw.isBlank()) throw invalid();
        IdentityActionTokenEntity token = repository.findByHashForUpdate(hash(raw)).orElseThrow(this::invalid);
        if (!purpose.equals(token.getPurpose()) || token.getConsumedAt() != null
                || !token.getExpiresAt().isAfter(clock.instant())) throw invalid();
        token.consume(clock.instant());
        return token.getUser();
    }

    private ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action token is invalid or expired");
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
