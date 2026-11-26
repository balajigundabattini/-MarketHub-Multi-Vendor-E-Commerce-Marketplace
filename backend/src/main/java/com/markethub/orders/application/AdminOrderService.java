
package com.markethub.orders.application;

import java.math.BigDecimal;
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
import com.markethub.orders.api.AdminCancellationRequestResponse;
import com.markethub.orders.api.AdminCancellationReviewRequest;
import com.markethub.orders.api.AdminOrderResponse;
import com.markethub.orders.api.AdminRefundResponse;
import com.markethub.orders.infrastructure.jpa.InventoryReservationEntity;
import com.markethub.orders.infrastructure.jpa.InventoryReservationRepository;
import com.markethub.orders.infrastructure.jpa.OrderItemRequestEntity;
import com.markethub.orders.infrastructure.jpa.OrderItemRequestRepository;
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
        private final OrderItemRequestRepository itemRequests;
        private final OrderStatusAggregationService orderStatusAggregation;

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
                        Clock clock, OrderItemRequestRepository itemRequests,
                        OrderStatusAggregationService orderStatusAggregation) {

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
                this.itemRequests = itemRequests;
                this.orderStatusAggregation = orderStatusAggregation;
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

                if ("refund.processed".equals(event) || "processed".equals(event)) {

                        refund.processed();
                        finish(refund, null);

                } else if ("refund.failed".equals(event) || "failed".equals(event)) {

                        refund.failed();
                        restoreAfterFailure(refund, null);
                }
        }

        private void finish(
                        RefundEntity refund,
                        com.markethub.identity.infrastructure.jpa.UserEntity actor) {
                if (refund.getOrderItem() != null) {
                        finishPartialRefund(refund, actor);
                        return;
                }

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

                // Partial refund failure
                if (refund.getOrderItem() != null) {

                        var item = refund.getOrderItem();

                        String previousOrderStatus = order.getStatus();

                        item.setStatus("ACTIVE");

                        orderStatusAggregation.recalculate(order);

                        // If the processed refunds now cover the whole payment, mark it refunded.
                        var payment = refund.getPayment();

                        BigDecimal refundedTotal = refunds.findAllByPaymentId(payment.getId()).stream()
                                        .filter(r -> "PROCESSED".equals(r.getStatus()))
                                        .map(RefundEntity::getAmount)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                        if (refundedTotal.compareTo(payment.getAmount()) >= 0) {
                                payment.refunded();
                                order.markRefunded();
                        }

                        history.save(new OrderStatusHistoryEntity(
                                        order,
                                        actor,
                                        previousOrderStatus,
                                        order.getStatus(),
                                        "Partial refund failed for item "
                                                        + item.getId()
                                                        + ". Item restored to ACTIVE"));

                        return;
                }

                // Full-order refund failure
                refund.getPayment().refundFailed();

                order.setStatus(refund.getOrderStatusBefore());

                history.save(new OrderStatusHistoryEntity(
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

        @Transactional(readOnly = true)
        public List<AdminCancellationRequestResponse> cancellationRequests() {

                return itemRequests
                                .findAllByRequestTypeAndStatusOrderByCreatedAtDesc(
                                                "CANCELLATION",
                                                "REQUESTED")
                                .stream()
                                .map(request -> {

                                        var item = request.getOrderItem();
                                        var sellerOrder = item.getSellerOrder();
                                        var order = sellerOrder.getOrder();

                                        return new AdminCancellationRequestResponse(
                                                        request.getId(),
                                                        order.getId(),
                                                        order.getPublicOrderNumber(),
                                                        item.getId(),
                                                        item.getNameSnapshot(),
                                                        item.getQuantity(),
                                                        request.getCustomer().getDisplayName(),
                                                        request.getReason(),
                                                        request.getStatus(),
                                                        request.getCreatedAt());
                                })
                                .toList();
        }

        @Transactional
        public void reviewCancellationRequest(
                        Long adminId,
                        Long requestId,
                        AdminCancellationReviewRequest review) {

                OrderItemRequestEntity request = itemRequests
                                .findByIdForUpdate(requestId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Cancellation request was not found"));

                if (!"CANCELLATION".equals(request.getRequestType())
                                || !"REQUESTED".equals(request.getStatus())) {

                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "This cancellation request has already been reviewed");
                }

                var item = request.getOrderItem();

                var order = orders.findByIdForUpdate(
                                item.getSellerOrder().getOrder().getId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Order was not found"));

                if (!"CANCEL_REQUESTED".equals(item.getStatus())) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Order item is no longer awaiting cancellation review");
                }

                var admin = users.findById(adminId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Administrator was not found"));

                String decision = review.status();
                String note = review.note() == null ? "" : review.note().trim();

                String previousOrderStatus = order.getStatus();

                if ("APPROVED".equals(decision)) {

                        if ("PAID".equals(order.getPaymentStatus())) {

                                // Paid orders require an individual-item refund.
                                processPaidItemCancellation(
                                                admin,
                                                request,
                                                item,
                                                order,
                                                note);

                                return;
                        }

                        // Existing unpaid cancellation workflow.
                        var reservation = reservations
                                        .findByOrderItemIdForUpdate(item.getId())
                                        .orElseThrow(() -> new ResponseStatusException(
                                                        HttpStatus.CONFLICT,
                                                        "Inventory reservation was not found"));

                        if (!"ACTIVE".equals(reservation.getStatus())
                                        || reservation.getQuantity() != item.getQuantity()) {

                                throw new ResponseStatusException(
                                                HttpStatus.CONFLICT,
                                                "Inventory reservation is not valid for cancellation");
                        }

                        var stock = inventory.findByProductIdForUpdate(
                                        reservation.getProduct().getId())
                                        .orElseThrow(() -> new ResponseStatusException(
                                                        HttpStatus.CONFLICT,
                                                        "Product inventory was not found"));

                        try {
                                stock.release(reservation.getQuantity());
                                reservation.release();
                        } catch (IllegalStateException ex) {
                                throw new ResponseStatusException(
                                                HttpStatus.CONFLICT,
                                                "Inventory reservation could not be released",
                                                ex);
                        }

                        item.setStatus("CANCELLED");

                } else if ("REJECTED".equals(decision)) {

                        item.setStatus("ACTIVE");

                } else {

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Invalid cancellation decision");
                }

                request.review(admin, decision, note);

                orderStatusAggregation.recalculate(order);

                history.save(new OrderStatusHistoryEntity(
                                order,
                                admin,
                                previousOrderStatus,
                                order.getStatus(),
                                "Admin " + decision.toLowerCase()
                                                + " cancellation request #" + requestId
                                                + (note.isBlank() ? "" : ": " + note)));
        }

        private void processPaidItemCancellation(
                        com.markethub.identity.infrastructure.jpa.UserEntity admin,
                        OrderItemRequestEntity request,
                        com.markethub.orders.infrastructure.jpa.OrderItemEntity item,
                        com.markethub.orders.infrastructure.jpa.OrderEntity order,
                        String note) {

                var payment = payments.findByOrderIdForUpdate(order.getId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.CONFLICT,
                                                "Payment record was not found"));

                if (!"CAPTURED".equals(payment.getStatus())
                                || payment.getProviderPaymentId() == null
                                || payment.getProviderPaymentId().isBlank()
                                || !"RAZORPAY".equals(order.getPaymentMethod())) {

                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "This paid order does not have a refundable Razorpay payment");
                }

                // Ensure the item still has an active inventory reservation.
                var reservation = reservations
                                .findByOrderItemIdForUpdate(item.getId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.CONFLICT,
                                                "Inventory reservation was not found"));

                if ((!"ACTIVE".equals(reservation.getStatus())
                                && !"COMMITTED".equals(reservation.getStatus()))
                                || reservation.getQuantity() != item.getQuantity()) {

                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Inventory reservation is not valid for cancellation");
                }

                // Prevent another active or completed refund for this item.
                if (refunds.existsByOrderItemIdAndStatusIn(
                                item.getId(),
                                List.of("REQUESTED", "PROCESSING", "PROCESSED"))) {

                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "A refund already exists for this order item");
                }

                List<RefundEntity> existingRefunds = refunds.findAllByPaymentId(payment.getId());

                // A full-order refund and individual-item refunds must not overlap.
                boolean fullRefundExists = existingRefunds.stream()
                                .anyMatch(r -> r.getOrderItem() == null
                                                && !"FAILED".equals(r.getStatus()));

                if (fullRefundExists) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "A full-order refund already exists for this payment");
                }

                // Prevent total requested/completed refunds from exceeding payment amount.
                BigDecimal alreadyRefunded = existingRefunds.stream()
                                .filter(r -> List.of(
                                                "REQUESTED",
                                                "PROCESSING",
                                                "PROCESSED").contains(r.getStatus()))
                                .map(RefundEntity::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal refundAmount = item.getLineTotal();

                if (alreadyRefunded.add(refundAmount)
                                .compareTo(payment.getAmount()) > 0) {

                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Refund amount exceeds the remaining paid amount");
                }

                RefundEntity refund = refunds.saveAndFlush(
                                new RefundEntity(
                                                payment,
                                                admin,
                                                item,
                                                request.getReason(),
                                                order.getStatus(),
                                                refundAmount));

                // Request a refund for this item only.
                RazorpayGateway.RefundResult provider = gateway.refund(
                                payment.getProviderPaymentId(),
                                refundAmount,
                                payment.getCurrency(),
                                request.getReason());

                refund.submitted(provider.id(), provider.status());

                if ("FAILED".equals(refund.getStatus())) {

                        request.review(
                                        admin,
                                        "REJECTED",
                                        note.isBlank()
                                                        ? "Refund failed at payment provider"
                                                        : note + " - Refund failed at payment provider");

                        restoreAfterFailure(refund, admin);
                        return;
                }

                request.review(admin, "APPROVED", note);

                // If Razorpay has already processed it, complete immediately.
                // Otherwise, the refund webhook will complete it later.
                if ("PROCESSED".equals(refund.getStatus())) {
                        finish(refund, admin);
                }

                history.save(new OrderStatusHistoryEntity(
                                order,
                                admin,
                                order.getStatus(),
                                order.getStatus(),
                                "Individual item refund requested for item "
                                                + item.getId()
                                                + ", amount " + refundAmount));
        }

        private void finishPartialRefund(
                        RefundEntity refund,
                        com.markethub.identity.infrastructure.jpa.UserEntity actor) {

                var item = refund.getOrderItem();
                var order = refund.getOrder();

                var reservation = reservations
                                .findByOrderItemIdForUpdate(item.getId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.CONFLICT,
                                                "Inventory reservation was not found"));

                if ((!"ACTIVE".equals(reservation.getStatus())
                                && !"COMMITTED".equals(reservation.getStatus()))
                                || reservation.getQuantity() != item.getQuantity()) {

                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Inventory reservation cannot be released");
                }

                var stock = inventory.findByProductIdForUpdate(
                                reservation.getProduct().getId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.CONFLICT,
                                                "Product inventory was not found"));

                try {
                        if ("ACTIVE".equals(reservation.getStatus())) {

                                stock.release(reservation.getQuantity());
                                reservation.release();

                        } else if ("COMMITTED".equals(reservation.getStatus())) {

                                stock.restoreCommittedStock(reservation.getQuantity());
                                reservation.cancelCommitted();

                        } else {

                                throw new ResponseStatusException(
                                                HttpStatus.CONFLICT,
                                                "Inventory reservation cannot be released");
                        }
                } catch (IllegalStateException ex) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Inventory reservation could not be released",
                                        ex);
                }

                String previousOrderStatus = order.getStatus();

                item.setStatus("CANCELLED");

                // Recalculate the parent order without cancelling other items.
                orderStatusAggregation.recalculate(order);

                history.save(new OrderStatusHistoryEntity(
                                order,
                                actor,
                                previousOrderStatus,
                                order.getStatus(),
                                "Partial refund processed for item "
                                                + item.getId()
                                                + ", amount " + refund.getAmount()));
        }

}
