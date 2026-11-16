package com.markethub.seller.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoreUpdateRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 4000) String description) { }
