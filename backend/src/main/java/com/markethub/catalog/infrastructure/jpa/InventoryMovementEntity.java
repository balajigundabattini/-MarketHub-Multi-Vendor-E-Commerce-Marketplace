package com.markethub.catalog.infrastructure.jpa;

import java.time.Instant;

import com.markethub.identity.infrastructure.jpa.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovementEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id", nullable = false) private ProductEntity product;
    @Column(name = "quantity_delta", nullable = false) private int quantityDelta;
    @Column(nullable = false, length = 40) private String reason;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "actor_user_id") private UserEntity actor;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected InventoryMovementEntity() { }
    public InventoryMovementEntity(ProductEntity product, int quantityDelta, String reason, UserEntity actor) { this.product = product; this.quantityDelta = quantityDelta; this.reason = reason; this.actor = actor; }
    @PrePersist void beforeInsert() { createdAt = Instant.now(); }
}
