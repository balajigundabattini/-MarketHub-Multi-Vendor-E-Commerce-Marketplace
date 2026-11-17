package com.markethub.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.markethub.catalog.infrastructure.jpa.CategoryRepository;
import com.markethub.catalog.infrastructure.jpa.InventoryMovementRepository;
import com.markethub.catalog.infrastructure.jpa.InventoryRepository;
import com.markethub.catalog.infrastructure.jpa.ProductRepository;
import com.markethub.identity.infrastructure.jpa.UserRepository;
import com.markethub.seller.domain.SellerStatus;
import com.markethub.seller.infrastructure.jpa.SellerEntity;
import com.markethub.seller.infrastructure.jpa.SellerRepository;
import com.markethub.seller.infrastructure.jpa.StoreEntity;
import com.markethub.seller.infrastructure.jpa.StoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class SellerCatalogServiceTest {
    private final SellerRepository sellers = mock(SellerRepository.class);
    private final StoreRepository stores = mock(StoreRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final SellerCatalogService service = new SellerCatalogService(sellers, stores,
            mock(CategoryRepository.class), products, mock(InventoryRepository.class),
            mock(InventoryMovementRepository.class), mock(UserRepository.class));

    @Test
    void refusesProductAccessForAccountsWithoutApprovedSellerRecord() {
        when(sellers.findByUserId(55L)).thenReturn(Optional.empty());
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.listProducts(55L, 0, 20));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(products, never()).findAllByStoreId(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void refusesProductsWhenSellerStoreIsInactive() {
        SellerEntity seller = mock(SellerEntity.class);
        StoreEntity store = mock(StoreEntity.class);
        when(sellers.findByUserId(55L)).thenReturn(Optional.of(seller));
        when(seller.getStatus()).thenReturn(SellerStatus.APPROVED);
        when(seller.getId()).thenReturn(7L);
        when(stores.findBySellerId(7L)).thenReturn(Optional.of(store));
        when(store.getStatus()).thenReturn("SUSPENDED");
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.listProducts(55L, 0, 20));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(products, never()).findAllByStoreId(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }
}
