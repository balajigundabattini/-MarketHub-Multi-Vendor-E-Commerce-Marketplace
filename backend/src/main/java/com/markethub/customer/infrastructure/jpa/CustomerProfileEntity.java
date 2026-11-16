package com.markethub.customer.infrastructure.jpa;

import java.time.Instant;
import java.time.LocalDate;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_profiles")
public class CustomerProfileEntity {
    @Id @Column(name = "user_id") private Long userId;
    @MapsId @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) private UserEntity user;
    @Column(length = 32) private String phone;
    @Column(name = "birth_date") private LocalDate birthDate;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected CustomerProfileEntity() { }
    public CustomerProfileEntity(UserEntity user) { this.user = user; }
    public void update(String phone, LocalDate birthDate) { this.phone = phone; this.birthDate = birthDate; }
    @PrePersist void beforeInsert() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void beforeUpdate() { updatedAt = Instant.now(); }
    public UserEntity getUser() { return user; }
    public String getPhone() { return phone; }
    public LocalDate getBirthDate() { return birthDate; }
}
