package com.markethub.orders.infrastructure.jpa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservationEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InventoryReservationEntity> findByOrderItemId(Long orderItemId);
    List<InventoryReservationEntity> findAllByStatusAndExpiresAtBefore(String status, java.time.Instant expiresAt);
}
