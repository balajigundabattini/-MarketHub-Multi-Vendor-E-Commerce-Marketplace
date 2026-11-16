package com.markethub.security;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SignedCsrfTokenServiceTest {

    private final SignedCsrfTokenService service = new SignedCsrfTokenService(
            new SecretKeySpec(new byte[32], "HmacSHA256"));

    @Test
    void acceptsAnUntamperedToken() {
        String token = service.create();

        assertThat(service.isValid(token)).isTrue();
    }

    @Test
    void rejectsTamperedOrMalformedTokens() {
        String token = service.create();
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        assertThat(service.isValid(tampered)).isFalse();
        assertThat(service.isValid("missing-signature")).isFalse();
        assertThat(service.isValid(null)).isFalse();
    }
}
