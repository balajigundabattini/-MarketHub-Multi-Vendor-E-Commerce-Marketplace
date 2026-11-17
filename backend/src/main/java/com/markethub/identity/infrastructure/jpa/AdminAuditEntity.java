package com.markethub.identity.infrastructure.jpa;
import java.time.Instant;
import jakarta.persistence.*;
@Entity @Table(name="admin_audit_log")
public class AdminAuditEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="admin_user_id",nullable=false) private UserEntity admin;
    @Column(name="entity_type",nullable=false,length=48) private String entityType;
    @Column(name="entity_id",nullable=false) private Long entityId;
    @Column(nullable=false,length=48) private String action;
    @Column(length=500) private String detail;
    @Column(name="created_at",nullable=false,insertable=false,updatable=false) private Instant createdAt;
    protected AdminAuditEntity(){}
    public AdminAuditEntity(UserEntity admin,String entityType,Long entityId,String action,String detail){this.admin=admin;this.entityType=entityType;this.entityId=entityId;this.action=action;this.detail=detail;}
}
