package com.markethub.checkout.api;

import java.math.BigDecimal;
import java.time.Instant;

public record CheckoutQuoteResponse(String quoteId, BigDecimal subtotal, BigDecimal shipping,
        BigDecimal tax, BigDecimal discount, BigDecimal total, String currency, Instant expiresAt) { }
