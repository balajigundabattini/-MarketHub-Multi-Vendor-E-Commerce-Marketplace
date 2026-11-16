package com.markethub.seller.api;

import java.net.URI;

import com.markethub.seller.application.SellerManagementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller-applications")
public class SellerApplicationController {
    private final SellerManagementService sellers;

    public SellerApplicationController(SellerManagementService sellers) { this.sellers = sellers; }

    @GetMapping("/me")
    public SellerApplicationResponse getMyApplication(@AuthenticationPrincipal Jwt jwt) {
        return sellers.getMyApplication(userId(jwt));
    }

    @PostMapping
    public ResponseEntity<SellerApplicationResponse> apply(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SellerApplicationRequest request) {
        SellerApplicationResponse response = sellers.apply(userId(jwt), request);
        return ResponseEntity.created(URI.create("/api/v1/seller-applications/me")).body(response);
    }

    private Long userId(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }
}
