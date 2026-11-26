package com.markethub.orders.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface OrderItemRequestRepository extends JpaRepository<OrderItemRequestEntity, Long> {
        boolean existsByOrderItemIdAndRequestTypeAndStatus(Long orderItemId, String requestType, String status);

        java.util.List<OrderItemRequestEntity> findAllByOrderItem_SellerOrder_IdAndRequestTypeAndStatus(
                        Long sellerOrderId,
                        String requestType, String status);

        java.util.List<OrderItemRequestEntity> findAllByOrderItem_SellerOrder_IdAndRequestTypeAndStatusIn(
                        Long sellerOrderId, String requestType, java.util.List<String> statuses);

        java.util.Optional<OrderItemRequestEntity> findByIdAndOrderItem_SellerOrder_Seller_User_Id(Long id,
                        Long userId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                            SELECT r FROM OrderItemRequestEntity r
                            WHERE r.id = :id
                            AND r.orderItem.sellerOrder.seller.user.id = :userId
                        """)
        Optional<OrderItemRequestEntity> findByIdAndSellerUserIdForUpdate(
                        @Param("id") Long id,
                        @Param("userId") Long userId);

        List<OrderItemRequestEntity> findAllByRequestTypeAndStatusOrderByCreatedAtDesc(
                        String requestType,
                        String status);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                            SELECT r FROM OrderItemRequestEntity r
                            WHERE r.id = :id
                        """)
        Optional<OrderItemRequestEntity> findByIdForUpdate(@Param("id") Long id);
}
