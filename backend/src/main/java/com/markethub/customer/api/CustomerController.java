package com.markethub.customer.api;

import java.net.URI;
import java.util.List;
import com.markethub.customer.application.AddressService;
import com.markethub.customer.application.CustomerProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer")
public class CustomerController {
    private final CustomerProfileService profiles;
    private final AddressService addresses;
    public CustomerController(CustomerProfileService profiles, AddressService addresses) {
        this.profiles = profiles; this.addresses = addresses;
    }
    @GetMapping("/profile")
    public CustomerProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return profiles.get(userId(jwt));
    }
    @PutMapping("/profile")
    public CustomerProfileResponse updateProfile(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CustomerProfileRequest request) {
        return profiles.update(userId(jwt), request);
    }
    @GetMapping("/addresses")
    public List<AddressResponse> listAddresses(@AuthenticationPrincipal Jwt jwt) {
        return addresses.list(userId(jwt));
    }
    @PostMapping("/addresses")
    public ResponseEntity<AddressResponse> createAddress(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse result = addresses.create(userId(jwt), request);
        return ResponseEntity.created(URI.create("/api/v1/customer/addresses/" + result.id())).body(result);
    }
    @PutMapping("/addresses/{addressId}")
    public AddressResponse updateAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request) {
        return addresses.update(userId(jwt), addressId, request);
    }
    @PostMapping("/addresses/{addressId}/default")
    public AddressResponse makeDefault(@AuthenticationPrincipal Jwt jwt, @PathVariable Long addressId) {
        return addresses.makeDefault(userId(jwt), addressId);
    }
    @DeleteMapping("/addresses/{addressId}")
    public ResponseEntity<Void> deleteAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable Long addressId) {
        addresses.delete(userId(jwt), addressId);
        return ResponseEntity.noContent().build();
    }
    private Long userId(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }
}
