package com.markethub.wishlist.api;

import java.math.BigDecimal;
import java.util.List;

public record WishlistResponse(List<Item> items) {
    public record Item(Long productId, String name, BigDecimal price, String currency,
            String imageUrl, String imageAlt, boolean available) { }
}
