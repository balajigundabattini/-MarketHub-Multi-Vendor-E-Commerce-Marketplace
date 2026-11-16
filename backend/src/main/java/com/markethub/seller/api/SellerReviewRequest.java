package com.markethub.seller.api;

import com.markethub.seller.domain.SellerStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SellerReviewRequest(
        @NotNull SellerStatus status,
        @Size(max = 500) String reason) { }
