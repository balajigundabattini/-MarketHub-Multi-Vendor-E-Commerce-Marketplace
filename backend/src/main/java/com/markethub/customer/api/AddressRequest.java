package com.markethub.customer.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank @Size(max = 120) String recipientName,
        @NotBlank @Pattern(regexp = "^[0-9+() .-]{7,32}$") String phone,
        @NotBlank @Size(max = 200) String line1,
        @Size(max = 200) String line2,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String region,
        @NotBlank @Size(max = 24) String postalCode,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$") String countryCode,
        @Size(max = 40) String label,
        boolean isDefault) { }
