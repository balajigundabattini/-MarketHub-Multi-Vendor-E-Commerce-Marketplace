
package com.markethub.payments.infrastructure.jpa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface RefundRepository extends JpaRepository<RefundEntity, Long> {

    boolean existsByPaymentIdAndStatusIn(Long paymentId, List<String> statuses);

    // Used by the existing full-refund workflow.
    boolean existsByPaymentId(Long paymentId);

    // Checks whether a particular order item already has a refund.
    boolean existsByOrderItemIdAndStatusIn(Long orderItemId, List<String> statuses);

    // Retrieves refunds belonging to a payment.
    List<RefundEntity> findAllByPaymentId(Long paymentId);

    Optional<RefundEntity> findByProviderRefundId(String providerRefundId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RefundEntity r WHERE r.providerRefundId = :providerRefundId")
    Optional<RefundEntity> findByProviderRefundIdForUpdate(
            @Param("providerRefundId") String providerRefundId);

    List<RefundEntity> findAllByStatusAndProcessedAtGreaterThanEqualAndProcessedAtLessThan(
            String status, Instant from, Instant to);
}
