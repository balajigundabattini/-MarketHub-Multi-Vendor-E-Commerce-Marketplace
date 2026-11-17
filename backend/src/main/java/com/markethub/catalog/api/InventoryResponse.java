package com.markethub.catalog.api;

public record InventoryResponse(Long productId, String name, String sku, int onHand, int reserved, int available) { }
