
package com.markethub.payments.infrastructure.jpa;

import java.math.BigDecimal;
import java.time.Instant;

import com.markethub.identity.infrastructure.jpa.UserEntity;
import com.markethub.orders.infrastructure.jpa.OrderEntity;
import com.markethub.orders.infrastructure.jpa.OrderItemEntity;

import jakarta.persistence.*;

@Entity
@Table(name = "refunds")
public class RefundEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private PaymentEntity payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private UserEntity requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItemEntity orderItem;

    @Column(name = "provider_refund_id", length = 100, unique = true)
    private String providerRefundId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(name = "order_status_before", nullable = false, length = 32)
    private String orderStatusBefore;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false,
            insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected RefundEntity() {
    }

    // Existing full-order refund constructor
    public RefundEntity(
            PaymentEntity payment,
            UserEntity admin,
            String reason,
            String orderStatus) {

        this.payment = payment;
        this.order = payment.getOrder();
        this.requestedBy = admin;
        this.reason = reason;
        this.status = "REQUESTED";
        this.orderStatusBefore = orderStatus;
        this.amount = payment.getAmount();
        this.currency = payment.getCurrency();
    }

    // New individual-product partial refund constructor
    public RefundEntity(
            PaymentEntity payment,
            UserEntity admin,
            OrderItemEntity orderItem,
            String reason,
            String orderStatus,
            BigDecimal amount) {

        this.payment = payment;
        this.order = payment.getOrder();
        this.requestedBy = admin;
        this.orderItem = orderItem;
        this.reason = reason;
        this.status = "REQUESTED";
        this.orderStatusBefore = orderStatus;
        this.amount = amount;
        this.currency = payment.getCurrency();
    }

    public void submitted(String providerId, String providerStatus) {
        this.providerRefundId = providerId;

        if ("processed".equalsIgnoreCase(providerStatus)) {
            processed();
        } else if ("failed".equalsIgnoreCase(providerStatus)) {
            failed();
        } else {
            this.status = "PROCESSING";
        }
    }

    public void processed() {
        this.status = "PROCESSED";

        if (this.processedAt == null) {
            this.processedAt = Instant.now();
        }
    }

    public void failed() {
        this.status = "FAILED";
    }

    public Long getId() {
        return id;
    }

    public PaymentEntity getPayment() {
        return payment;
    }

    public OrderEntity getOrder() {
        return order;
    }

    public UserEntity getRequestedBy() {
        return requestedBy;
    }

    public OrderItemEntity getOrderItem() {
        return orderItem;
    }

    public String getProviderRefundId() {
        return providerRefundId;
    }

    public String getStatus() {
        return status;
    }

    public String getOrderStatusBefore() {
        return orderStatusBefore;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
