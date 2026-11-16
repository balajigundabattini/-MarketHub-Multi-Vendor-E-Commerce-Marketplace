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

- [ ] Seller application, business details, and onboarding status.
- [ ] Store profile and store management.
- [ ] Product creation, editing, images, pricing, SKU, and moderation status.
- [ ] Stock management and inventory history.
- [ ] Seller order processing, shipment updates, and return handling.
- [ ] Sales, commission, and settlement reports.

## Admin dashboard

- [ ] Admin sign-in route and dashboard.
- [ ] Customer and seller management, seller approval, and suspension.
- [ ] Category management and product moderation.
- [ ] Order, payment, refund, and coupon management.
- [ ] Platform reporting and moderation queues.

## Shared interface work still required

- Role-specific navigation and nested layouts for customer, seller, and admin areas.
- Consistent loading, empty, validation, and API failure states across all workflows.
- Reusable tables, data filters, pagination, confirmation dialogs, and accessible form controls.
- Frontend tests for authentication, route access, forms, and shopping flows.

## Dependency order

Build and verify matching backend contracts before wiring each commerce screen. The product listing/details/discovery screens are ready against the contract below; the next integration milestone is implementing the category/catalog backend. Checkout depends on cart, inventory, address, and pricing APIs, while seller and admin dashboards depend on their respective authorization and management services.

## Catalog frontend API contract

The catalog pages call these public endpoints; the backend catalog phase must implement this contract or update both sides together:

- `GET /api/v1/catalog/categories` returns an array of active categories: `{ id, name, slug }`.
- `GET /api/v1/catalog/products` accepts `search`, `category` (slug), `minPrice`, `maxPrice`, `sort` (`newest`, `price_asc`, `price_desc`, `name_asc`), zero-based `page`, and `size`.
- Product listing responses use Spring page fields: `{ content, number, size, totalElements, totalPages }`; each item has `{ id, name, brand, price, currency, storeName, imageUrl, imageAlt }`.
- `GET /api/v1/catalog/products/{id}` returns `{ id, name, brand, description, price, currency, storeName, categoryName, images: [{ url, altText }], specifications: [{ name, value }] }`.

The `/compare` screen loads two to four product details using the same detail endpoint. The screens show API loading, empty, and failure states. They do not fabricate catalog items while the backend endpoints are unavailable.

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
