package com.markethub.wishlist.infrastructure.jpa;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class WishlistItemId implements Serializable {
    @Column(name = "wishlist_id") private Long wishlistId;
    @Column(name = "product_id") private Long productId;
    protected WishlistItemId() { }
    public WishlistItemId(Long wishlistId, Long productId) { this.wishlistId = wishlistId; this.productId = productId; }
    @Override public boolean equals(Object value) { if (this == value) return true; if (!(value instanceof WishlistItemId other)) return false; return Objects.equals(wishlistId, other.wishlistId) && Objects.equals(productId, other.productId); }
    @Override public int hashCode() { return Objects.hash(wishlistId, productId); }
}
