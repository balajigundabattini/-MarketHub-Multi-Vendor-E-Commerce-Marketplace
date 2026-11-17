package com.markethub.checkout.api;

public record PlacedOrderResponse(Long orderId, String orderNumber, Long paymentId) { }
