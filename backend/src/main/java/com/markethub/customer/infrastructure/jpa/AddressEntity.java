package com.markethub.customer.infrastructure.jpa;

import java.time.Instant;
import java.util.Locale;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "addresses")
public class AddressEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private UserEntity user;
    @Column(name = "recipient_name", nullable = false, length = 120) private String recipientName;
    @Column(nullable = false, length = 32) private String phone;
    @Column(nullable = false, length = 200) private String line1;
    @Column(length = 200) private String line2;
    @Column(nullable = false, length = 100) private String city;
    @Column(nullable = false, length = 100) private String region;
    @Column(name = "postal_code", nullable = false, length = 24) private String postalCode;
    @Column(name = "country_code", nullable = false, length = 2) private String countryCode;
    @Column(length = 40) private String label;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "is_active", nullable = false) private boolean isActive = true;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected AddressEntity() { }
    public AddressEntity(UserEntity user, String recipientName, String phone, String line1, String line2,
            String city, String region, String postalCode, String countryCode, String label, boolean isDefault) {
        this.user = user;
        update(recipientName, phone, line1, line2, city, region, postalCode, countryCode, label);
        this.isDefault = isDefault; this.isActive = true;
    }
    public void update(String recipientName, String phone, String line1, String line2, String city,
            String region, String postalCode, String countryCode, String label) {
        this.recipientName = recipientName.trim(); this.phone = phone.trim(); this.line1 = line1.trim();
        this.line2 = line2 == null || line2.isBlank() ? null : line2.trim(); this.city = city.trim();
        this.region = region.trim(); this.postalCode = postalCode.trim();
        this.countryCode = countryCode.trim().toUpperCase(Locale.ROOT);
        this.label = label == null || label.isBlank() ? null : label.trim();
    }
    public void setDefault(boolean value) { isDefault = value; }
    public void deactivate() { isActive = false; isDefault = false; }
    @PrePersist void beforeInsert() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void beforeUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getRecipientName() { return recipientName; }
    public String getPhone() { return phone; }
    public String getLine1() { return line1; }
    public String getLine2() { return line2; }
    public String getCity() { return city; }
    public String getRegion() { return region; }
    public String getPostalCode() { return postalCode; }
    public String getCountryCode() { return countryCode; }
    public String getLabel() { return label; }
    public boolean isDefault() { return isDefault; }
}
