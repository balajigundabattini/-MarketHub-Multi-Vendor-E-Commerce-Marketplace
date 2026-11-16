package com.markethub.identity.application;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.markethub.identity.domain.MarketHubPrincipal;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import com.markethub.identity.infrastructure.jpa.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public IdentityUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        UserEntity user = userRepository.findByEmailWithRoles(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
        Set<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getCode())
                .collect(Collectors.toUnmodifiableSet());

        return new MarketHubPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(),
                user.getDisplayName(), user.getStatus(), roles);
    }
}
