# MarketHub – Phase 8: Cart and Wishlist

**Status:** Persistent customer cart and wishlist APIs implemented  
**Packages:** `com.markethub.cart`, `com.markethub.wishlist`

## Business workflow

Cart lines persist for the authenticated customer and refer to product records rather than copying prices. Each read calculates line totals and the subtotal from the current catalog price. Add and quantity updates lock the inventory row and validate current availability, but they do not reserve stock. Checkout will perform its own transactional price and stock validation so browsing carts cannot block inventory.

Wishlist entries persist independently of product sellability. The response marks each entry as available/unavailable; customers can keep saved products but cannot move a currently unavailable product to the cart.

## API

### Cart

- `GET /api/v1/cart` returns `{ items, subtotal, currency }`.
- `POST /api/v1/cart/items` accepts `{ productId, quantity }`; adding an existing product increments its line.
- `PUT /api/v1/cart/items/{itemId}` accepts `{ quantity }`.
- `DELETE /api/v1/cart/items/{itemId}` removes the line and returns the updated cart.
- Cart item fields are `{ id, productId, name, quantity, availableQuantity, unitPrice, lineTotal, currency, imageUrl, imageAlt, sellerName }`.

Quantities are limited to 1–99 and cannot exceed current available stock. A cart uses one currency. The customer identity comes only from the verified JWT subject, and mutation lookups scope item IDs through the caller's cart.

### Wishlist

- `GET /api/v1/wishlist` returns `{ items: [{ productId, name, price, currency, imageUrl, imageAlt, available }] }`.
- `POST /api/v1/wishlist/items` accepts `{ productId }`; the unique `(wishlist_id, product_id)` key makes saved entries distinct.
- `DELETE /api/v1/wishlist/items/{productId}` removes the caller's saved entry.

## Persistence and concurrency

The APIs map to the existing `carts`, `cart_items`, `wishlists`, and `wishlist_items` tables. The first cart/wishlist creation locks the user row, then rechecks for an existing record to avoid duplicate creation under concurrent requests. Inventory updates are not made by cart mutations; inventory reads are checked under a pessimistic lock before accepting a requested quantity.

## Verification

`CartServiceTest` checks owner-scoped updates, rejection of unsellable products, and live subtotal/availability mapping. Run `cd backend; .\mvnw.cmd test` for the full backend suite.

## Remaining work

This phase does not reserve stock or calculate coupons, tax, shipping, or checkout totals. Cart/wishlist endpoints use the V1 tables and do not add a schema migration. Server-priced checkout and order creation are the next dependent phase.
