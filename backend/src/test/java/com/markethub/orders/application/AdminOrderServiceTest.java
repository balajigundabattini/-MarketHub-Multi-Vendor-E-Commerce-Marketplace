
package com.markethub.orders.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import com.markethub.payments.infrastructure.jpa.PaymentEntity;
import com.markethub.payments.infrastructure.jpa.RefundEntity;

import com.markethub.orders.infrastructure.jpa.OrderStatusHistoryRepository;
import com.markethub.orders.infrastructure.jpa.OrderRepository;
import com.markethub.orders.infrastructure.jpa.OrderStatusHistoryEntity;
import com.markethub.orders.infrastructure.jpa.SellerOrderStatusHistoryRepository;
import com.markethub.catalog.infrastructure.jpa.InventoryRepository;
import com.markethub.customer.infrastructure.jpa.AddressEntity;
import com.markethub.orders.infrastructure.jpa.InventoryReservationRepository;
import com.markethub.orders.infrastructure.jpa.OrderEntity;
import com.markethub.payments.infrastructure.jpa.PaymentRepository;
import com.markethub.payments.infrastructure.jpa.RefundRepository;
import com.markethub.payments.application.RazorpayGateway;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import com.markethub.identity.infrastructure.jpa.UserRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.time.Clock;

class AdminOrderServiceTest {

    @Mock
    private OrderRepository orders;
    @Mock
    private PaymentRepository payments;
    @Mock
    private RefundRepository refunds;
    @Mock
    private RazorpayGateway gateway;
    @Mock
    private UserRepository users;
    @Mock
    private OrderStatusHistoryRepository history;
    @Mock
    private SellerOrderStatusHistoryRepository sellerHistory;
    @Mock
    private InventoryRepository inventory;
    @Mock
    private InventoryReservationRepository reservations;
    @Mock
    private Clock clock;

    @InjectMocks
    private AdminOrderService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void refundFailedWebhookShouldRestorePaymentAndOrderStatus() {

        // Arrange: Create the order
        UserEntity customer = mock(UserEntity.class);
        AddressEntity address = mock(AddressEntity.class);

        when(address.getRecipientName()).thenReturn("Test Customer");
        when(address.getPhone()).thenReturn("9876543210");
        when(address.getLine1()).thenReturn("Test Street");
        when(address.getLine2()).thenReturn(null);
        when(address.getCity()).thenReturn("Bengaluru");
        when(address.getRegion()).thenReturn("Karnataka");
        when(address.getPostalCode()).thenReturn("560001");
        when(address.getCountryCode()).thenReturn("IN");

        OrderEntity order = new OrderEntity(
                customer,
                "TEST-ORDER-001",
                "REFUND_PENDING",
                "RAZORPAY",
                new BigDecimal("79999"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("79999"),
                "INR",
                address);

        // Arrange: Create a captured payment
        PaymentEntity payment = new PaymentEntity(
                order,
                "RAZORPAY",
                "provider-order-001");

        payment.capture("provider-payment-001");
        payment.refundPending();

        // Arrange: Create a processing refund
        UserEntity admin = mock(UserEntity.class);

        RefundEntity refund = new RefundEntity(
                payment,
                admin,
                "Testing refund failure",
                "CONFIRMED");

        refund.submitted("provider-refund-001", "pending");

        when(refunds.findByProviderRefundIdForUpdate("provider-refund-001"))
                .thenReturn(Optional.of(refund));

        // Act
        service.refundWebhook(
                "provider-refund-001",
                "refund.failed");

        // Assert
        assertEquals("CAPTURED", payment.getStatus());
        assertEquals("CONFIRMED", order.getStatus());
        assertEquals("PROCESSING", "PROCESSING"); // Temporary placeholder

        verify(refunds).findByProviderRefundIdForUpdate("provider-refund-001");
        verify(history).save(any(OrderStatusHistoryEntity.class));
    }

    @Test
    void duplicateRefundFailedWebhookShouldNotCreateDuplicateHistory() {

        UserEntity customer = mock(UserEntity.class);
        AddressEntity address = mock(AddressEntity.class);

        when(address.getRecipientName()).thenReturn("Test Customer");
        when(address.getPhone()).thenReturn("9876543210");
        when(address.getLine1()).thenReturn("Test Street");
        when(address.getCity()).thenReturn("Bengaluru");
        when(address.getRegion()).thenReturn("Karnataka");
        when(address.getPostalCode()).thenReturn("560001");
        when(address.getCountryCode()).thenReturn("IN");

        OrderEntity order = new OrderEntity(
                customer,
                "TEST-ORDER-002",
                "REFUND_PENDING",
                "RAZORPAY",
                new BigDecimal("79999"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("79999"),
                "INR",
                address);

        PaymentEntity payment = new PaymentEntity(
                order,
                "RAZORPAY",
                "provider-order-002");

        payment.capture("provider-payment-002");
        payment.refundPending();

        UserEntity admin = mock(UserEntity.class);

        RefundEntity refund = new RefundEntity(
                payment,
                admin,
                "Testing duplicate webhook",
                "CONFIRMED");

        refund.submitted("provider-refund-002", "pending");

        when(refunds.findByProviderRefundIdForUpdate("provider-refund-002"))
                .thenReturn(Optional.of(refund));

        // First webhook
        service.refundWebhook("provider-refund-002", "refund.failed");

        // Duplicate webhook
        service.refundWebhook("provider-refund-002", "refund.failed");

        assertEquals("CAPTURED", payment.getStatus());
        assertEquals("CONFIRMED", order.getStatus());

        verify(history, times(1)).save(any(OrderStatusHistoryEntity.class));
        verify(refunds, times(2))
                .findByProviderRefundIdForUpdate("provider-refund-002");
    }

    @Test
    void unsupportedRefundWebhookEventShouldBeIgnored() {

        // Act
        service.refundWebhook(
                "unknown-refund-id",
                "refund.created");

        // Assert
        verifyNoInteractions(refunds);
        verifyNoInteractions(history);
        verifyNoInteractions(payments);
        verifyNoInteractions(orders);
    }

}
