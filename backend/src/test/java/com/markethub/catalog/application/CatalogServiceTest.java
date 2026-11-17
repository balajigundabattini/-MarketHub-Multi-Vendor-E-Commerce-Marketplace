package com.markethub.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.markethub.catalog.domain.ProductStatus;
import com.markethub.catalog.infrastructure.jpa.CategoryEntity;
import com.markethub.catalog.infrastructure.jpa.CategoryRepository;
import com.markethub.catalog.infrastructure.jpa.InventoryRepository;
import com.markethub.catalog.infrastructure.jpa.ProductRepository;
import com.markethub.seller.domain.SellerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class CatalogServiceTest {
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final InventoryRepository inventory = mock(InventoryRepository.class);
    private final CatalogService service = new CatalogService(categories, products, inventory);

    @Test
    void returnsOnlyActiveCategories() {
        CategoryEntity category = mock(CategoryEntity.class);
        when(categories.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(category));
        when(category.getId()).thenReturn(2L);
        when(category.getName()).thenReturn("Electronics");
        when(category.getSlug()).thenReturn("electronics");
        when(category.isActive()).thenReturn(true);
        assertEquals("electronics", service.activeCategories().getFirst().slug());
    }

    @Test
    void searchesOnlyApprovedActiveProductsAndUsesAllowedFilters() {
        BigDecimal min = new BigDecimal("10.00");
        BigDecimal max = new BigDecimal("90.00");
        when(products.searchCatalog(eq(ProductStatus.ACTIVE), eq("ACTIVE"), eq(SellerStatus.APPROVED),
                eq("camera"), eq("electronics"), eq(min), eq(max), any(Pageable.class))).thenReturn(Page.empty());
        Page<?> result = service.search(" camera ", "electronics", min, max, "price_asc", 0, 12);
        assertEquals(0, result.getTotalElements());
        verify(products).searchCatalog(eq(ProductStatus.ACTIVE), eq("ACTIVE"), eq(SellerStatus.APPROVED),
                eq("camera"), eq("electronics"), eq(min), eq(max), any(Pageable.class));
    }

    @Test
    void rejectsInvalidPriceRange() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.search(null, null, new BigDecimal("50"), new BigDecimal("10"), "newest", 0, 12));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void hidesProductsThatAreNotPubliclySellable() {
        when(products.findByIdAndStatusAndStore_StatusAndStore_Seller_Status(
                11L, ProductStatus.ACTIVE, "ACTIVE", SellerStatus.APPROVED)).thenReturn(Optional.empty());
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.getProduct(11L));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
