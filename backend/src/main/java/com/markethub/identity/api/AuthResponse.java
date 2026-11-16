package com.markethub.identity.api;

import java.util.Set;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserSummary user) {

    public record UserSummary(Long id, String email, String displayName, Set<String> roles) {
    }
}
