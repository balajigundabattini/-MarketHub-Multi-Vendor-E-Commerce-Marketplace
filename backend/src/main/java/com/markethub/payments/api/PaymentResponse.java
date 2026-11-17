package com.markethub.payments.api;
import java.math.BigDecimal;
public record PaymentResponse(Long paymentId, Long orderId, String status, BigDecimal amount, long amountMinor, String currency,
        boolean canRetry, String keyId, String providerOrderId) { }
