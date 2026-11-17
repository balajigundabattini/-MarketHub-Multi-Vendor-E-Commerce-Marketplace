package com.markethub.orders.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDetailsResponse(Long id, String orderNumber, Instant createdAt, String status,
        String paymentStatus, Long paymentId, BigDecimal subtotal, BigDecimal shipping, BigDecimal tax,
        BigDecimal discount, BigDecimal total, String currency, ShippingAddress shippingAddress,
        List<SellerOrder> sellerOrders) {
    public record ShippingAddress(String recipientName, String phone, String line1, String line2,
            String city, String region, String postalCode, String countryCode) { }
    public record SellerOrder(Long id, String storeName, String status, List<Shipment> shipments, List<Item> items) { }
    public record Shipment(Long id, String status, String carrier, String trackingNumber) { }
    public record Item(Long id, Long productId, String name, String sku, int quantity,
            BigDecimal unitPrice, String status, boolean canCancel, boolean canReturn, boolean canReview) { }
}
