package com.markethub.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.markethub.identity.domain.MarketHubPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final JwtEncoder encoder;
    private final Clock clock;
    private final String issuer;
    private final Duration accessTokenLifetime;

    public JwtTokenService(
            JwtEncoder encoder,
            Clock clock,
            @Value("${markethub.security.jwt.issuer}") String issuer,
            @Value("${markethub.security.jwt.access-token-minutes}") long accessTokenMinutes) {
        this.encoder = encoder;
        this.clock = clock;
        this.issuer = issuer;
        this.accessTokenLifetime = Duration.ofMinutes(accessTokenMinutes);
    }

    public IssuedAccessToken issue(MarketHubPrincipal principal) {
        Instant issuedAt = clock.instant();
        List<String> roles = principal.getRoleCodes().stream().sorted().toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(principal.getUserId().toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(accessTokenLifetime))
                .claim("email", principal.getEmail())
                .claim("display_name", principal.getDisplayName())
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAccessToken(token, accessTokenLifetime.toSeconds());
    }

    public record IssuedAccessToken(String value, long expiresInSeconds) {
    }
}
