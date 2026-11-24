package com.markethub.payments.infrastructure.jpa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByIdAndOrderCustomerId(Long id, Long customerId);

    Optional<PaymentEntity> findByOrderId(Long orderId);

    List<PaymentEntity> findAllByCapturedAtGreaterThanEqualAndCapturedAtLessThan(
        Instant from, Instant to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentEntity p join fetch p.order where p.order.id=:orderId")
    Optional<PaymentEntity> findByOrderIdForUpdate(@Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentEntity p join fetch p.order where p.id=:id and p.order.customer.id=:userId")
    Optional<PaymentEntity> findOwnedForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentEntity p join fetch p.order where p.providerOrderId=:providerOrderId")
    Optional<PaymentEntity> findByProviderOrderIdForUpdate(@Param("providerOrderId") String providerOrderId);
}
