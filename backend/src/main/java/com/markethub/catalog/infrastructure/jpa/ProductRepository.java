package com.markethub.catalog.infrastructure.jpa;

import java.util.Optional;

import com.markethub.catalog.domain.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<ProductEntity, Long>, JpaSpecificationExecutor<ProductEntity> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductEntity p where p.id = :productId")
    Optional<ProductEntity> findByIdForUpdate(@Param("productId") Long productId);
    @EntityGraph(attributePaths = {"category", "store", "store.seller"})
    Page<ProductEntity> findAllByStoreId(Long storeId, Pageable pageable);
    @EntityGraph(attributePaths = {"category", "store", "store.seller", "images"})
    Optional<ProductEntity> findByIdAndStoreId(Long id, Long storeId);

    @EntityGraph(attributePaths = {"category", "store", "store.seller", "images"})
    Optional<ProductEntity> findByIdAndStatusAndStore_StatusAndStore_Seller_Status(
            Long id, ProductStatus status, String storeStatus, com.markethub.seller.domain.SellerStatus sellerStatus);

    @EntityGraph(attributePaths = {"category", "store", "store.seller", "images"})
    Optional<ProductEntity> findByIdAndStatusAndStore_StatusAndStore_Seller_StatusAndCategory_Active(
            Long id, ProductStatus status, String storeStatus, com.markethub.seller.domain.SellerStatus sellerStatus, boolean categoryActive);

    boolean existsByStoreIdAndSkuIgnoreCase(Long storeId, String sku);
    boolean existsByStoreIdAndSkuIgnoreCaseAndIdNot(Long storeId, String sku, Long id);
    long countByStore_Seller_User_IdAndStatus(Long userId, ProductStatus status);
    long countByStatus(ProductStatus status);
    java.util.List<ProductEntity> findAllByStore_Seller_User_Id(Long userId);

    @EntityGraph(attributePaths = {"category", "store", "store.seller"})
    @Query("select p from ProductEntity p join p.store s join s.seller seller join p.category c " +
            "where p.status = :status and s.status = :storeStatus and seller.status = :sellerStatus and c.active = true " +
            "and (:search is null or lower(p.name) like lower(concat('%', :search, '%')) or lower(coalesce(p.brand, '')) like lower(concat('%', :search, '%'))) " +
            "and (:categorySlug is null or c.slug = :categorySlug) and (:minPrice is null or p.price >= :minPrice) " +
            "and (:maxPrice is null or p.price <= :maxPrice)")
    Page<ProductEntity> searchCatalog(@Param("status") ProductStatus status, @Param("storeStatus") String storeStatus,
            @Param("sellerStatus") com.markethub.seller.domain.SellerStatus sellerStatus,
            @Param("search") String search, @Param("categorySlug") String categorySlug,
            @Param("minPrice") java.math.BigDecimal minPrice, @Param("maxPrice") java.math.BigDecimal maxPrice,
            Pageable pageable);

    @EntityGraph(attributePaths = {"category", "store", "store.seller", "images"})
    Page<ProductEntity> findAllByStatus(ProductStatus status, Pageable pageable);
}
