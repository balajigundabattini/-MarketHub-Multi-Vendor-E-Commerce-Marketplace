package com.markethub.catalog.infrastructure.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class CategoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, unique = true, length = 140) private String slug;
    @Column(length = 500) private String description;
    @Column(name = "is_active", nullable = false) private boolean active = true;

    protected CategoryEntity() { }
    public CategoryEntity(String name, String slug, String description) { this.name = name; this.slug = slug; this.description = description; }
    public void update(String name, String slug, String description) { this.name = name; this.slug = slug; this.description = description; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public void deactivate() { active = false; }
    public void setActive(boolean active) { this.active = active; }
}
