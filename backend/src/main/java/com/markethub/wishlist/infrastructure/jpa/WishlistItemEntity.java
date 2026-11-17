package com.markethub.wishlist.infrastructure.jpa;

import java.time.Instant;

import com.markethub.catalog.infrastructure.jpa.ProductEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "wishlist_items")
public class WishlistItemEntity {
    @EmbeddedId private WishlistItemId id;
    @MapsId("wishlistId") @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "wishlist_id", nullable = false) private WishlistEntity wishlist;
    @MapsId("productId") @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id", nullable = false) private ProductEntity product;
    @Column(name = "added_at", nullable = false, insertable = false, updatable = false) private Instant addedAt;
    protected WishlistItemEntity() { }
    public WishlistItemEntity(WishlistEntity wishlist, ProductEntity product) { this.wishlist = wishlist; this.product = product; this.id = new WishlistItemId(wishlist.getId(), product.getId()); }
    public ProductEntity getProduct() { return product; }
    public WishlistEntity getWishlist() { return wishlist; }
    public WishlistItemId getId() { return id; }
}
