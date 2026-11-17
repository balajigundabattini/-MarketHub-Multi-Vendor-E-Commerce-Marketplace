package com.markethub.orders.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRequestRepository extends JpaRepository<OrderItemRequestEntity, Long> {
    boolean existsByOrderItemIdAndRequestTypeAndStatus(Long orderItemId, String requestType, String status);
}
