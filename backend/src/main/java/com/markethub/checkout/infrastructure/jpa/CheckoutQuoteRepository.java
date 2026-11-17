package com.markethub.checkout.infrastructure.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface CheckoutQuoteRepository extends JpaRepository<CheckoutQuoteEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from CheckoutQuoteEntity q left join fetch q.coupon where q.id = :id")
    Optional<CheckoutQuoteEntity> findByIdForUpdate(@Param("id") String id);
}
