package com.markethub.orders.api;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummaryResponse(Long id, String orderNumber, Instant createdAt, String status,
        BigDecimal total, String currency, int sellerOrderCount) { }
