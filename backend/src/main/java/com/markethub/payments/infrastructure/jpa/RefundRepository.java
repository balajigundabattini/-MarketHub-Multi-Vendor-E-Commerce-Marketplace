package com.markethub.payments.infrastructure.jpa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RefundRepository extends JpaRepository<RefundEntity,Long> {
    boolean existsByPaymentIdAndStatusIn(Long paymentId,List<String> statuses);
    boolean existsByPaymentId(Long paymentId);
    Optional<RefundEntity> findByProviderRefundId(String providerRefundId);
    List<RefundEntity> findAllByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(String status,java.time.Instant from,java.time.Instant to);
}
