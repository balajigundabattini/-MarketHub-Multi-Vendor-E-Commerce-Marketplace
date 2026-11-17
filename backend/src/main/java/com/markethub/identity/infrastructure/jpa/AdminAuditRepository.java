package com.markethub.identity.infrastructure.jpa;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AdminAuditRepository extends JpaRepository<AdminAuditEntity,Long>{}
