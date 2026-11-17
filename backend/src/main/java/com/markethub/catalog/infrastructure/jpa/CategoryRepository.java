package com.markethub.catalog.infrastructure.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
    List<CategoryEntity> findAllByActiveTrueOrderByNameAsc();
    Optional<CategoryEntity> findBySlugAndActiveTrue(String slug);
    boolean existsBySlug(String slug);
}
