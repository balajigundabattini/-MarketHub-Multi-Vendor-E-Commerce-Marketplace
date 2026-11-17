# MarketHub – Phase 7: Catalog, Products, and Inventory

**Status:** Catalog discovery, seller product management, seller inventory adjustment, and category/product moderation APIs implemented  
**Packages:** `com.markethub.catalog`, seller APIs under `com.markethub.seller`

## Business workflow

Administrators create and maintain active marketplace categories. An approved seller creates a product in an active category. New and edited products enter `PENDING_APPROVAL`; an administrator can activate or reject a pending listing. Public search only returns products whose product, store, seller, and category are all active/approved. A seller can read or change only products and stock belonging to their own approved store.

The database keeps stock in `inventory`. Manual seller adjustments lock the row with `SELECT ... FOR UPDATE`, reject values below reserved stock, and add an `inventory_movements` audit row. Checkout will later use the same inventory records for its own atomic reservation workflow.

## Public catalog API

- `GET /api/v1/catalog/categories` returns active categories ordered by name: `{ id, name, slug, description, active }`.
- `GET /api/v1/catalog/products` accepts `search`, `category` slug, `minPrice`, `maxPrice`, `sort` (`newest`, `price_asc`, `price_desc`, `name_asc`), zero-based `page`, and `size` (maximum 100). The response is Spring `Page<ProductResponse>`.
- `GET /api/v1/catalog/products/{id}` returns a public product only if the product is active, its store is active, the seller is approved, and its category is active. Other records return 404.
- Product responses contain `id`, `name`, `sku`, `slug`, `brand`, `description`, `price`, `currency`, `categoryId`, `categoryName`, `storeName`, `sellerName`, `status`, `imageUrl`, `imageAlt`, `images`, `specifications`, `availableQuantity`, and `createdAt`.

Search price bounds, page sizes, and sort keys are validated server-side. A client-provided total is never used for a purchase calculation.

## Seller catalog and inventory API

- `GET /api/v1/seller/products?page=&size=` returns a page scoped to the authenticated seller's store.
- `GET /api/v1/seller/products/{id}` returns the seller-owned product or 404.
- `POST /api/v1/seller/products` and `PUT /api/v1/seller/products/{id}` accept SKU, name, category ID, brand, description, price, currency, and up to ten HTTP(S) image URLs. SKU uniqueness is enforced per store. New or edited listings enter moderation.
- `POST /api/v1/seller/products/{id}/deactivate` soft-deactivates an owned product.
- `GET /api/v1/seller/inventory` returns `{ productId, name, sku, onHand, reserved, available }` rows.
- `PATCH /api/v1/seller/inventory/{productId}` accepts `{ onHand, reason }`, applies a row lock, and records the delta and actor in `inventory_movements`.

Image URLs are temporary references stored in `storage_key`. The production upload, file validation, malware scanning, and object storage flow remains future work.

## Admin catalog API

- `GET /api/v1/admin/categories` lists active and inactive categories.
- `POST /api/v1/admin/categories` and `PUT /api/v1/admin/categories/{id}` accept `{ name, slug, description }`.
- `PATCH /api/v1/admin/categories/{id}/status` accepts `{ active }`. Deactivation is soft; existing product/order history is preserved and products in inactive categories stop appearing publicly.
- `GET /api/v1/admin/products?page=&size=&status=` returns a product page.
- `PATCH /api/v1/admin/products/{id}/moderation` accepts `{ status: "ACTIVE"|"REJECTED" }` for products in `PENDING_APPROVAL`.

## Database changes

Flyway migration `V3__category_description.sql` adds optional category descriptions. Existing product, product image, inventory, and stock movement tables are reused. JPA remains in `ddl-auto=validate` mode.

## Verification

`CatalogServiceTest` covers category list mapping, the approved/public listing filters, and invalid price bounds. `SellerCatalogServiceTest` verifies that unapproved sellers and inactive stores cannot access their product collection. Run the backend suite with `cd backend; .\mvnw.cmd test`.

## Remaining work

This phase does not implement product variants/specifications, rating aggregates, full-text search tuning, image upload, carts, checkout, inventory reservations, or seller order/revenue screens. Product and stock APIs are prerequisites for those later modules.
