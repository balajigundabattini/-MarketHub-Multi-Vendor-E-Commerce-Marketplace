package com.markethub.payments.infrastructure.jpa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import jakarta.persistence.LockModeType;

public interface RefundRepository extends JpaRepository<RefundEntity, Long> {
    boolean existsByPaymentIdAndStatusIn(Long paymentId, List<String> statuses);

    boolean existsByPaymentId(Long paymentId);

    Optional<RefundEntity> findByProviderRefundId(String providerRefundId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RefundEntity r WHERE r.providerRefundId = :providerRefundId")
    Optional<RefundEntity> findByProviderRefundIdForUpdate(
            @Param("providerRefundId") String providerRefundId);

    List<RefundEntity> findAllByStatusAndProcessedAtGreaterThanEqualAndProcessedAtLessThan(
            String status, java.time.Instant from, java.time.Instant to);
}