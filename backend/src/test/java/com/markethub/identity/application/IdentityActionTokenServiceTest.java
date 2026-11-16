package com.markethub.identity.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;

import com.markethub.identity.infrastructure.jpa.IdentityActionTokenEntity;
import com.markethub.identity.infrastructure.jpa.IdentityActionTokenRepository;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityActionTokenServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-16T00:00:00Z");

    @Test
    void storesOnlyHashAndConsumesTokenOnce() throws Exception {
        IdentityActionTokenRepository repository = mock(IdentityActionTokenRepository.class);
        when(repository.findOpen(null, "PASSWORD_RESET")).thenReturn(java.util.List.of());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new IdentityActionTokenService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

        String raw = service.issue(new UserEntity("person@example.com", "hash", "Person"),
                "PASSWORD_RESET", Duration.ofMinutes(30));
        var tokenCaptor = org.mockito.ArgumentCaptor.forClass(IdentityActionTokenEntity.class);
        verify(repository).save(tokenCaptor.capture());
        IdentityActionTokenEntity stored = tokenCaptor.getValue();
        String expectedHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(raw.getBytes(StandardCharsets.UTF_8)));

        assertThat(stored.getTokenHash()).isEqualTo(expectedHash).isNotEqualTo(raw);
        when(repository.findByHashForUpdate(expectedHash)).thenReturn(Optional.of(stored));
        assertThat(service.consume(raw, "PASSWORD_RESET")).isSameAs(stored.getUser());
        assertThat(stored.getConsumedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> service.consume(raw, "PASSWORD_RESET"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void rejectsWrongPurposeAndExpiredTokens() throws Exception {
        IdentityActionTokenRepository repository = mock(IdentityActionTokenRepository.class);
        var service = new IdentityActionTokenService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        String raw = "action-token";
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(raw.getBytes(StandardCharsets.UTF_8)));
        UserEntity user = new UserEntity("person@example.com", "hash", "Person");
        IdentityActionTokenEntity token = new IdentityActionTokenEntity(user, hash,
                "EMAIL_VERIFICATION", NOW);
        when(repository.findByHashForUpdate(hash)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.consume(raw, "PASSWORD_RESET"))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(token.getConsumedAt()).isNull();
        assertThatThrownBy(() -> service.consume(raw, "EMAIL_VERIFICATION"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
