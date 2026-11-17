package com.markethub.orders.api;
import java.time.Instant;
import java.util.List;
public record SellerOrderResponse(Long id, String orderNumber, Instant createdAt, String status,
        List<Item> items, List<Shipment> shipments, List<ReturnRequest> returnRequests, boolean canAccept, boolean canReject,
        boolean canStartProcessing, boolean canShip, boolean canMarkDelivered, boolean canApproveCancellation) {
    public record Item(Long id, String name, int quantity) { }
    public record Shipment(Long id, String status, String carrier, String trackingNumber, Instant shippedAt) { }
    public record ReturnRequest(Long id, Long itemId, String itemName, int quantity, String reason, Instant createdAt) { }
}
