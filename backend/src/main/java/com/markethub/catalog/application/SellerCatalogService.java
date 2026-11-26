package com.markethub.catalog.application;

import java.util.List;

import com.markethub.catalog.api.InventoryResponse;
import com.markethub.catalog.api.InventoryUpdateRequest;
import com.markethub.catalog.api.ProductRequest;
import com.markethub.catalog.api.ProductResponse;
import com.markethub.catalog.infrastructure.jpa.CategoryEntity;
import com.markethub.catalog.infrastructure.jpa.CategoryRepository;
import com.markethub.catalog.infrastructure.jpa.InventoryEntity;
import com.markethub.catalog.infrastructure.jpa.InventoryMovementEntity;
import com.markethub.catalog.infrastructure.jpa.InventoryMovementRepository;
import com.markethub.catalog.infrastructure.jpa.InventoryRepository;
import com.markethub.catalog.infrastructure.jpa.ProductEntity;
import com.markethub.catalog.infrastructure.jpa.ProductRepository;
import com.markethub.identity.infrastructure.jpa.UserRepository;
import com.markethub.seller.domain.SellerStatus;
import com.markethub.seller.infrastructure.jpa.SellerEntity;
import com.markethub.seller.infrastructure.jpa.SellerRepository;
import com.markethub.seller.infrastructure.jpa.StoreEntity;
import com.markethub.seller.infrastructure.jpa.StoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SellerCatalogService {
    private final SellerRepository sellers;
    private final StoreRepository stores;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final InventoryRepository inventory;
    private final InventoryMovementRepository movements;
    private final UserRepository users;

    public SellerCatalogService(SellerRepository sellers, StoreRepository stores, CategoryRepository categories,
            ProductRepository products, InventoryRepository inventory, InventoryMovementRepository movements,
            UserRepository users) {
        this.sellers = sellers; this.stores = stores; this.categories = categories; this.products = products;
        this.inventory = inventory; this.movements = movements; this.users = users;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(Long userId, int page, int size) {
        StoreEntity store = storeFor(userId);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return products.findAllByStoreId(store.getId(), pageable)
                .map(product -> CatalogMapper.toResponse(product, inventory.findById(product.getId()).map(i -> i.getAvailable()).orElse(0)));
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long userId, Long productId) {
        StoreEntity store = storeFor(userId);
        ProductEntity product = products.findByIdAndStoreId(productId, store.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product was not found"));
        return CatalogMapper.toResponse(product, inventory.findById(productId).map(i -> i.getAvailable()).orElse(0));
    }

    @Transactional
    public ProductResponse createProduct(Long userId, ProductRequest request) {
        StoreEntity store = storeFor(userId);
        CategoryEntity category = activeCategory(request.categoryId());
        if (products.existsByStoreIdAndSkuIgnoreCase(store.getId(), request.sku().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU is already used in this store");
        }
        ProductEntity product = new ProductEntity(store, category, request.sku().trim(), request.name().trim(),
                clean(request.brand()), request.description().trim(), request.price(), request.currency());
        product.replaceImages(request.imageUrls() == null ? List.of() : request.imageUrls());
        products.saveAndFlush(product);
        inventory.save(new InventoryEntity(product.getId()));
        return CatalogMapper.toResponse(product, 0);
    }

    @Transactional
    public ProductResponse updateProduct(Long userId, Long productId, ProductRequest request) {
        StoreEntity store = storeFor(userId);
        ProductEntity product = products.findByIdAndStoreId(productId, store.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product was not found"));
        CategoryEntity category = activeCategory(request.categoryId());
        if (products.existsByStoreIdAndSkuIgnoreCaseAndIdNot(store.getId(), request.sku().trim(), productId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU is already used in this store");
        }
        product.update(category, request.sku().trim(), request.name().trim(), clean(request.brand()),
                request.description().trim(), request.price(), request.currency());
        product.replaceImages(request.imageUrls() == null ? List.of() : request.imageUrls());
        return CatalogMapper.toResponse(product, inventory.findById(productId).map(i -> i.getAvailable()).orElse(0));
    }

    @Transactional
    public void deactivateProduct(Long userId, Long productId) {
        StoreEntity store = storeFor(userId);
        ProductEntity product = products.findByIdAndStoreId(productId, store.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product was not found"));
        product.deactivate();
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> inventory(Long userId) {
        StoreEntity store = storeFor(userId);
        List<ProductEntity> rows = products.findAllByStoreId(store.getId(), PageRequest.of(0, 500)).getContent();
        var byId = inventory.findAllByProductIdIn(rows.stream().map(ProductEntity::getId).toList()).stream()
                .collect(java.util.stream.Collectors.toMap(InventoryEntity::getProductId, item -> item));
        return rows.stream().map(product -> {
            InventoryEntity stock = byId.get(product.getId());
            return new InventoryResponse(product.getId(), product.getName(), product.getSku(),
                    stock == null ? 0 : stock.getOnHand(), stock == null ? 0 : stock.getReserved(), stock == null ? 0 : stock.getAvailable());
        }).toList();
    }

    @Transactional
    public InventoryResponse adjustInventory(Long userId, Long productId, InventoryUpdateRequest request) {
        StoreEntity store = storeFor(userId);
        ProductEntity product = products.findByIdAndStoreId(productId, store.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product was not found"));
        InventoryEntity stock = inventory.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory record was not found"));
        if (request.onHand() < stock.getReserved()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "On-hand stock cannot be lower than reserved stock");
        }
        int delta = request.onHand() - stock.getOnHand();
        stock.setOnHand(request.onHand());
        if (delta != 0) {
            var actor = users.findById(userId).orElse(null);
            String reason = request.reason() == null || request.reason().isBlank() ? "SELLER_ADJUSTMENT" : request.reason().trim();
            movements.save(new InventoryMovementEntity(product, delta, reason, actor));
        }
        return new InventoryResponse(productId, product.getName(), product.getSku(), stock.getOnHand(), stock.getReserved(), stock.getAvailable());
    }

    private StoreEntity storeFor(Long userId) {
        SellerEntity seller = sellers.findByUserId(userId)
                .filter(s -> s.getStatus() == SellerStatus.APPROVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Approved seller access is required"));
        return stores.findBySellerId(seller.getId())
                .filter(store -> "ACTIVE".equals(store.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Active seller store access is required"));
    }

    private CategoryEntity activeCategory(Long categoryId) {
        return categories.findById(categoryId).filter(CategoryEntity::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active product category was not found"));
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
