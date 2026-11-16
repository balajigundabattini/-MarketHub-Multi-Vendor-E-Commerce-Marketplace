package com.markethub.seller.api;

import com.markethub.seller.application.SellerManagementService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller/store")
public class SellerStoreController {
    private final SellerManagementService sellers;

    public SellerStoreController(SellerManagementService sellers) { this.sellers = sellers; }

    @GetMapping
    public StoreResponse getStore(@AuthenticationPrincipal Jwt jwt) {
        return sellers.getMyStore(userId(jwt));
    }

    @PutMapping
    public StoreResponse updateStore(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody StoreUpdateRequest request) {
        return sellers.updateMyStore(userId(jwt), request);
    }

    private Long userId(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }
}
