package com.markethub.checkout.api;

import java.math.BigDecimal;
import java.time.Instant;
public record CouponResponse(Long id, String code, String discountType, BigDecimal discountValue,
        BigDecimal minimumOrderAmount, Integer usageLimit, int usageCount, Instant startsAt, Instant expiresAt, boolean active) { }
