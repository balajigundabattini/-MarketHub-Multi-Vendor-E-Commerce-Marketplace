
package com.markethub.checkout.api;

import java.math.BigDecimal;

public record PlacedOrderResponse(
        Long orderId,
        String orderNumber,
        Long paymentId,
        String paymentStatus,
        BigDecimal amount,
        Long amountMinor,
        String currency,
        String keyId,
        String providerOrderId
) {}
