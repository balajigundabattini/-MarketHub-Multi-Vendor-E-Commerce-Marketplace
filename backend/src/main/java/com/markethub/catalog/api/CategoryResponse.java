package com.markethub.catalog.api;

public record CategoryResponse(Long id, String name, String slug, String description, boolean active) { }
