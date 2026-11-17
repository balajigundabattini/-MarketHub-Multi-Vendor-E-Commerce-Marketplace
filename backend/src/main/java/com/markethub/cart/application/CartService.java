package com.markethub.cart.application;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import com.markethub.cart.api.CartResponse;
import com.markethub.cart.infrastructure.jpa.CartEntity;
import com.markethub.cart.infrastructure.jpa.CartItemEntity;
import com.markethub.cart.infrastructure.jpa.CartItemRepository;
import com.markethub.cart.infrastructure.jpa.CartRepository;
import com.markethub.catalog.domain.ProductStatus;
import com.markethub.catalog.infrastructure.jpa.InventoryEntity;
import com.markethub.catalog.infrastructure.jpa.InventoryRepository;
import com.markethub.catalog.infrastructure.jpa.ProductEntity;
import com.markethub.catalog.infrastructure.jpa.ProductRepository;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import com.markethub.identity.infrastructure.jpa.UserRepository;
import com.markethub.seller.domain.SellerStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CartService {
    private static final String ACTIVE = "ACTIVE";
    private final CartRepository carts;
    private final CartItemRepository items;
    private final ProductRepository products;
    private final InventoryRepository inventory;
    private final UserRepository users;

    public CartService(CartRepository carts, CartItemRepository items, ProductRepository products,
            InventoryRepository inventory, UserRepository users) {
        this.carts = carts; this.items = items; this.products = products; this.inventory = inventory; this.users = users;
    }

    @Transactional
    public CartResponse getCart(Long userId) { return response(cartFor(userId)); }

    @Transactional
    public CartResponse add(Long userId, Long productId, int quantity) {
        CartEntity cart = cartFor(userId);
        ProductEntity product = sellableProduct(productId);
        InventoryEntity stock = inventory.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "This product is not available for purchase"));
        var existing = items.findByCartIdAndProductId(cart.getId(), productId);
        int updatedQuantity = existing.map(item -> item.getQuantity() + quantity).orElse(quantity);
        if (updatedQuantity > 99) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart quantity cannot exceed 99");
        if (updatedQuantity > stock.getAvailable()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Requested quantity is greater than available stock");
        ensureCurrency(cart, product.getCurrency());
        if (existing.isPresent()) existing.get().setQuantity(updatedQuantity);
        else items.save(new CartItemEntity(cart, product, quantity));
        return response(cart);
    }

    @Transactional
    public CartResponse update(Long userId, Long itemId, int quantity) {
        CartEntity cart = cartFor(userId);
        CartItemEntity item = items.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item was not found"));
        ProductEntity product = sellableProduct(item.getProduct().getId());
        InventoryEntity stock = inventory.findByProductIdForUpdate(product.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "This product is not available for purchase"));
        if (quantity > stock.getAvailable()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Requested quantity is greater than available stock");
        ensureCurrency(cart, product.getCurrency());
        item.setQuantity(quantity);
        return response(cart);
    }

    @Transactional
    public CartResponse remove(Long userId, Long itemId) {
        CartEntity cart = cartFor(userId);
        CartItemEntity item = items.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item was not found"));
        items.delete(item);
        return response(cart);
    }

    private CartEntity cartFor(Long userId) {
        var current = carts.findByUserIdAndStatus(userId, ACTIVE);
        if (current.isPresent()) return current.get();
        UserEntity user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer account was not found"));
        return carts.findByUserIdAndStatus(userId, ACTIVE).orElseGet(() -> carts.save(new CartEntity(user)));
    }

    private ProductEntity sellableProduct(Long productId) {
        return products.findByIdAndStatusAndStore_StatusAndStore_Seller_StatusAndCategory_Active(
                        productId, ProductStatus.ACTIVE, ACTIVE, SellerStatus.APPROVED, true)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product is not currently available"));
    }

    private void ensureCurrency(CartEntity cart, String currency) {
        for (CartItemEntity item : items.findAllByCartId(cart.getId())) {
            if (!currency.equals(item.getProduct().getCurrency())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A cart can contain products in one currency only");
            }
        }
    }

    private CartResponse response(CartEntity cart) {
        List<CartItemEntity> lines = items.findAllByCartId(cart.getId());
        var productIds = lines.stream().map(item -> item.getProduct().getId()).toList();
        var stockByProduct = productIds.isEmpty() ? java.util.Map.<Long, InventoryEntity>of() :
                inventory.findAllByProductIdIn(productIds).stream().collect(java.util.stream.Collectors.toMap(InventoryEntity::getProductId, row -> row));
        String currency = lines.isEmpty() ? "INR" : lines.getFirst().getProduct().getCurrency();
        BigDecimal subtotal = BigDecimal.ZERO;
        var responseItems = new java.util.ArrayList<CartResponse.Item>();
        for (CartItemEntity line : lines) {
            ProductEntity product = line.getProduct();
            if (!currency.equals(product.getCurrency())) throw new ResponseStatusException(HttpStatus.CONFLICT, "A cart can contain products in one currency only");
            InventoryEntity stock = stockByProduct.get(product.getId());
            int available = publiclySellable(product) && stock != null ? stock.getAvailable() : 0;
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(line.getQuantity()));
            subtotal = subtotal.add(lineTotal);
            var image = product.getImages().stream().min(Comparator.comparingInt(row -> row.getSortOrder())).orElse(null);
            responseItems.add(new CartResponse.Item(line.getId(), product.getId(), product.getName(), line.getQuantity(), available,
                    product.getPrice(), lineTotal, product.getCurrency(), image == null ? null : image.getStorageKey(),
                    image == null ? null : image.getAltText(), product.getStore().getSeller().getBusinessName()));
        }
        return new CartResponse(responseItems, subtotal, currency);
    }

    private boolean publiclySellable(ProductEntity product) {
        return product.getStatus() == ProductStatus.ACTIVE && ACTIVE.equals(product.getStore().getStatus())
                && product.getStore().getSeller().getStatus() == SellerStatus.APPROVED && product.getCategory().isActive();
    }
}
