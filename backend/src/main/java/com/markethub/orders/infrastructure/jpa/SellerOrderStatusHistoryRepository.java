package com.markethub.orders.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerOrderStatusHistoryRepository extends JpaRepository<SellerOrderStatusHistoryEntity, Long> { }
