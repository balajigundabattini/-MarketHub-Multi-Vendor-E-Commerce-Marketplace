package com.markethub.security;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.markethub.identity.domain.MarketHubPrincipal;
import com.markethub.identity.domain.UserStatus;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    @Test
    void signsShortLivedTokenWithIdentityAndRoles() {
        byte[] keyBytes = new byte[32];
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        JwtTokenService service = new JwtTokenService(
                new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key)),
                Clock.fixed(now, ZoneOffset.UTC),
                "markethub-test",
                15);
        MarketHubPrincipal principal = new MarketHubPrincipal(
                42L, "buyer@example.com", "unused", "Buyer", UserStatus.ACTIVE, Set.of("CUSTOMER"));

        JwtTokenService.IssuedAccessToken issued = service.issue(principal);
        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        Jwt decoded = decoder.decode(issued.value());

        assertThat(issued.expiresInSeconds()).isEqualTo(900);
        assertThat(decoded.getSubject()).isEqualTo("42");
        assertThat(decoded.getClaimAsString("email")).isEqualTo("buyer@example.com");
        assertThat(decoded.getClaimAsStringList("roles")).containsExactly("CUSTOMER");
        assertThat(decoded.getExpiresAt()).isEqualTo(now.plusSeconds(900));
    }
}
