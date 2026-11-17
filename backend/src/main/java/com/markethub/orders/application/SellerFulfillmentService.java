package com.markethub.orders.application;

import java.util.List;
import java.util.Map;
import com.markethub.identity.infrastructure.jpa.UserRepository;
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
    public SellerFulfillmentService(SellerOrderRepository orders, ShipmentRepository shipments,
            SellerOrderStatusHistoryRepository history, UserRepository users, InventoryRepository inventory,
            InventoryReservationRepository reservations, OrderItemRequestRepository itemRequests) {
        this.orders = orders; this.shipments = shipments; this.history = history; this.users = users;
        this.inventory = inventory; this.reservations = reservations; this.itemRequests = itemRequests;
    }
    @Transactional(readOnly = true) public Page<SellerOrderResponse> list(Long userId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page or page size is invalid");
        return orders.findAllBySeller_User_Id(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))).map(this::response);
    }
    @Transactional public SellerOrderResponse update(Long userId, Long id, String target) {
        SellerOrderEntity order = owned(userId, id);
        Map<String, List<String>> transitions = Map.of("SHIPPED", List.of("DELIVERED"), "CONFIRMED", List.of("ACCEPTED", "REJECTED"),
                "ACCEPTED", List.of("PROCESSING"), "CANCELLATION_REQUESTED", List.of("CANCELLED"));
        if (!transitions.getOrDefault(order.getStatus(), List.of()).contains(target))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This seller order cannot move from " + order.getStatus() + " to " + target);
        if (("REJECTED".equals(target) || "CANCELLED".equals(target)) && "PAID".equals(order.getOrder().getPaymentStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Paid orders require a refund workflow before rejection");
        var actor = users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller account was not found"));
        String previous = order.getStatus(); order.setStatus(target);
        history.save(new SellerOrderStatusHistoryEntity(order, actor, previous, target, "Seller updated order status"));
        if ("DELIVERED".equals(target)) {
            shipments.findAllBySellerOrderId(id).forEach(ShipmentEntity::markDelivered);
            order.getItems().forEach(item -> { if ("SHIPPED".equals(item.getStatus())) item.setStatus("DELIVERED"); });
        }
        if ("REJECTED".equals(target) || "CANCELLED".equals(target)) for (var item : order.getItems()) {
            if (!"CANCEL_REQUESTED".equals(item.getStatus()) && "CANCELLED".equals(target)) continue;
            item.setStatus("CANCELLED");
            reservations.findByOrderItemId(item.getId()).ifPresent(reservation -> {
                if ("ACTIVE".equals(reservation.getStatus())) {
                    inventory.findByProductIdForUpdate(reservation.getProduct().getId()).ifPresent(stock -> stock.release(reservation.getQuantity()));
                    reservation.release();
                }
            });
        }
        return response(order);
    }
    @Transactional public SellerOrderResponse ship(Long userId, Long id, String carrier, String trackingNumber) {
        SellerOrderEntity order = owned(userId, id);
        if (!"PROCESSING".equals(order.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Only processing orders can be shipped");
        if (shipments.findAllBySellerOrderId(id).stream().anyMatch(s -> s.getTrackingNumber().equalsIgnoreCase(trackingNumber.trim())))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This tracking number is already recorded");
        shipments.save(new ShipmentEntity(order, carrier.trim(), trackingNumber.trim()));
        for (var item : order.getItems()) reservations.findByOrderItemId(item.getId()).ifPresent(reservation -> {
            if ("ACTIVE".equals(reservation.getStatus())) {
                inventory.findByProductIdForUpdate(reservation.getProduct().getId()).ifPresent(stock -> stock.commitReservation(reservation.getQuantity()));
                reservation.commit();
            }
        });
        order.setStatus("SHIPPED");
        var actor = users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller account was not found"));
        history.save(new SellerOrderStatusHistoryEntity(order, actor, "PROCESSING", "SHIPPED", "Shipment recorded"));
        order.getItems().forEach(item -> item.setStatus("SHIPPED"));
        return response(order);
    }
    @Transactional public SellerOrderResponse reviewReturn(Long userId, Long sellerOrderId, Long requestId, String decision, String note) {
        var order = owned(userId, sellerOrderId);
        if (!List.of("APPROVED", "REJECTED").contains(decision)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Return decision is invalid");
        var request = itemRequests.findByIdAndOrderItem_SellerOrder_Seller_User_Id(requestId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Return request was not found"));
        if (!request.getOrderItem().getSellerOrder().getId().equals(sellerOrderId) || !"RETURN".equals(request.getRequestType()) || !"REQUESTED".equals(request.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This return request is no longer pending");
        var actor = users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller account was not found"));
        request.review(actor, decision, note == null ? null : note.trim());
        request.getOrderItem().setStatus("APPROVED".equals(decision) ? "RETURN_APPROVED" : "DELIVERED");
        history.save(new SellerOrderStatusHistoryEntity(order, actor, order.getStatus(), order.getStatus(), "Return request " + decision.toLowerCase()));
        return response(order);
    }
    private SellerOrderEntity owned(Long userId, Long id) {
        return orders.findOwnedForUpdate(id, userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller order was not found"));
    }
    private SellerOrderResponse response(SellerOrderEntity order) {
        var itemRows = order.getItems().stream().map(item -> new SellerOrderResponse.Item(item.getId(), item.getNameSnapshot(), item.getQuantity())).toList();
        var shipmentRows = shipments.findAllBySellerOrderId(order.getId()).stream().map(s -> new SellerOrderResponse.Shipment(s.getId(), s.getStatus(), s.getCarrier(), s.getTrackingNumber(), s.getShippedAt())).toList();
        var returnRows = itemRequests.findAllByOrderItem_SellerOrder_IdAndRequestTypeAndStatus(order.getId(), "RETURN", "REQUESTED").stream()
                .map(r -> new SellerOrderResponse.ReturnRequest(r.getId(), r.getOrderItem().getId(), r.getOrderItem().getNameSnapshot(), r.getOrderItem().getQuantity(), r.getReason(), r.getCreatedAt())).toList();
        String status = order.getStatus();
        return new SellerOrderResponse(order.getId(), order.getOrder().getPublicOrderNumber(), order.getCreatedAt(), status,
                itemRows, shipmentRows, returnRows, "CONFIRMED".equals(status), "CONFIRMED".equals(status), "ACCEPTED".equals(status), "PROCESSING".equals(status), "SHIPPED".equals(status),
                "CANCELLATION_REQUESTED".equals(status) && !"PAID".equals(order.getOrder().getPaymentStatus()));
    }
}
