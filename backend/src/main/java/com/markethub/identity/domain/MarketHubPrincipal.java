package com.markethub.identity.domain;

import java.util.Collection;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class MarketHubPrincipal implements UserDetails {

    private final Long userId;
    private final String email;
    private final String passwordHash;
    private final String displayName;
    private final UserStatus status;
    private final Set<String> roleCodes;

    public MarketHubPrincipal(Long userId, String email, String passwordHash, String displayName,
                              UserStatus status, Set<String> roleCodes) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.status = status;
        this.roleCodes = Set.copyOf(roleCodes);
    }

    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public Set<String> getRoleCodes() { return roleCodes; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roleCodes.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return status != UserStatus.LOCKED; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return status == UserStatus.ACTIVE; }
}
