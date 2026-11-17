package com.markethub.orders.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerOrderRepository extends JpaRepository<SellerOrderEntity, Long> { }
