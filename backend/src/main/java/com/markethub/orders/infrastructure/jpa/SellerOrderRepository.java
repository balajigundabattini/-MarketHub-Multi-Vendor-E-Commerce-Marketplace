package com.markethub.orders.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerOrderRepository extends JpaRepository<SellerOrderEntity, Long> {
    java.util.List<SellerOrderEntity> findAllBySeller_User_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Long userId, java.time.Instant from, java.time.Instant to);
    long countBySeller_User_IdAndStatusIn(Long userId, java.util.List<String> statuses);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"order", "items"})
    org.springframework.data.domain.Page<SellerOrderEntity> findAllBySeller_User_Id(Long userId, org.springframework.data.domain.Pageable pageable);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select so from SellerOrderEntity so where so.id = :id and so.seller.user.id = :userId")
    java.util.Optional<SellerOrderEntity> findOwnedForUpdate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("userId") Long userId);
}
