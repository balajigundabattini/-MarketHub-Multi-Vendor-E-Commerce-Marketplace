package com.markethub.orders.api;

import java.time.Instant;

public record AdminCancellationRequestResponse(
        Long requestId,
        Long orderId,
        String orderNumber,
        Long itemId,
        String productName,
        int quantity,
        String customerName,
        String reason,
        String status,
        Instant requestedAt) {
}