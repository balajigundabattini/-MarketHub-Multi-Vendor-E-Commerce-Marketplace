package com.markethub.catalog.infrastructure.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface InventoryRepository extends JpaRepository<InventoryEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryEntity i where i.productId = :productId")
    java.util.Optional<InventoryEntity> findByProductIdForUpdate(@Param("productId") Long productId);

    List<InventoryEntity> findAllByProductIdIn(List<Long> productIds);
}
