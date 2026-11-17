package com.markethub.cart.infrastructure.jpa;

import java.time.Instant;

import com.markethub.catalog.infrastructure.jpa.ProductEntity;
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
@Table(name = "cart_items")
public class CartItemEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "cart_id", nullable = false) private CartEntity cart;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id", nullable = false) private ProductEntity product;
    @Column(nullable = false) private int quantity;
    @Column(name = "added_at", nullable = false, updatable = false) private Instant addedAt;
    protected CartItemEntity() { }
    public CartItemEntity(CartEntity cart, ProductEntity product, int quantity) { this.cart = cart; this.product = product; this.quantity = quantity; }
    @PrePersist void beforeInsert() { addedAt = Instant.now(); }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Long getId() { return id; }
    public CartEntity getCart() { return cart; }
    public ProductEntity getProduct() { return product; }
    public int getQuantity() { return quantity; }
}
