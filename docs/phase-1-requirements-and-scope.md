# MarketHub — Phase 1: Requirements and Scope

**Status:** Draft baseline for implementation  
**Purpose:** Define the product scope, core workflows, and acceptance criteria before architecture and database design.

## 1. Product summary

MarketHub is a web marketplace where multiple approved sellers list products and customers can buy products from multiple sellers in one checkout. The customer receives one parent order view; sellers fulfill and manage their own suborders. Administrators govern sellers, catalog content, and platform operations.

The initial system is a modular monolith: one React frontend, one Spring Boot REST API, and one MySQL database. It will be developed as a portfolio project with explicit attention to security, data integrity, and explainable business workflows.

## 2. Goals

1. Deliver a usable customer shopping flow from product discovery through order tracking.
2. Allow sellers to onboard after approval and manage only their own store, products, inventory, and fulfillment.
3. Give administrators control over seller approval, catalog governance, and marketplace operations.
4. Preserve order and payment history accurately when catalog data changes or a workflow fails.
5. Build the core flows in a form that can be demonstrated and explained in a Java full-stack interview.

## 3. Out of scope for the first release

- Splitting the application into microservices.
- Production payment processing, handling card details, or real-money payouts.
- A native mobile application.
- Recommendation or personalization algorithms.
- Multi-region, multi-currency, or international tax support.
- Full shipping-carrier integrations; initial shipment tracking is managed by the platform/seller.
- Redis and Kafka before a concrete performance or asynchronous-processing need is established.

These can be considered later without blocking the core marketplace workflow.

## 4. Actors and permissions

| Actor | Core permissions |
|---|---|
| Visitor | Browse and search active products; register or log in. |
| Customer | Manage own profile and addresses, cart, wishlist, orders, eligible cancellations/returns, and purchase reviews. |
| Seller applicant | Submit store/business details and view application status. |
| Approved seller | Manage own store, products, stock, seller suborders, shipments, and return requests. |
| Administrator | Approve/suspend sellers, govern categories and products, inspect orders/payments, manage promotions, and review reports. |

Authorization is enforced by the backend on every protected operation. A role check alone is insufficient: customer and seller ownership must also be verified. Public sign-up must not let a visitor grant themselves administrator privileges.

## 5. Functional requirements

### Identity and profiles

- Register and authenticate users; store only password hashes.
- Support role-aware access for customer, seller, and administrator workflows.
- Manage customer profile and multiple delivery addresses.
- Support seller application, administrator review, and active/suspended states.
- Provide email verification, password recovery, and refresh-token rotation as planned authentication capabilities; sequence these after the initial secure registration/login slice if needed.

### Catalog and discovery

- Maintain hierarchical categories and active/inactive state.
- Let approved sellers create and manage their own products, images, SKUs/variants, price, and stock.
- Let administrators moderate or manage products according to the configured policy.
- Show active products with detail pages, search, filtering, sorting, and pagination.
- Record product attributes appropriate to the category (for example brand, color, size, weight, and dimensions).

### Cart, pricing, and checkout

- Persist a customer cart and wishlist.
- Add/remove cart items and update quantities with stock validation.
- Re-read current product prices and availability during checkout; never accept a client-supplied total as authoritative.
- Validate shipping address, coupon eligibility, tax, and shipping charges on the backend.
- Prevent overselling when purchases compete for the final stock units.

### Orders and fulfillment

- Create one customer-facing parent order for a checkout and seller-specific suborders for each participating seller.
- Snapshot purchased item name/SKU/price so later catalog edits do not change order history.
- Maintain an auditable status history and enforce allowed transitions.
- Support seller-specific fulfillment, shipments, eligible cancellation, returns, and refunds while retaining a unified customer order history.
- Core status vocabulary: `PENDING_PAYMENT`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `OUT_FOR_DELIVERY`, `DELIVERED`, `CANCELLED`, `RETURN_REQUESTED`, `RETURN_APPROVED`, `RETURNED`, `REFUNDED`.

### Payments and financial records

- Use Razorpay test mode for payment initiation and verification.
- Verify provider signatures server-side and treat verified webhooks/provider status as authoritative.
- Handle retries and duplicate webhook delivery idempotently.
- Keep payment attempts, refunds, commissions, and settlements traceable; never store card numbers.

### Marketplace operations

- Manage coupons with validity windows and usage limits.
- Permit reviews only under the defined purchase eligibility rules.
- Provide basic seller sales and admin platform reports.
- Notify users about important account, order, payment, and fulfillment events; delivery failure must not corrupt the underlying order.

## 6. Non-functional requirements

- **Security:** BCrypt password hashing; secret configuration outside source control; server-side validation; secure authorization and ownership checks; safe payment verification.
- **Consistency:** Transactional order creation and inventory updates; database constraints; idempotent payment/webhook processing.
- **Auditability:** Preserve status changes and inventory movements; snapshot order-time product and pricing data.
- **Usability:** Responsive customer storefront and role-specific seller/admin interfaces with clear validation and status feedback.
- **Maintainability:** Modular business packages, DTO-based API boundaries, consistent error responses, and documented decisions.
- **Performance:** Pagination for lists, suitable indexes for common filters and lookups, and avoid premature infrastructure complexity.
- **Operability:** Health endpoints, useful logs, environment-based configuration, and reproducible local setup.
- **Testability:** Business rules and access boundaries should be verifiable at unit/API/integration levels as their modules are implemented.

## 7. Core business rules

1. Only an approved and active seller may publish or manage listings.
2. A seller may only read or change their own store data, products, stock, and seller suborders.
3. A customer may only read or change their own profile, cart, addresses, and orders.
4. Product price and availability are revalidated at checkout; the backend calculates all payable amounts.
5. Stock cannot be sold below zero. Concurrent checkout must use a transaction and a concurrency-safe update/lock strategy.
6. A parent order belongs to one customer; seller suborders group line items by seller and can progress independently.
7. Purchased line items preserve a snapshot of the transaction’s product description, SKU, and price.
8. Payment success is accepted only after server-side provider verification. Replayed events must not duplicate order/payment effects.
9. Cancellation, return, refund, and stock-restoration eligibility depends on order/suborder/item state and documented policy.
10. Every status change must be authorized and recorded with actor, time, and prior/new state.

## 8. Key workflows

### Seller onboarding and listing

Seller applies → administrator approves or rejects → approved seller creates a listing → listing is active after any configured moderation → customer can discover and purchase it.

### Multi-vendor checkout

Customer reviews cart → backend reloads prices and stock → validates address/coupon and calculates totals → reserves/deducts inventory safely → creates parent order and seller suborders → starts payment → verifies provider result/webhook → seller suborders become fulfillable.

If payment fails or expires, the system records the failure and follows a defined retry/expiry policy that releases reserved inventory where applicable. An order must not be marked paid because the browser claims success.

### Seller fulfillment

Seller sees only their suborders → updates allowed fulfillment states → shipment/tracking is recorded per seller suborder → customer sees the combined parent order and each seller shipment.

### Cancellation and return

Customer requests an eligible action → backend validates timing/state and affected items → seller/admin decision is recorded where required → refund and inventory restoration are processed idempotently → customer and seller views reflect the result.

## 9. MVP boundary

### Required for the first demonstrable end-to-end release

- Customer registration/login and protected routes/endpoints.
- Seller application and administrator approval.
- Category and seller-owned product management.
- Product listing/detail, search, filters, sort, and pagination.
- Customer address, cart, and wishlist basics.
- Server-calculated checkout and concurrency-safe inventory behavior.
- One parent order plus seller-specific suborders.
- Razorpay test-mode payment, server verification, and webhook idempotency.
- Basic seller fulfillment and customer order history.
- Basic admin seller/catalog/order management.
- README setup instructions and API documentation.

### Later increments

- Email verification, password reset, refresh-token rotation, and account recovery refinements.
- Advanced product variants and moderation workflows.
- Full coupon/promotion rules, returns/refunds, reviews, and settlement reporting.
- Email notifications, Redis caching, Kafka processing, richer analytics, and deployment hardening.

This prioritization is a sequencing choice; the final portfolio scope can include later increments after the end-to-end MVP is stable.

## 10. Dependencies and sequencing

`Identity → seller approval/profile → categories and catalog → discovery → cart/address → inventory and checkout → orders → payment → fulfillment → returns/reviews/settlements/reporting`.

Security and API conventions begin with identity and apply to every later module. Database design must settle the parent-order/seller-suborder and inventory approach before checkout implementation. Payments depend on order creation and must be designed together with failure and retry behavior.

## 11. Acceptance criteria for Phase 1

- The product purpose, user roles, and access boundaries are documented.
- MVP scope and explicitly deferred work are identified.
- Core workflows cover seller onboarding, multi-seller checkout, fulfillment, and cancellation/return.
- Business rules cover server-authoritative pricing, seller/customer ownership, stock concurrency, and payment idempotency.
- Module dependencies are ordered so later design work can proceed without assuming unrelated CRUD features.
- The existing repository baseline is recorded accurately: it is currently a Vite/React frontend starter with basic login/register pages and route wiring; no completed marketplace workflow or backend implementation is assumed.

## 12. Current assumptions to validate during design

- “MarketHub” is the product name. The current local folder still has its original `sales-savvy-frontend` directory name; the npm package identity is now `markethub-marketplace`.
- The first release targets one country/currency and a single deployment region.
- Shipping rates, tax rules, seller commission rates, and return windows will begin as configurable/simple policies rather than integrations with external services.
- Product images are stored as media references; the storage provider and upload flow will be chosen during architecture/setup.
- The backend is a separate Spring Boot project to be added alongside this frontend unless the repository structure or user direction indicates otherwise.

These assumptions are visible design inputs, not claims that those capabilities are already implemented.
