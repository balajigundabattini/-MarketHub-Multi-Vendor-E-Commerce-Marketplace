package com.markethub.wishlist.infrastructure.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemRepository extends JpaRepository<WishlistItemEntity, WishlistItemId> {
    @EntityGraph(attributePaths = {"product", "product.images", "product.store", "product.store.seller"})
    List<WishlistItemEntity> findAllByWishlist_Id(Long wishlistId);
    void deleteById_WishlistIdAndId_ProductId(Long wishlistId, Long productId);
}
