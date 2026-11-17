package com.markethub.wishlist.infrastructure.jpa;

import java.time.Instant;

import com.markethub.identity.infrastructure.jpa.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "wishlists")
public class WishlistEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false, unique = true) private UserEntity user;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected WishlistEntity() { }
    public WishlistEntity(UserEntity user) { this.user = user; }
    @PrePersist void beforeInsert() { createdAt = Instant.now(); }
    public Long getId() { return id; }
}
