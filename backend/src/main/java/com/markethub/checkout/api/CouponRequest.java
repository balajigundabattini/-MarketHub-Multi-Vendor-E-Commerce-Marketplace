package com.markethub.checkout.api;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CouponRequest(@NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9_-]+") String code,
        @NotBlank @Pattern(regexp = "PERCENT|FIXED") String discountType,
        @NotNull @DecimalMin("0.01") BigDecimal discountValue,
        @NotNull @DecimalMin("0.00") BigDecimal minimumOrderAmount,
        Instant startsAt, Instant expiresAt, Integer usageLimit, @NotNull Boolean active) { }
