package com.markethub.seller.infrastructure.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<StoreEntity, Long> {
    Optional<StoreEntity> findBySellerId(Long sellerId);
    boolean existsBySlug(String slug);
}
