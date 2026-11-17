package com.markethub.catalog.api;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank @Size(max = 80) String sku,
        @NotBlank @Size(max = 200) String name,
        @NotNull Long categoryId,
        @Size(max = 120) String brand,
        @NotBlank @Size(max = 20000) String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        @Size(max = 10) List<@NotBlank @Size(max = 500) @Pattern(regexp = "https?://[^\\s]+") String> imageUrls) { }
