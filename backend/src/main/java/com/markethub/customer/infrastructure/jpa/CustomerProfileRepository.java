package com.markethub.customer.infrastructure.jpa;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CustomerProfileRepository extends JpaRepository<CustomerProfileEntity, Long> { }
