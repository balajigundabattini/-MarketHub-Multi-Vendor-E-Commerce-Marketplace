package com.markethub.orders.infrastructure.jpa;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ShipmentRepository extends JpaRepository<ShipmentEntity, Long> { List<ShipmentEntity> findAllBySellerOrderId(Long sellerOrderId); }
