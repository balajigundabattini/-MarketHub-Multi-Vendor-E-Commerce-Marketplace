package com.markethub.wishlist.api;

import jakarta.validation.constraints.NotNull;

public record WishlistItemRequest(@NotNull Long productId) { }
