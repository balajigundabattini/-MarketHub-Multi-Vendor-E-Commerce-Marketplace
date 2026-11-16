package com.markethub.seller.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerApplicationRequest(
        @NotBlank @Size(max = 160) String legalName,
        @NotBlank @Size(max = 160) String businessName) { }
