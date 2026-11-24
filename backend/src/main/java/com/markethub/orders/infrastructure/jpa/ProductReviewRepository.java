package com.markethub.orders.infrastructure.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductReviewRepository extends JpaRepository<ProductReviewEntity, Long> {
  boolean existsByItem_Id(Long itemId);

  Page<ProductReviewEntity> findAllByProduct_IdAndStatus(Long productId, String status, Pageable pageable);

  Page<ProductReviewEntity> findAllByStatus(String status, Pageable pageable);

  @Query("""
      SELECT r.product.id, AVG(r.rating), COUNT(r.id)
      FROM ProductReviewEntity r
      WHERE r.product.id IN :productIds
        AND r.status = 'APPROVED'
      GROUP BY r.product.id
      """)
  List<Object[]> getRatingStatistics(@Param("productIds") List<Long> productIds);

  Optional<ProductReviewEntity> findByIdAndProduct_IdAndCustomer_Id(
        Long id, Long productId, Long customerId);

  boolean existsByItem_IdAndIdNot(Long itemId, Long reviewId);
}
