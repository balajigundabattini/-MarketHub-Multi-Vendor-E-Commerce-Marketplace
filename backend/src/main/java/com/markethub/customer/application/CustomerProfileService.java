package com.markethub.customer.application;

import com.markethub.customer.api.CustomerProfileRequest;
import com.markethub.customer.api.CustomerProfileResponse;
import com.markethub.customer.infrastructure.jpa.CustomerProfileEntity;
import com.markethub.customer.infrastructure.jpa.CustomerProfileRepository;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import com.markethub.identity.infrastructure.jpa.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerProfileService {
    private final UserRepository users;
    private final CustomerProfileRepository profiles;
    public CustomerProfileService(UserRepository users, CustomerProfileRepository profiles) {
        this.users = users; this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse get(Long userId) {
        UserEntity user = requireUser(userId);
        CustomerProfileEntity profile = profiles.findById(userId).orElse(null);
        return new CustomerProfileResponse(user.getId(), user.getEmail(), user.getDisplayName(),
                profile == null ? null : profile.getPhone(), profile == null ? null : profile.getBirthDate());
    }

    @Transactional
    public CustomerProfileResponse update(Long userId, CustomerProfileRequest request) {
        UserEntity user = requireUser(userId);
        user.setDisplayName(request.displayName().trim());
        CustomerProfileEntity profile = profiles.findById(userId)
                .orElseGet(() -> profiles.save(new CustomerProfileEntity(user)));
        profile.update(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim(), request.birthDate());
        return new CustomerProfileResponse(user.getId(), user.getEmail(), user.getDisplayName(),
                profile.getPhone(), profile.getBirthDate());
    }

    private UserEntity requireUser(Long userId) {
        return users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }
}
