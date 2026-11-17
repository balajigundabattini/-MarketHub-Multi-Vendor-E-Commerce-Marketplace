package com.markethub.cart.api;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(List<Item> items, BigDecimal subtotal, String currency) {
    public record Item(Long id, Long productId, String name, int quantity, int availableQuantity,
            BigDecimal unitPrice, BigDecimal lineTotal, String currency, String imageUrl,
            String imageAlt, String sellerName) { }
}
