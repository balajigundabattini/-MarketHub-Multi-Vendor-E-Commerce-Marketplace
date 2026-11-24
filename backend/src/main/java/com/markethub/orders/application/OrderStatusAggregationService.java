
package com.markethub.orders.application;

import java.util.List;
import java.util.Set;

import com.markethub.orders.infrastructure.jpa.OrderEntity;
import com.markethub.orders.infrastructure.jpa.OrderStatusHistoryEntity;
import com.markethub.orders.infrastructure.jpa.OrderStatusHistoryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderStatusAggregationService {

    private static final Set<String> PROTECTED_STATUSES = Set.of(
            "PENDING_PAYMENT",
            "PAYMENT_EXPIRED",
            "PAYMENT_REVIEW_REQUIRED",
            "REFUND_PENDING",
            "REFUNDED");

    private static final Set<String> DELIVERED_OR_RETURNING = Set.of(
            "DELIVERED",
            "RETURN_REQUESTED",
            "RETURN_APPROVED");

    private static final Set<String> SHIPPED_OR_BEYOND = Set.of(
            "SHIPPED",
            "DELIVERED",
            "RETURN_REQUESTED",
            "RETURN_APPROVED",
            "RETURNED");

    private static final Set<String> VALID_ITEM_STATUSES = Set.of(
            "ACTIVE",
            "SHIPPED",
            "DELIVERED",
            "CANCEL_REQUESTED",
            "CANCELLED",
            "RETURN_REQUESTED",
            "RETURN_APPROVED",
            "RETURNED");

    private final OrderStatusHistoryRepository history;

    public OrderStatusAggregationService(
            OrderStatusHistoryRepository history) {
        this.history = history;
    }

    @Transactional
    public void recalculate(OrderEntity order) {

        String currentStatus = order.getStatus();

        // Do not overwrite payment or refund lifecycle statuses.
        if (PROTECTED_STATUSES.contains(currentStatus)) {
            return;
        }

        var items = order.getSellerOrders().stream()
                .flatMap(seller -> seller.getItems().stream())
                .toList();

        if (items.isEmpty()) {
            return;
        }

        List<String> statuses = items.stream()
                .map(item -> item.getStatus())
                .toList();

        String newStatus = calculateStatus(statuses);

        if (newStatus.equals(currentStatus)) {
            return;
        }

        order.setStatus(newStatus);

        history.save(new OrderStatusHistoryEntity(
                order,
                null,
                currentStatus,
                newStatus,
                "Parent order status recalculated from order items"));
    }

    static String calculateStatus(List<String> statuses) {

        if (statuses == null || statuses.isEmpty()) {
            throw new IllegalArgumentException("Order items cannot be empty");
        }

        if (statuses.stream().anyMatch(status -> status == null || !VALID_ITEM_STATUSES.contains(status))) {
            throw new IllegalArgumentException("Invalid order item status");
        }

        boolean allCancelled = statuses.stream()
                .allMatch("CANCELLED"::equals);

        boolean allReturned = statuses.stream()
                .allMatch("RETURNED"::equals);

        boolean allDeliveredOrReturning = statuses.stream()
                .allMatch(DELIVERED_OR_RETURNING::contains);

        boolean anyReturned = statuses.contains("RETURNED");

        boolean anyCancelled = statuses.contains("CANCELLED");

        boolean anyShipped = statuses.stream()
                .anyMatch(SHIPPED_OR_BEYOND::contains);

        boolean allShippedOrBeyond = statuses.stream()
                .allMatch(SHIPPED_OR_BEYOND::contains);

        if (allCancelled) {
            return "CANCELLED";
        }

        if (allReturned) {
            return "RETURNED";
        }

        if (anyReturned && anyCancelled) {
            return "PARTIALLY_COMPLETED";
        }

        if (anyReturned) {
            return "PARTIALLY_RETURNED";
        }

        if (anyCancelled) {
            return "PARTIALLY_CANCELLED";
        }

        if (allDeliveredOrReturning) {
            return "DELIVERED";
        }

        if (anyShipped && !allShippedOrBeyond) {
            return "PARTIALLY_SHIPPED";
        }

        if (allShippedOrBeyond) {
            return "SHIPPED";
        }

        // ACTIVE, PROCESSING and other pre-shipment statuses.
        return "CONFIRMED";
    }
}
