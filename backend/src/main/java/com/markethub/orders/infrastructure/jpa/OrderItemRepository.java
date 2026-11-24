package com.markethub.orders.infrastructure.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from OrderItemEntity i join fetch i.sellerOrder s join fetch s.order o where i.id=:itemId and i.product.id=:productId and o.customer.id=:customerId")
    Optional<OrderItemEntity> findOwnedProductItemForUpdate(@Param("itemId") Long itemId, @Param("productId") Long productId, @Param("customerId") Long customerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from OrderItemEntity i join fetch i.sellerOrder s join fetch s.order o where i.id = :itemId and o.id = :orderId and o.customer.id = :customerId")
    Optional<OrderItemEntity> findOwnedForUpdate(@Param("itemId") Long itemId, @Param("orderId") Long orderId, @Param("customerId") Long customerId);
}
