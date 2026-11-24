
package com.markethub.orders.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.markethub.catalog.infrastructure.jpa.InventoryEntity;
import com.markethub.catalog.infrastructure.jpa.InventoryRepository;
import com.markethub.catalog.infrastructure.jpa.ProductEntity;
import com.markethub.orders.infrastructure.jpa.*;
import com.markethub.payments.infrastructure.jpa.PaymentEntity;
import com.markethub.payments.infrastructure.jpa.PaymentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationExpiryServiceTest {

    private final AtomicReference<String> itemStatus =
            new AtomicReference<>("ACTIVE");

    private final AtomicReference<String> sellerStatus =
            new AtomicReference<>("CONFIRMED");

    private final AtomicReference<String> orderStatus =
            new AtomicReference<>("PENDING_PAYMENT");

    @Mock private InventoryReservationRepository reservations;
    @Mock private InventoryRepository inventory;
    @Mock private PaymentRepository payments;
    @Mock private OrderStatusHistoryRepository orderHistory;
    @Mock private SellerOrderStatusHistoryRepository sellerHistory;
    @Mock private Clock clock;

    @Mock private InventoryReservationEntity reservation;
    @Mock private OrderItemEntity item;
    @Mock private SellerOrderEntity seller;
    @Mock private OrderEntity order;
    @Mock private PaymentEntity payment;
    @Mock private ProductEntity product;
    @Mock private InventoryEntity stock;

    @InjectMocks
    private ReservationExpiryService service;

    private final Instant now =
            Instant.parse("2026-11-24T05:00:00Z");

    @BeforeEach
    void setUp() {

        when(clock.instant()).thenReturn(now);

        when(reservations.findAllByStatusAndExpiresAtBefore("ACTIVE", now))
                .thenReturn(List.of(reservation));

        when(reservation.getOrderItem()).thenReturn(item);
        when(item.getSellerOrder()).thenReturn(seller);
        when(seller.getOrder()).thenReturn(order);

        when(order.getId()).thenReturn(100L);
        when(order.getStatus()).thenAnswer(inv -> orderStatus.get());

        when(payments.findByOrderIdForUpdate(100L))
                .thenReturn(Optional.of(payment));

        when(payment.getStatus()).thenReturn("PENDING");

        when(item.getId()).thenReturn(200L);
        when(item.getStatus()).thenAnswer(inv -> itemStatus.get());

        when(reservations.findByOrderItemId(200L))
                .thenReturn(Optional.of(reservation));

        when(reservation.getStatus()).thenReturn("ACTIVE");
        when(reservation.getExpiresAt()).thenReturn(now.minusSeconds(60));
        when(reservation.getProduct()).thenReturn(product);
        when(product.getId()).thenReturn(50L);
        when(reservation.getQuantity()).thenReturn(2);

        when(inventory.findByProductIdForUpdate(50L))
                .thenReturn(Optional.of(stock));

        when(seller.getItems()).thenReturn(List.of(item));
        when(seller.getStatus()).thenAnswer(inv -> sellerStatus.get());

        when(order.getSellerOrders()).thenReturn(List.of(seller));

        // Simulate item status changes
        doAnswer(inv -> {
            itemStatus.set(inv.getArgument(0));
            return null;
        }).when(item).setStatus(anyString());

        // Simulate seller order status changes
        doAnswer(inv -> {
            sellerStatus.set(inv.getArgument(0));
            return null;
        }).when(seller).setStatus(anyString());

        // Simulate parent order status changes
        doAnswer(inv -> {
            orderStatus.set(inv.getArgument(0));
            return null;
        }).when(order).setStatus(anyString());
    }

    @Test
    void expiredPendingPaymentShouldReleaseReservationAndExpireOrder() {

        service.expirePendingReservations();

        // Verify inventory and reservation
        verify(stock).release(2);
        verify(reservation).release();

        // Verify status changes
        assertEquals("CANCELLED", itemStatus.get());
        assertEquals("CANCELLED", sellerStatus.get());
        assertEquals("PAYMENT_EXPIRED", orderStatus.get());

        // Verify payment expiry
        verify(payment).expire();

        // Verify status history
        verify(orderHistory).save(any(OrderStatusHistoryEntity.class));
        verify(sellerHistory).save(any(SellerOrderStatusHistoryEntity.class));
    }
}
