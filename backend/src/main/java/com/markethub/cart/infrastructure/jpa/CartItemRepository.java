package com.markethub.cart.infrastructure.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {
    @EntityGraph(attributePaths = {"product", "product.images", "product.store", "product.store.seller"})
    List<CartItemEntity> findAllByCartId(Long cartId);
    Optional<CartItemEntity> findByIdAndCartId(Long id, Long cartId);
    Optional<CartItemEntity> findByCartIdAndProductId(Long cartId, Long productId);
    @Modifying
    @Query("delete from CartItemEntity i where i.cart.id = :cartId")
    int clearForCart(@Param("cartId") Long cartId);
}
