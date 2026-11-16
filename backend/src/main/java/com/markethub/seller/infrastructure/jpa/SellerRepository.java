package com.markethub.seller.infrastructure.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SellerRepository extends JpaRepository<SellerEntity, Long> {
    @EntityGraph(attributePaths = "user")
    Page<SellerEntity> findAllByStatus(com.markethub.seller.domain.SellerStatus status, Pageable pageable);
    @EntityGraph(attributePaths = "user")
    Page<SellerEntity> findAll(Pageable pageable);
    Optional<SellerEntity> findByUserId(Long userId);
    boolean existsByUserId(Long userId);

    @Query("select s from SellerEntity s join fetch s.user where s.id = :id")
    Optional<SellerEntity> findByIdWithUser(@Param("id") Long id);
}
