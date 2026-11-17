package com.markethub.orders.application;

import java.util.List;

import com.markethub.identity.infrastructure.jpa.UserRepository;
import com.markethub.orders.api.OrderDetailsResponse;
import com.markethub.orders.api.OrderSummaryResponse;
import com.markethub.orders.infrastructure.jpa.InventoryReservationRepository;
import com.markethub.orders.infrastructure.jpa.OrderEntity;
import com.markethub.orders.infrastructure.jpa.OrderItemEntity;
import com.markethub.orders.infrastructure.jpa.OrderItemRepository;
import com.markethub.orders.infrastructure.jpa.OrderItemRequestEntity;
import com.markethub.orders.infrastructure.jpa.OrderItemRequestRepository;
import com.markethub.orders.infrastructure.jpa.OrderRepository;
import com.markethub.orders.infrastructure.jpa.OrderStatusHistoryEntity;
import com.markethub.orders.infrastructure.jpa.OrderStatusHistoryRepository;
import com.markethub.orders.infrastructure.jpa.SellerOrderEntity;
import com.markethub.orders.infrastructure.jpa.SellerOrderStatusHistoryEntity;
import com.markethub.orders.infrastructure.jpa.SellerOrderStatusHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final OrderItemRepository items;
    private final OrderItemRequestRepository requests;
    private final OrderStatusHistoryRepository orderHistory;
    private final SellerOrderStatusHistoryRepository sellerOrderHistory;
    private final UserRepository users;

    public OrderService(OrderRepository orders, OrderItemRepository items, OrderItemRequestRepository requests,
            OrderStatusHistoryRepository orderHistory, SellerOrderStatusHistoryRepository sellerOrderHistory,
            UserRepository users) {
        this.orders = orders; this.items = items; this.requests = requests;
        this.orderHistory = orderHistory; this.sellerOrderHistory = sellerOrderHistory; this.users = users;
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> list(Long userId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page or page size is invalid");
        return orders.findAllByCustomerId(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(order -> new OrderSummaryResponse(order.getId(), order.getPublicOrderNumber(), order.getCreatedAt(),
                        order.getStatus(), order.getTotal(), order.getCurrency(), order.getSellerOrders().size()));
    }

    @Transactional(readOnly = true)
    public OrderDetailsResponse get(Long userId, Long orderId) {
        OrderEntity order = ownedOrder(userId, orderId);
        return details(order);
    }

    @Transactional
    public OrderDetailsResponse requestCancellation(Long userId, Long orderId, Long itemId, String reason) {
        OrderEntity order = ownedOrder(userId, orderId);
        if (!List.of("PENDING_PAYMENT", "CONFIRMED").contains(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order can no longer be cancelled");
        }
        OrderItemEntity item = ownedItem(userId, orderId, itemId);
        if (!"ACTIVE".equals(item.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "This item cannot be cancelled");
        if (requests.existsByOrderItemIdAndRequestTypeAndStatus(itemId, "CANCELLATION", "REQUESTED")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A cancellation request is already pending");
        }
        var customer = users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer account was not found"));
        item.setStatus("CANCEL_REQUESTED");
        requests.save(new OrderItemRequestEntity(item, customer, "CANCELLATION", reason.trim()));
        SellerOrderEntity sellerOrder = item.getSellerOrder();
        String previous = sellerOrder.getStatus();
        sellerOrder.setStatus("CANCELLATION_REQUESTED");
        sellerOrderHistory.save(new SellerOrderStatusHistoryEntity(sellerOrder, customer, previous, sellerOrder.getStatus(), reason.trim()));
        orderHistory.save(new OrderStatusHistoryEntity(order, customer, order.getStatus(), order.getStatus(), "Cancellation requested for item " + itemId));
        return details(ownedOrder(userId, orderId));
    }

    @Transactional
    public OrderDetailsResponse requestReturn(Long userId, Long orderId, Long itemId, String reason) {
        OrderEntity order = ownedOrder(userId, orderId);
        OrderItemEntity item = ownedItem(userId, orderId, itemId);
        if (!"DELIVERED".equals(item.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Returns are available after an item is delivered");
        if (requests.existsByOrderItemIdAndRequestTypeAndStatus(itemId, "RETURN", "REQUESTED")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A return request is already pending");
        }
        var customer = users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer account was not found"));
        item.setStatus("RETURN_REQUESTED");
        requests.save(new OrderItemRequestEntity(item, customer, "RETURN", reason.trim()));
        String previous = order.getStatus();
        order.setStatus("RETURN_REQUESTED");
        orderHistory.save(new OrderStatusHistoryEntity(order, customer, previous, order.getStatus(), "Return requested for item " + itemId));
        return details(ownedOrder(userId, orderId));
    }

    private OrderEntity ownedOrder(Long userId, Long orderId) {
        return orders.findByIdAndCustomerId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order was not found"));
    }
    private OrderItemEntity ownedItem(Long userId, Long orderId, Long itemId) {
        return items.findOwnedForUpdate(itemId, orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item was not found"));
    }

    private OrderDetailsResponse details(OrderEntity order) {
        var address = new OrderDetailsResponse.ShippingAddress(order.getShippingRecipientName(), order.getShippingPhone(),
                order.getShippingLine1(), order.getShippingLine2(), order.getShippingCity(), order.getShippingRegion(),
                order.getShippingPostalCode(), order.getShippingCountryCode());
        List<OrderDetailsResponse.SellerOrder> sellerOrderResponses = order.getSellerOrders().stream().map(sellerOrder -> {
            List<OrderDetailsResponse.Item> orderItems = sellerOrder.getItems().stream().map(item -> {
                boolean cancellable = "ACTIVE".equals(item.getStatus()) && List.of("PENDING_PAYMENT", "CONFIRMED").contains(order.getStatus());
                boolean returnable = "DELIVERED".equals(item.getStatus());
                return new OrderDetailsResponse.Item(item.getId(), item.getProduct().getId(), item.getNameSnapshot(),
                        item.getSkuSnapshot(), item.getQuantity(), item.getUnitPrice(), item.getStatus(), cancellable, returnable, false);
            }).toList();
            return new OrderDetailsResponse.SellerOrder(sellerOrder.getId(), sellerOrder.getStoreNameSnapshot(), sellerOrder.getStatus(), List.of(), orderItems);
        }).toList();
        return new OrderDetailsResponse(order.getId(), order.getPublicOrderNumber(), order.getCreatedAt(), order.getStatus(),
                order.getPaymentStatus(), null, order.getSubtotal(), order.getShipping(), order.getTax(), order.getDiscount(),
                order.getTotal(), order.getCurrency(), address, sellerOrderResponses);
    }
}
