package com.markethub.orders.infrastructure.jpa;

import java.time.Instant;
import com.markethub.catalog.infrastructure.jpa.ProductEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "inventory_reservations")
public class InventoryReservationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false, unique = true)
    private OrderItemEntity orderItem;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;
    @Column(nullable = false)
    private int quantity;
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryReservationEntity() {
    }

    public InventoryReservationEntity(OrderItemEntity item, ProductEntity product, int quantity, Instant expiresAt) {
        this.orderItem = item;
        this.product = product;
        this.quantity = quantity;
        this.expiresAt = expiresAt;
    }

    public void release() {
        status = "RELEASED";
    }

    public void reactivate(Instant newExpiresAt) {
        if (!"RELEASED".equals(status)) {
            throw new IllegalStateException(
                    "Only released reservations can be reactivated");
        }

        this.status = "ACTIVE";
        this.expiresAt = newExpiresAt;
    }

    public void commit() {
        status = "COMMITTED";
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

    public Long getId() {
        return id;
    }

    public OrderItemEntity getOrderItem() {
        return orderItem;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
