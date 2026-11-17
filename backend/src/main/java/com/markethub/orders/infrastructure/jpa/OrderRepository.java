package com.markethub.orders.infrastructure.jpa;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Page<OrderEntity> findAllByCustomerId(Long customerId, Pageable pageable);
    @EntityGraph(attributePaths = {"sellerOrders", "sellerOrders.seller", "sellerOrders.items", "sellerOrders.items.product", "sellerOrders.items.product.store"})
    Optional<OrderEntity> findByIdAndCustomerId(Long id, Long customerId);
}
