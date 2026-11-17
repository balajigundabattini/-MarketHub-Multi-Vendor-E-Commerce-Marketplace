package com.markethub.orders.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRequestRepository extends JpaRepository<OrderItemRequestEntity, Long> {
    boolean existsByOrderItemIdAndRequestTypeAndStatus(Long orderItemId, String requestType, String status);
    java.util.List<OrderItemRequestEntity> findAllByOrderItem_SellerOrder_IdAndRequestTypeAndStatus(Long sellerOrderId, String requestType, String status);
    java.util.Optional<OrderItemRequestEntity> findByIdAndOrderItem_SellerOrder_Seller_User_Id(Long id, Long userId);
}
