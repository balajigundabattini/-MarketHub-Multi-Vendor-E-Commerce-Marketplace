package com.markethub.orders.application;

import java.util.List;
import java.util.Map;
import com.markethub.identity.infrastructure.jpa.UserRepository;
import com.markethub.catalog.infrastructure.jpa.InventoryMovementEntity;
import com.markethub.catalog.infrastructure.jpa.InventoryMovementRepository;
import com.markethub.catalog.infrastructure.jpa.InventoryRepository;
import com.markethub.orders.infrastructure.jpa.InventoryReservationRepository;
import com.markethub.orders.api.SellerOrderResponse;
import com.markethub.orders.infrastructure.jpa.SellerOrderEntity;
import com.markethub.orders.infrastructure.jpa.SellerOrderRepository;
import com.markethub.orders.infrastructure.jpa.SellerOrderStatusHistoryEntity;
import com.markethub.orders.infrastructure.jpa.SellerOrderStatusHistoryRepository;
import com.markethub.orders.infrastructure.jpa.ShipmentEntity;
import com.markethub.orders.infrastructure.jpa.ShipmentRepository;
import com.markethub.orders.infrastructure.jpa.OrderItemRequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SellerFulfillmentService {
        private final SellerOrderRepository orders;
        private final ShipmentRepository shipments;
        private final SellerOrderStatusHistoryRepository history;
        private final UserRepository users;
        private final InventoryRepository inventory;
        private final InventoryReservationRepository reservations;
        private final OrderItemRequestRepository itemRequests;
        private final NotificationService notifications;
        private final OrderStatusAggregationService orderStatusAggregation;
        private final InventoryMovementRepository movements;

        public SellerFulfillmentService(
                        SellerOrderRepository orders,
                        ShipmentRepository shipments,
                        SellerOrderStatusHistoryRepository history,
                        UserRepository users,
                        InventoryRepository inventory,
                        InventoryReservationRepository reservations,
                        OrderItemRequestRepository itemRequests,
                        NotificationService notifications,
                        OrderStatusAggregationService orderStatusAggregation, InventoryMovementRepository movements) {

                this.orders = orders;
                this.shipments = shipments;
                this.history = history;
                this.users = users;
                this.inventory = inventory;
                this.reservations = reservations;
                this.itemRequests = itemRequests;
                this.notifications = notifications;
                this.orderStatusAggregation = orderStatusAggregation;
                this.movements = movements;
        }

        @Transactional(readOnly = true)
        public Page<SellerOrderResponse> list(Long userId, int page, int size) {
                if (page < 0 || size < 1 || size > 100)
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page or page size is invalid");
                return orders
                                .findAllBySeller_User_Id(userId,
                                                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                                .map(this::response);
        }

        @Transactional
        public SellerOrderResponse update(Long userId, Long id, String target) {
                SellerOrderEntity order = owned(userId, id);
                Map<String, List<String>> transitions = Map.of("SHIPPED", List.of("DELIVERED"), "CONFIRMED",
                                List.of("ACCEPTED", "REJECTED"),
                                "ACCEPTED", List.of("PROCESSING"), "CANCELLATION_REQUESTED", List.of("CANCELLED"));
                if (!transitions.getOrDefault(order.getStatus(), List.of()).contains(target))
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                        "This seller order cannot move from " + order.getStatus() + " to " + target);
                if (("REJECTED".equals(target) || "CANCELLED".equals(target))
                                && "PAID".equals(order.getOrder().getPaymentStatus()))
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                        "Paid orders require a refund workflow before rejection");
                var actor = users.findById(userId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Seller account was not found"));
                String previous = order.getStatus();
                order.setStatus(target);
                history.save(new SellerOrderStatusHistoryEntity(order, actor, previous, target,
                                "Seller updated order status"));
                if ("DELIVERED".equals(target)) {
                        shipments.findAllBySellerOrderId(id).forEach(ShipmentEntity::markDelivered);
                        order.getItems().forEach(item -> {
                                if ("SHIPPED".equals(item.getStatus()))
                                        item.setStatus("DELIVERED");
                        });
                        notifications.create(order.getOrder().getCustomer().getId(), "Order delivered",
                                        "Your order " + order.getOrder().getPublicOrderNumber()
                                                        + " was marked delivered.",
                                        order.getOrder());
                }

                if ("REJECTED".equals(target) || "CANCELLED".equals(target)) {

                        for (var item : order.getItems()) {

                                if ("CANCELLED".equals(target)
                                                && !"CANCEL_REQUESTED".equals(item.getStatus())) {
                                        continue;
                                }

                                if ("REJECTED".equals(target)
                                                && !"ACTIVE".equals(item.getStatus())) {
                                        continue;
                                }

                                var reservation = reservations.findByOrderItemId(item.getId())
                                                .orElseThrow(() -> new ResponseStatusException(
                                                                HttpStatus.CONFLICT,
                                                                "Inventory reservation was not found"));

                                if (!"ACTIVE".equals(reservation.getStatus())) {
                                        throw new ResponseStatusException(
                                                        HttpStatus.CONFLICT,
                                                        "Inventory reservation is not active");
                                }

                                var stock = inventory.findByProductIdForUpdate(
                                                reservation.getProduct().getId())
                                                .orElseThrow(() -> new ResponseStatusException(
                                                                HttpStatus.CONFLICT,
                                                                "Product inventory was not found"));

                                stock.release(reservation.getQuantity());
                                reservation.release();

                                item.setStatus("CANCELLED");
                        }
                }
                // Recalculate only after all item status changes
                orderStatusAggregation.recalculate(order.getOrder());
                return response(order);
        }

        @Transactional
        public SellerOrderResponse ship(Long userId, Long id, String carrier, String trackingNumber) {
                SellerOrderEntity order = owned(userId, id);
                if (!"PROCESSING".equals(order.getStatus()))
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Only processing orders can be shipped");
                if (shipments.findAllBySellerOrderId(id).stream()
                                .anyMatch(s -> s.getTrackingNumber().equalsIgnoreCase(trackingNumber.trim())))
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                        "This tracking number is already recorded");
                shipments.save(new ShipmentEntity(order, carrier.trim(), trackingNumber.trim()));
                for (var item : order.getItems()) {

                        if (!"ACTIVE".equals(item.getStatus())) {
                                continue;
                        }

                        var reservation = reservations.findByOrderItemId(item.getId())
                                        .orElseThrow(() -> new ResponseStatusException(
                                                        HttpStatus.CONFLICT, "Active inventory reservation not found"));

                        if (!"ACTIVE".equals(reservation.getStatus())) {
                                throw new ResponseStatusException(
                                                HttpStatus.CONFLICT, "Inventory reservation is not active");
                        }

                        var stock = inventory.findByProductIdForUpdate(reservation.getProduct().getId())
                                        .orElseThrow(() -> new ResponseStatusException(
                                                        HttpStatus.CONFLICT, "Inventory record not found"));

                        stock.commitReservation(reservation.getQuantity());
                        reservation.commit();

                        item.setStatus("SHIPPED");
                }
                order.setStatus("SHIPPED");
                var actor = users.findById(userId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Seller account was not found"));
                history.save(new SellerOrderStatusHistoryEntity(order, actor, "PROCESSING", "SHIPPED",
                                "Shipment recorded"));

                orderStatusAggregation.recalculate(order.getOrder());
                return response(order);
        }

        @Transactional
        public SellerOrderResponse reviewReturn(Long userId, Long sellerOrderId, Long requestId, String decision,
                        String note) {
                var order = owned(userId, sellerOrderId);
                if (!List.of("APPROVED", "REJECTED").contains(decision))
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Return decision is invalid");
                var request = itemRequests.findByIdAndOrderItem_SellerOrder_Seller_User_Id(requestId, userId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Return request was not found"));
                if (!request.getOrderItem().getSellerOrder().getId().equals(sellerOrderId)
                                || !"RETURN".equals(request.getRequestType())
                                || !"REQUESTED".equals(request.getStatus()))
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                        "This return request is no longer pending");
                var actor = users.findById(userId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Seller account was not found"));
                request.review(actor, decision, note == null ? null : note.trim());
                request.getOrderItem().setStatus("APPROVED".equals(decision) ? "RETURN_APPROVED" : "DELIVERED");
                notifications.create(order.getOrder().getCustomer().getId(), "Return request updated",
                                "Your return request was " + decision.toLowerCase() + ".", order.getOrder());
                history.save(new SellerOrderStatusHistoryEntity(order, actor, order.getStatus(), order.getStatus(),
                                "Return request " + decision.toLowerCase()));
                return response(order);
        }

        @Transactional
        public SellerOrderResponse reviewCancellation(
                        Long userId,
                        Long sellerOrderId,
                        Long requestId,
                        String decision,
                        String note) {

                var order = owned(userId, sellerOrderId);

                if (!List.of("APPROVED", "REJECTED").contains(decision)) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Cancellation decision is invalid");
                }

                var request = itemRequests
                                .findByIdAndOrderItem_SellerOrder_Seller_User_Id(requestId, userId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Cancellation request was not found"));

                if (!request.getOrderItem().getSellerOrder().getId().equals(sellerOrderId)
                                || !"CANCELLATION".equals(request.getRequestType())
                                || !"REQUESTED".equals(request.getStatus())) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "This cancellation request is no longer pending");
                }

                var item = request.getOrderItem();

                if (!"CANCEL_REQUESTED".equals(item.getStatus())) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "This item is no longer awaiting cancellation");
                }

                if ("PAID".equals(order.getOrder().getPaymentStatus())) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Paid orders require a refund workflow");
                }

                var actor = users.findById(userId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Seller account was not found"));

                String previousSellerStatus = order.getStatus();

                if ("APPROVED".equals(decision)) {

                        var reservation = reservations.findByOrderItemId(item.getId())
                                        .orElseThrow(() -> new ResponseStatusException(
                                                        HttpStatus.CONFLICT,
                                                        "Inventory reservation was not found"));

                        if ("ACTIVE".equals(reservation.getStatus())) {
                                var stock = inventory.findByProductIdForUpdate(
                                                reservation.getProduct().getId())
                                                .orElseThrow(() -> new ResponseStatusException(
                                                                HttpStatus.CONFLICT,
                                                                "Product inventory was not found"));

                                stock.release(reservation.getQuantity());
                                reservation.release();

                        } else {
                                throw new ResponseStatusException(
                                                HttpStatus.CONFLICT,
                                                "Inventory reservation is no longer active");
                        }

                        item.setStatus("CANCELLED");

                } else {
                        item.setStatus("ACTIVE");
                }

                request.review(
                                actor,
                                decision,
                                note == null ? null : note.trim());

                // Cancel the entire seller order only when every item is cancelled.
                boolean allItemsCancelled = order.getItems().stream()
                                .allMatch(i -> "CANCELLED".equals(i.getStatus()));

                if (allItemsCancelled) {
                        order.setStatus("CANCELLED");
                }
                orderStatusAggregation.recalculate(order.getOrder());

                history.save(new SellerOrderStatusHistoryEntity(
                                order,
                                actor,
                                previousSellerStatus,
                                order.getStatus(),
                                "Cancellation request " + decision.toLowerCase()));

                notifications.create(
                                order.getOrder().getCustomer().getId(),
                                "Cancellation request updated",
                                "Your cancellation request was " + decision.toLowerCase() + ".",
                                order.getOrder());

                return response(order);
        }

        @Transactional
        public SellerOrderResponse receiveReturn(Long userId, Long sellerOrderId, Long requestId) {
                var order = owned(userId, sellerOrderId);
                var request = itemRequests.findByIdAndSellerUserIdForUpdate(requestId, userId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Return request was not found"));
                if (!request.getOrderItem().getSellerOrder().getId().equals(sellerOrderId)
                                || !"RETURN".equals(request.getRequestType())
                                || !"APPROVED".equals(request.getStatus()))
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                        "Only an approved return can be marked received");
                var item = request.getOrderItem();
                var stock = inventory.findByProductIdForUpdate(item.getProduct().getId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                                                "Product inventory was not found"));
                var actor = users.findById(userId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Seller account was not found"));

                stock.setOnHand(stock.getOnHand() + item.getQuantity());

                movements.save(new InventoryMovementEntity(
                                item.getProduct(),
                                item.getQuantity(),
                                "RETURN_RESTOCK",
                                actor));
                request.review(actor, "RECEIVED", "Returned item received and restocked");
                item.setStatus("RETURNED");
                history.save(new SellerOrderStatusHistoryEntity(order, actor, order.getStatus(), order.getStatus(),
                                "Returned item received and restocked"));
                orderStatusAggregation.recalculate(order.getOrder());
                return response(order);
        }

        private SellerOrderEntity owned(Long userId, Long id) {
                return orders.findOwnedForUpdate(id, userId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Seller order was not found"));
        }

        private SellerOrderResponse response(SellerOrderEntity order) {
                var itemRows = order.getItems().stream()
                                .map(item -> new SellerOrderResponse.Item(
                                                item.getId(),
                                                item.getNameSnapshot(),
                                                item.getQuantity()))
                                .toList();

                var shipmentRows = shipments.findAllBySellerOrderId(order.getId()).stream()
                                .map(s -> new SellerOrderResponse.Shipment(
                                                s.getId(),
                                                s.getStatus(),
                                                s.getCarrier(),
                                                s.getTrackingNumber(),
                                                s.getShippedAt()))
                                .toList();

                var returnRows = itemRequests
                                .findAllByOrderItem_SellerOrder_IdAndRequestTypeAndStatusIn(
                                                order.getId(),
                                                "RETURN",
                                                List.of("REQUESTED", "APPROVED", "REJECTED", "RECEIVED"))
                                .stream()
                                .map(r -> new SellerOrderResponse.ReturnRequest(
                                                r.getId(),
                                                r.getOrderItem().getId(),
                                                r.getOrderItem().getNameSnapshot(),
                                                r.getOrderItem().getQuantity(),
                                                r.getReason(),
                                                r.getStatus(),
                                                r.getCreatedAt()))
                                .toList();

                String status = order.getStatus();

                return new SellerOrderResponse(
                                order.getId(),
                                order.getOrder().getPublicOrderNumber(),
                                order.getCreatedAt(),
                                status,
                                itemRows,
                                shipmentRows,
                                returnRows,
                                "CONFIRMED".equals(status),
                                "CONFIRMED".equals(status),
                                "ACCEPTED".equals(status),
                                "PROCESSING".equals(status),
                                "SHIPPED".equals(status),
                                "CANCELLATION_REQUESTED".equals(status)
                                                && !"PAID".equals(order.getOrder().getPaymentStatus()));
        }

}
