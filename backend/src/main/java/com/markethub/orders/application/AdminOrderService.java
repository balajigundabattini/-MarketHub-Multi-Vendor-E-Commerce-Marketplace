
package com.markethub.orders.application;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.markethub.catalog.infrastructure.jpa.InventoryEntity;
import com.markethub.catalog.infrastructure.jpa.InventoryRepository;
import com.markethub.identity.infrastructure.jpa.UserRepository;
import com.markethub.orders.api.AdminOrderResponse;
import com.markethub.orders.api.AdminRefundResponse;
import com.markethub.orders.infrastructure.jpa.InventoryReservationEntity;
import com.markethub.orders.infrastructure.jpa.InventoryReservationRepository;
import com.markethub.orders.infrastructure.jpa.OrderRepository;
import com.markethub.orders.infrastructure.jpa.OrderStatusHistoryEntity;
import com.markethub.orders.infrastructure.jpa.OrderStatusHistoryRepository;
import com.markethub.orders.infrastructure.jpa.SellerOrderStatusHistoryEntity;
import com.markethub.orders.infrastructure.jpa.SellerOrderStatusHistoryRepository;
import com.markethub.payments.application.RazorpayGateway;
import com.markethub.payments.infrastructure.jpa.PaymentRepository;
import com.markethub.payments.infrastructure.jpa.RefundEntity;
import com.markethub.payments.infrastructure.jpa.RefundRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminOrderService {

        private final OrderRepository orders;
        private final PaymentRepository payments;
        private final RefundRepository refunds;
        private final RazorpayGateway gateway;
        private final UserRepository users;
        private final OrderStatusHistoryRepository history;
        private final SellerOrderStatusHistoryRepository sellerHistory;
        private final InventoryRepository inventory;
        private final InventoryReservationRepository reservations;
        private final Clock clock;

        private static final Set<String> VALID_ORDER_STATUSES = Set.of(
                        "PENDING_PAYMENT",
                        "PAYMENT_EXPIRED",
                        "PAYMENT_REVIEW_REQUIRED",
                        "CONFIRMED",
                        "PROCESSING",
                        "ACCEPTED",
                        "SHIPPED",
                        "DELIVERED",
                        "RETURN_REQUESTED",
                        "RETURN_APPROVED",
                        "RETURNED",
                        "CANCELLED",
                        "PARTIALLY_COMPLETED",
                        "PARTIALLY_RETURNED",
                        "PARTIALLY_CANCELLED",
                        "PARTIALLY_SHIPPED",
                        "REFUND_PENDING",
                        "REFUNDED");

        public AdminOrderService(
                        OrderRepository orders,
                        PaymentRepository payments,
                        RefundRepository refunds,
                        RazorpayGateway gateway,
                        UserRepository users,
                        OrderStatusHistoryRepository history,
                        SellerOrderStatusHistoryRepository sellerHistory,
                        InventoryRepository inventory,
                        InventoryReservationRepository reservations,
                        Clock clock) {

                this.orders = orders;
                this.payments = payments;
                this.refunds = refunds;
                this.gateway = gateway;
                this.users = users;
                this.history = history;
                this.sellerHistory = sellerHistory;
                this.inventory = inventory;
                this.reservations = reservations;
                this.clock = clock;
        }

        @Transactional(readOnly = true)
        public Page<AdminOrderResponse> list(String status, int page, int size) {
                if (status != null && !VALID_ORDER_STATUSES.contains(status)) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Invalid order status");
                }

                if (page < 0 || size < 1 || size > 100)
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Page or page size is invalid");

                return orders.findAdminPage(
                                status,
                                PageRequest.of(
                                                page,
                                                size,
                                                Sort.by(Sort.Direction.DESC, "createdAt")))
                                .map(o -> {

                                        var p = payments.findByOrderId(o.getId()).orElse(null);

                                        boolean can = p != null
                                                        && "CAPTURED".equals(p.getStatus())
                                                        && List.of(
                                                                        "CONFIRMED",
                                                                        "PROCESSING",
                                                                        "SHIPPED",
                                                                        "DELIVERED",
                                                                        "PAYMENT_REVIEW_REQUIRED")
                                                                        .contains(o.getStatus())
                                                        && !refunds.existsByPaymentId(p.getId());

                                        return new AdminOrderResponse(
                                                        o.getId(),
                                                        o.getPublicOrderNumber(),
                                                        o.getCustomer().getDisplayName(),
                                                        o.getCreatedAt(),
                                                        o.getTotal(),
                                                        o.getCurrency(),
                                                        o.getPaymentStatus(),
                                                        o.getStatus(),
                                                        can);
                                });
        }

        // Admin approves fulfillment of a late-captured payment.
        @Transactional
        public void fulfillLatePaidOrder(Long adminId, Long orderId) {

                var payment = payments.findByOrderIdForUpdate(orderId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.CONFLICT,
                                                "Captured payment was not found"));

                var order = orders.findByIdForUpdate(orderId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Order was not found"));

                if (!"CAPTURED".equals(payment.getStatus())
                                || !"PAID".equals(order.getPaymentStatus())
                                || !"RAZORPAY".equals(order.getPaymentMethod())
                                || !"PAYMENT_REVIEW_REQUIRED".equals(order.getStatus())) {

                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Order is not eligible for late-payment fulfillment");
                }

                if (refunds.existsByPaymentId(payment.getId())) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "An order with a refund request cannot be fulfilled");
                }

                var admin = users.findById(adminId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Administrator was not found"));

                var items = order.getSellerOrders().stream()
                                .flatMap(seller -> seller.getItems().stream())
                                .sorted(Comparator.comparing(
                                                item -> item.getProduct().getId()))
                                .toList();

                if (items.isEmpty()) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Order has no items");
                }

                // Validate seller and item states before changing inventory.
                for (var seller : order.getSellerOrders()) {

                        if (!"CANCELLED".equals(seller.getStatus())) {
                                throw new ResponseStatusException(
                                                HttpStatus.CONFLICT,
                                                "Seller order is not in the expected cancelled state");
                        }

                        for (var item : seller.getItems()) {

                                if (!"CANCELLED".equals(item.getStatus())) {
                                        throw new ResponseStatusException(
                                                        HttpStatus.CONFLICT,
                                                        "Order item is not in the expected cancelled state");
                                }
                        }
                }

                // Lock existing reservation records.
                List<InventoryReservationEntity> lockedReservations = new ArrayList<>();

                for (var item : items) {

                        var reservation = reservations
                                        .findByOrderItemId(item.getId())
                                        .orElseThrow(() -> new ResponseStatusException(
                                                        HttpStatus.CONFLICT,
                                                        "Inventory reservation is missing"));

                        if (!"RELEASED".equals(reservation.getStatus())
                                        || reservation.getQuantity() != item.getQuantity()) {

                                throw new ResponseStatusException(
                                                HttpStatus.CONFLICT,
                                                "Inventory reservation cannot be reactivated");
                        }

                        lockedReservations.add(reservation);
                }

                // Lock each distinct product inventory row in a consistent order.
                Map<Long, InventoryEntity> lockedInventory = new LinkedHashMap<>();

                for (var item : items) {

                        Long productId = item.getProduct().getId();

                        if (!lockedInventory.containsKey(productId)) {

                                var stock = inventory.findByProductIdForUpdate(productId)
                                                .orElseThrow(() -> new ResponseStatusException(
                                                                HttpStatus.CONFLICT,
                                                                "Inventory record was not found"));

                                lockedInventory.put(productId, stock);
                        }
                }

                // Reserve and immediately commit stock because payment is captured.
                // Sequential processing also handles multiple items for one product.
                var newExpiry = clock.instant().plus(Duration.ofMinutes(15));

                for (int i = 0; i < items.size(); i++) {

                        var item = items.get(i);

                        var stock = lockedInventory.get(
                                        item.getProduct().getId());

                        var reservation = lockedReservations.get(i);

                        try {
                                stock.reserve(item.getQuantity());
                                reservation.reactivate(newExpiry);

                                stock.commitReservation(item.getQuantity());
                                reservation.commit();

                        } catch (IllegalStateException ex) {

                                throw new ResponseStatusException(
                                                HttpStatus.CONFLICT,
                                                "Insufficient stock or invalid reservation state",
                                                ex);
                        }

                        item.setStatus("ACTIVE");
                }

                // Restore seller orders.
                for (var seller : order.getSellerOrders()) {

                        String oldStatus = seller.getStatus();

                        seller.setStatus("CONFIRMED");

                        sellerHistory.save(
                                        new SellerOrderStatusHistoryEntity(
                                                        seller,
                                                        admin,
                                                        oldStatus,
                                                        "CONFIRMED",
                                                        "Late payment reviewed and fulfillment approved"));
                }

                // Restore customer order. Payment remains PAID.
                order.setStatus("CONFIRMED");

                history.save(
                                new OrderStatusHistoryEntity(
                                                order,
                                                admin,
                                                "PAYMENT_REVIEW_REQUIRED",
                                                "CONFIRMED",
                                                "Admin approved late payment fulfillment and inventory was committed"));
        }

        @Transactional
        public AdminRefundResponse refund(
                        Long adminId,
                        Long orderId,
                        String reason) {

                var payment = payments.findByOrderIdForUpdate(orderId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.CONFLICT,
                                                "Only captured online payments can be refunded"));

                var order = orders.findByIdForUpdate(orderId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Order was not found"));

                if (!"CAPTURED".equals(payment.getStatus())
                                || !List.of(
                                                "CONFIRMED",
                                                "PROCESSING",
                                                "SHIPPED",
                                                "DELIVERED",
                                                "PAYMENT_REVIEW_REQUIRED")
                                                .contains(order.getStatus())) {

                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "This order is not eligible for a refund");
                }

                if (refunds.existsByPaymentId(payment.getId())) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "A refund was already requested for this order");
                }

                var admin = users.findById(adminId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Administrator was not found"));

                RefundEntity refund = refunds.saveAndFlush(
                                new RefundEntity(
                                                payment,
                                                admin,
                                                reason.trim(),
                                                order.getStatus()));

                String previous = order.getStatus();

                order.setStatus("REFUND_PENDING");
                payment.refundPending();

                history.save(
                                new OrderStatusHistoryEntity(
                                                order,
                                                admin,
                                                previous,
                                                "REFUND_PENDING",
                                                "Full refund requested: " + reason.trim()));

                RazorpayGateway.RefundResult provider = gateway.refund(
                                payment.getProviderPaymentId(),
                                reason.trim());

                refund.submitted(provider.id(), provider.status());

                if ("PROCESSED".equals(refund.getStatus()))
                        finish(refund, admin);
                else if ("FAILED".equals(refund.getStatus()))
                        restoreAfterFailure(refund, admin);

                return response(refund);
        }

        @Transactional
        public void refundWebhook(
                        String providerRefundId,
                        String event) {

                if (!List.of("refund.processed", "refund.failed").contains(event))
                        return;

                RefundEntity refund = refunds.findByProviderRefundIdForUpdate(providerRefundId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Refund was not found"));

                if (List.of("PROCESSED", "FAILED").contains(refund.getStatus()))
                        return;

                if ("refund.processed".equals(event)) {

                        refund.processed();
                        finish(refund, null);

                } else if ("refund.failed".equals(event)) {

                        refund.failed();
                        restoreAfterFailure(refund, null);
                }
        }

        private void finish(
                        RefundEntity refund,
                        com.markethub.identity.infrastructure.jpa.UserEntity actor) {

                var order = refund.getOrder();

                String old = order.getStatus();

                order.setStatus("REFUNDED");
                order.markRefunded();
                refund.getPayment().refunded();

                history.save(
                                new OrderStatusHistoryEntity(
                                                order,
                                                actor,
                                                old,
                                                "REFUNDED",
                                                "Provider refund processed"));

                for (var seller : order.getSellerOrders()) {

                        String sellerOld = seller.getStatus();

                        seller.setStatus("REFUNDED");

                        seller.getItems().forEach(
                                        item -> item.setStatus("REFUNDED"));

                        sellerHistory.save(
                                        new SellerOrderStatusHistoryEntity(
                                                        seller,
                                                        actor,
                                                        sellerOld,
                                                        "REFUNDED",
                                                        "Order refunded"));
                }
        }

        private void restoreAfterFailure(
                        RefundEntity refund,
                        com.markethub.identity.infrastructure.jpa.UserEntity actor) {

                var order = refund.getOrder();

                refund.getPayment().refundFailed();

                order.setStatus(refund.getOrderStatusBefore());

                history.save(
                                new OrderStatusHistoryEntity(
                                                order,
                                                actor,
                                                "REFUND_PENDING",
                                                refund.getOrderStatusBefore(),
                                                "Provider refund failed"));
        }

        private AdminRefundResponse response(RefundEntity r) {

                return new AdminRefundResponse(
                                r.getId(),
                                r.getOrder().getId(),
                                r.getStatus(),
                                r.getAmount(),
                                r.getCurrency(),
                                r.getProviderRefundId());
        }
}
