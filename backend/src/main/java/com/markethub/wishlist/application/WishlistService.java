package com.markethub.wishlist.application;

import java.util.Comparator;

import com.markethub.catalog.domain.ProductStatus;
import com.markethub.catalog.infrastructure.jpa.ProductRepository;
import com.markethub.identity.infrastructure.jpa.UserRepository;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import com.markethub.seller.domain.SellerStatus;
import com.markethub.wishlist.api.WishlistResponse;
import com.markethub.wishlist.infrastructure.jpa.WishlistEntity;
import com.markethub.wishlist.infrastructure.jpa.WishlistItemEntity;
import com.markethub.wishlist.infrastructure.jpa.WishlistItemId;
import com.markethub.wishlist.infrastructure.jpa.WishlistItemRepository;
import com.markethub.wishlist.infrastructure.jpa.WishlistRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WishlistService {
    private final WishlistRepository wishlists;
    private final WishlistItemRepository items;
    private final ProductRepository products;
    private final UserRepository users;

    public WishlistService(WishlistRepository wishlists, WishlistItemRepository items,
            ProductRepository products, UserRepository users) {
        this.wishlists = wishlists; this.items = items; this.products = products; this.users = users;
    }

    @Transactional
    public WishlistResponse get(Long userId) { return response(wishlistFor(userId)); }

    @Transactional
    public WishlistResponse add(Long userId, Long productId) {
        WishlistEntity wishlist = wishlistFor(userId);
        var product = products.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product was not found"));
        WishlistItemId key = new WishlistItemId(wishlist.getId(), productId);
        if (!items.existsById(key)) items.save(new WishlistItemEntity(wishlist, product));
        return response(wishlist);
    }

    @Transactional
    public void remove(Long userId, Long productId) {
        WishlistEntity wishlist = wishlistFor(userId);
        items.deleteById(new WishlistItemId(wishlist.getId(), productId));
    }

    private WishlistEntity wishlistFor(Long userId) {
        var current = wishlists.findByUserId(userId);
        if (current.isPresent()) return current.get();
        UserEntity user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer account was not found"));
        return wishlists.findByUserId(userId).orElseGet(() -> wishlists.save(new WishlistEntity(user)));
    }

    private WishlistResponse response(WishlistEntity wishlist) {
        var result = items.findAllByWishlist_Id(wishlist.getId()).stream().map(item -> {
            var product = item.getProduct();
            var image = product.getImages().stream().min(Comparator.comparingInt(row -> row.getSortOrder())).orElse(null);
            boolean available = product.getStatus() == ProductStatus.ACTIVE
                    && "ACTIVE".equals(product.getStore().getStatus())
                    && product.getStore().getSeller().getStatus() == SellerStatus.APPROVED
                    && product.getCategory().isActive();
            return new WishlistResponse.Item(product.getId(), product.getName(), product.getPrice(), product.getCurrency(),
                    image == null ? null : image.getStorageKey(), image == null ? null : image.getAltText(), available);
        }).toList();
        return new WishlistResponse(result);
    }
}
