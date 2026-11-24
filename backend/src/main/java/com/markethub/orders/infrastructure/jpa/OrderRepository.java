package com.markethub.orders.infrastructure.jpa;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Page<OrderEntity> findAllByCustomerId(Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "sellerOrders",
            "sellerOrders.seller"
    })
    Optional<OrderEntity> findByIdAndCustomerId(Long id, Long customerId);

    @EntityGraph(attributePaths = { "customer" })
    @org.springframework.data.jpa.repository.Query("select o from OrderEntity o where (:status is null or o.status = :status)")
    Page<OrderEntity> findAdminPage(@org.springframework.data.repository.query.Param("status") String status,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from OrderEntity o where o.id=:id")
    Optional<OrderEntity> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    java.util.List<OrderEntity> findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThan(java.time.Instant from,
            java.time.Instant to);
}
