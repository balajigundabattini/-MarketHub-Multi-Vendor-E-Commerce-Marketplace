package com.markethub.catalog.infrastructure.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "inventory")
public class InventoryEntity {
    @Id @Column(name = "product_id") private Long productId;
    @Column(name = "on_hand", nullable = false) private int onHand;
    @Column(nullable = false) private int reserved;
    @Version @Column(nullable = false) private Long version;
    protected InventoryEntity() { }
    public InventoryEntity(Long productId) { this.productId = productId; }
    public void setOnHand(int onHand) { if (onHand < reserved) throw new IllegalArgumentException("On-hand stock cannot be less than reserved stock"); this.onHand = onHand; }
    public void reserve(int quantity) { if (quantity < 1 || getAvailable() < quantity) throw new IllegalStateException("Insufficient available stock"); reserved += quantity; }
    public void release(int quantity) { if (quantity < 1 || reserved < quantity) throw new IllegalStateException("Invalid stock reservation release"); reserved -= quantity; }
    public void commitReservation(int quantity) { release(quantity); if (onHand < quantity) throw new IllegalStateException("Reserved stock exceeds on-hand quantity"); onHand -= quantity; }
    public Long getProductId() { return productId; }
    public int getOnHand() { return onHand; }
    public int getReserved() { return reserved; }
    public int getAvailable() { return onHand - reserved; }
}
