package com.markethub.cart.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.markethub.cart.infrastructure.jpa.CartEntity;
import com.markethub.cart.infrastructure.jpa.CartItemEntity;
import com.markethub.cart.infrastructure.jpa.CartItemRepository;
import com.markethub.cart.infrastructure.jpa.CartRepository;
import com.markethub.catalog.domain.ProductStatus;
import com.markethub.catalog.infrastructure.jpa.CategoryEntity;
import com.markethub.catalog.infrastructure.jpa.InventoryEntity;
import com.markethub.catalog.infrastructure.jpa.InventoryRepository;
import com.markethub.catalog.infrastructure.jpa.ProductEntity;
import com.markethub.catalog.infrastructure.jpa.ProductRepository;
import com.markethub.identity.infrastructure.jpa.UserRepository;
import com.markethub.seller.domain.SellerStatus;
import com.markethub.seller.infrastructure.jpa.SellerEntity;
import com.markethub.seller.infrastructure.jpa.StoreEntity;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class CartServiceTest {
    private final CartRepository carts = mock(CartRepository.class);
    private final CartItemRepository items = mock(CartItemRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final InventoryRepository inventory = mock(InventoryRepository.class);
    private final CartService service = new CartService(carts, items, products, inventory, mock(UserRepository.class));

    @Test
    void cannotChangeAnotherCustomersCartItem() {
        CartEntity cart = mock(CartEntity.class);
        when(cart.getId()).thenReturn(8L);
        when(carts.findByUserIdAndStatus(12L, "ACTIVE")).thenReturn(Optional.of(cart));
        when(items.findByIdAndCartId(77L, 8L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.update(12L, 77L, 1));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(inventory, never()).findByProductIdForUpdate(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void doesNotAddInactiveOrUnapprovedProducts() {
        CartEntity cart = mock(CartEntity.class);
        when(cart.getId()).thenReturn(8L);
        when(carts.findByUserIdAndStatus(12L, "ACTIVE")).thenReturn(Optional.of(cart));
        when(products.findByIdAndStatusAndStore_StatusAndStore_Seller_StatusAndCategory_Active(
                44L, ProductStatus.ACTIVE, "ACTIVE", SellerStatus.APPROVED, true)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.add(12L, 44L, 1));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(items, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsLiveSubtotalAndAvailabilityFromCurrentProductAndStock() {
        CartEntity cart = mock(CartEntity.class);
        CartItemEntity line = mock(CartItemEntity.class);
        ProductEntity product = mock(ProductEntity.class);
        InventoryEntity stock = mock(InventoryEntity.class);
        StoreEntity store = mock(StoreEntity.class);
        SellerEntity seller = mock(SellerEntity.class);
        CategoryEntity category = mock(CategoryEntity.class);
        when(cart.getId()).thenReturn(8L);
        when(carts.findByUserIdAndStatus(12L, "ACTIVE")).thenReturn(Optional.of(cart));
        when(items.findAllByCartId(8L)).thenReturn(List.of(line));
        when(line.getId()).thenReturn(77L);
        when(line.getProduct()).thenReturn(product);
        when(line.getQuantity()).thenReturn(2);
        when(product.getId()).thenReturn(44L);
        when(product.getName()).thenReturn("Desk lamp");
        when(product.getPrice()).thenReturn(new BigDecimal("12.50"));
        when(product.getCurrency()).thenReturn("INR");
        when(product.getImages()).thenReturn(List.of());
        when(product.getStore()).thenReturn(store);
        when(store.getSeller()).thenReturn(seller);
        when(seller.getBusinessName()).thenReturn("North Pine");
        when(seller.getStatus()).thenReturn(SellerStatus.APPROVED);
        when(store.getStatus()).thenReturn("ACTIVE");
        when(product.getStatus()).thenReturn(ProductStatus.ACTIVE);
        when(product.getCategory()).thenReturn(category);
        when(category.isActive()).thenReturn(true);
        when(inventory.findAllByProductIdIn(List.of(44L))).thenReturn(List.of(stock));
        when(stock.getProductId()).thenReturn(44L);
        when(stock.getAvailable()).thenReturn(3);

        var response = service.getCart(12L);

        assertEquals(new BigDecimal("25.00"), response.subtotal());
        assertEquals(3, response.items().getFirst().availableQuantity());
        assertEquals("North Pine", response.items().getFirst().sellerName());
    }
}
