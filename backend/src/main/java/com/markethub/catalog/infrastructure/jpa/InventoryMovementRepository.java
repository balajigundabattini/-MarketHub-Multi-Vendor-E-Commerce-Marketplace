package com.markethub.catalog.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovementEntity, Long> { }
