package com.markethub.orders.infrastructure.jpa;

import java.time.Instant;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "order_item_requests")
public class OrderItemRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItemEntity orderItem;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private UserEntity customer;
    @Column(name = "request_type", nullable = false, length = 20)
    private String requestType;
    @Column(nullable = false, length = 500)
    private String reason;
    @Column(nullable = false, length = 20)
    private String status = "REQUESTED";
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private UserEntity reviewer;
    @Column(name = "review_note", length = 500)
    private String reviewNote;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderItemRequestEntity() {
    }

    public OrderItemRequestEntity(OrderItemEntity orderItem, UserEntity customer, String requestType, String reason) {
        this.orderItem = orderItem;
        this.customer = customer;
        this.requestType = requestType;
        this.reason = reason;
    }

    public void review(UserEntity reviewer, String status, String note) {
        this.reviewer = reviewer;
        this.status = status;
        this.reviewNote = note;
    }

    public Long getId() {
        return id;
    }

    public OrderItemEntity getOrderItem() {
        return orderItem;
    }

    public String getRequestType() {
        return requestType;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UserEntity getCustomer() {
        return customer;
    }

    @PrePersist
    void beforeInsert() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void beforeUpdate() {
        updatedAt = Instant.now();
    }
}
