# Frontend delivery roadmap

This tracks the React screens requested in the MarketHub project brief. Screens become functional as their backend contracts are implemented; they should not use hardcoded commerce data to imitate completed workflows.

## Implemented

- Public home, customer registration/login, email verification, password recovery, and session restoration.
- In-memory token handling with a shared auth context, sign-out, and a role-aware protected-route component.
- Customer account page for profile and delivery address management, connected to the API.
- Shared responsive shell and API client.
- Public product listing/search/filter/sort/pagination and product detail screens, wired to the catalog API contract below.
- Customer cart quantity/removal controls, wishlist actions, and checkout UI using server-calculated quotes.
- Customer order history/detail screens with shipment tracking, eligible cancellation/return requests, and purchase-verified review submission.
- Customer notification center and payment status/retry screen.

## Customer application

- [x] Home, login, registration, profile, address book.
- [x] Product listing, search, category/price filters, sorting, and pagination screen.
- [x] Product detail screen and primary product image.
- [x] Compare up to four products and their available specifications.
- [x] Cart and wishlist screens and actions.
- [x] Checkout address selection, coupon entry, server quote, and payment method selection.
- [x] Payment status/retry page; provider checkout UI depends on the server payment session contract.
- [x] Order history, seller suborders, shipment tracking, cancellation/return requests, and product reviews.
- [x] Purchase-verified review submission and customer notification center.

## Seller dashboard

- [x] Seller application, business details, and onboarding status.
- [x] Store profile and store management.
- [x] Product creation, editing, image URLs, pricing, SKU, and moderation status.
- [x] Stock management and inventory adjustments.
- [x] Seller order processing and shipment updates.
- [x] Sales and commission reports.
- [ ] Return handling and settlement disbursement views need backend lifecycle contracts.

## Admin dashboard

- [x] Existing sign-in with admin role guard and admin dashboard.
- [x] Customer status management, seller application review, and suspension.
- [x] Category management and product moderation.
- [x] Order visibility, eligible refund requests, and coupon management.
- [x] Platform reporting and moderation queues.

## Shared interface work still required

- [x] Role-specific navigation and nested layouts for customer, seller, and admin areas.
- [x] Loading, empty, validation, and API failure states across primary workflows.
- Reusable tables, data filters, pagination, confirmation dialogs, and accessible form controls.
- [ ] Frontend tests for authentication, route access, forms, and shopping flows (no frontend test runner is configured).

## Dependency order

The backend currently implements identity, customer profile/address, seller onboarding/store management, catalog discovery, seller product/inventory management, and category/product moderation. Cart/checkout, payment, seller orders/reports, and other admin services are still outstanding. Provider checkout needs a server-issued provider session. Product images currently use URL inputs; secure upload needs a backend storage contract.

## Catalog frontend API contract

The catalog pages call these public endpoints, implemented in backend Phase 7:

- `GET /api/v1/catalog/categories` returns an array of active categories: `{ id, name, slug }`.
- `GET /api/v1/catalog/products` accepts `search`, `category` (slug), `minPrice`, `maxPrice`, `sort` (`newest`, `price_asc`, `price_desc`, `name_asc`), zero-based `page`, and `size`.
- Product listing responses use Spring page fields: `{ content, number, size, totalElements, totalPages }`; each item has `{ id, name, brand, price, currency, storeName, imageUrl, imageAlt }`.
- `GET /api/v1/catalog/products/{id}` returns `{ id, name, brand, description, price, currency, storeName, categoryName, images: [{ url, altText }], specifications: [{ name, value }] }`.

The `/compare` screen loads two to four product details using the same detail endpoint. The screens show API loading, empty, and failure states and do not fabricate catalog items.

## Cart and checkout frontend API contract

- `GET /api/v1/cart` returns `{ items: [{ id, productId, name, quantity, availableQuantity, unitPrice, lineTotal, currency, imageUrl, imageAlt, sellerName }], subtotal, currency }`.
- `PUT /api/v1/cart/items/{id}` accepts `{ quantity }`; `DELETE /api/v1/cart/items/{id}` removes the line. Both mutations return the updated cart representation.
- `POST /api/v1/cart/items` accepts `{ productId, quantity }` and returns the updated cart.
- `GET /api/v1/wishlist` returns `{ items: [{ productId, name, price, currency, imageUrl, imageAlt }] }`; add/remove use `POST /api/v1/wishlist/items` and `DELETE /api/v1/wishlist/items/{productId}`.
- `POST /api/v1/checkout/quote` accepts `{ addressId, couponCode }` and returns a server-owned quote `{ quoteId, subtotal, shipping, tax, discount, total, currency, expiresAt }` after stock and price validation.
- `POST /api/v1/checkout/orders` accepts `{ quoteId, addressId, paymentMethod }` and creates an order; the frontend never submits an authoritative price or treats an online payment as complete.

## Order management frontend API contract

- `GET /api/v1/orders?page=&size=` returns a Spring page of `{ id, orderNumber, createdAt, status, total, currency, sellerOrderCount }`.
- `GET /api/v1/orders/{id}` returns the owner's order, shipping address snapshot, payment status, seller suborders, shipments, and items. Item flags `canCancel`, `canReturn`, and `canReview` come from server-side policy.
- `POST /api/v1/orders/{id}/items/{itemId}/cancel` and `/returns` accept `{ reason }`; the server validates eligibility and ownership.
- `POST /api/v1/products/{productId}/reviews` accepts `{ orderItemId, rating, title, body }`; the server confirms purchase eligibility and moderates the review.
- `GET /api/v1/notifications?page=&size=` returns `{ content: [{ id, title, message, createdAt, read, orderId, paymentId }] }`; mark one or all read with `PUT /api/v1/notifications/{id}/read` or `PUT /api/v1/notifications/read-all`.
- `GET /api/v1/payments/{id}` returns `{ paymentId, orderId, status, amount, currency, canRetry }`; `POST /api/v1/payments/{id}/retry` creates a new provider attempt and returns its current state. Provider checkout UI must use the server-issued payment session and verify the callback server-side.

## Seller frontend API contract

Application/store profile endpoints are implemented in backend Phase 6. Product and inventory endpoints are implemented in backend Phase 7. Seller orders and reporting remain future backend work.

- `GET /api/v1/seller-applications/me` returns the current user's application or not-found; `POST /api/v1/seller-applications` accepts `{ legalName, businessName }` and returns application status.
- `GET /api/v1/seller/dashboard` returns `{ grossSales, orderCount, pendingOrders, productCount, currency }`.
- `GET|PUT /api/v1/seller/store` reads or updates `{ name, description }`.
- `GET /api/v1/seller/products?page=&size=` returns a Spring page of product data; detail/create/update use `/api/v1/seller/products/{id}` and `/api/v1/seller/products`. `POST /api/v1/seller/products/{id}/deactivate` deactivates a listing. New and edited listings enter moderation.
- `GET /api/v1/seller/inventory` returns rows `{ productId, name, sku, onHand, reserved, available }`; `PATCH /api/v1/seller/inventory/{productId}` accepts `{ onHand, reason }`, locks the stock row, and rejects quantities below reserved stock.
- `GET /api/v1/seller/orders?page=&size=` returns order rows and policy flags (`canAccept`, `canReject`, `canStartProcessing`, `canShip`); `PATCH /api/v1/seller/orders/{id}/status` accepts `{ status }`; shipment creation accepts `{ carrier, trackingNumber }`.
- `GET /api/v1/seller/reports?from=&to=` returns `{ grossSales, commission, settlement, currency, daily }`.

## Admin frontend API contract

Seller application listing and approval/rejection endpoints are implemented in backend Phase 6. Category management and product moderation are implemented in backend Phase 7. Other admin endpoints below remain future frontend contracts.

- `GET /api/v1/admin/dashboard` returns platform sales, order/customer/active-seller counts, pending applications, and products awaiting moderation.
- `GET /api/v1/admin/customers?page=&size=&status=` returns a Spring page; `PATCH /api/v1/admin/customers/{id}/status` accepts `{ status }`.
- `GET /api/v1/admin/seller-applications?page=&size=&status=` returns a Spring page; `PATCH /api/v1/admin/seller-applications/{id}/decision` accepts `{ status, reason? }`.
- `GET /api/v1/admin/categories` lists categories; category create/update accepts `{ name, slug, description }`; `PATCH /api/v1/admin/categories/{id}/status` accepts `{ active }` for soft activation/deactivation.
- `GET /api/v1/admin/products?page=&size=&status=` returns a product page; `PATCH /api/v1/admin/products/{id}/moderation` accepts `{ status: ACTIVE|REJECTED }` for products awaiting review.
- `GET /api/v1/admin/orders?page=&size=&status=` returns order/payment states and `canRefund`; refund requests accept `{ reason }` and the server must enforce eligibility and confirm provider refunds.
- `GET /api/v1/admin/coupons` lists rules; create/update accepts code, discount type/value, minimum, usage limit, start/end dates, and active state.
- `GET /api/v1/admin/reports?from=&to=` returns `{ grossSales, orderCount, commissionTotal, refundTotal, daily }`.

These are frontend contracts, not claims that server endpoints exist. Admin mutations require server-side authorization and audit records; client route guards only control navigation.
