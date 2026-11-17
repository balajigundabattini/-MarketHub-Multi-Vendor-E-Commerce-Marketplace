# MarketHub backend

Java 21 and Spring Boot API for the MarketHub modular monolith. Implemented endpoints include health, identity, customer profile/address, seller onboarding/store management, public catalog discovery, seller product/inventory management, admin category/product moderation, cart, and wishlist.

## Prerequisites

- Java 21
- MySQL 8.x

Maven is provided by the checked-in Maven Wrapper, so a global Maven installation is not required. The wrapper downloads Maven 3.10.1 on first use.

## Configure the database

Create a local MySQL database named `markethub`, then set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the shell before starting the application. Configure an SMTP relay (for local development, Mailpit or MailHog can listen on port 1025) to deliver verification and reset links. Generate a random signing key as well. Spring Boot does not load `.env` files automatically.

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/markethub'
$env:DB_USERNAME = 'markethub'
$env:DB_PASSWORD = 'your-local-password'
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
$env:FRONTEND_ORIGIN = 'http://localhost:5173'
$env:REFRESH_COOKIE_SECURE = 'false' # localhost only; use true over HTTPS
$env:SMTP_HOST = 'localhost'
$env:SMTP_PORT = '1025'
$env:SMTP_AUTH = 'false'
$env:SMTP_STARTTLS = 'false'
```

Flyway applies versioned SQL migrations. Hibernate uses `ddl-auto=validate`; it checks mapped entities against the migration-managed schema and does not create or mutate tables.

## Run

From this directory in PowerShell:

```powershell
./mvnw.cmd spring-boot:run
```

Health endpoint: `GET http://localhost:8080/api/v1/health`  
Actuator health: `GET http://localhost:8080/actuator/health`

Authentication routes are `POST /api/v1/auth/register`, `/verify-email`, `/resend-verification`, `/login`, `/refresh`, `/logout`, `/forgot-password`, and `/reset-password`. Registration accepts `displayName`, `email`, and `password` and always creates a pending customer account. Access tokens are returned in JSON for in-memory frontend use after verification and login; the rotating refresh token is an HttpOnly cookie. Refresh and logout use the signed `XSRF-TOKEN` double-submit cookie/header. Action tokens are single-use and stored as hashes.

Customer routes require a valid access token with the `CUSTOMER` role. Use `GET`/`PUT /api/v1/customer/profile` for the signed-in account, and `GET`/`POST /api/v1/customer/addresses`, `PUT`/`DELETE /api/v1/customer/addresses/{id}`, or `POST /api/v1/customer/addresses/{id}/default` for saved delivery addresses. The user ID is read from the verified JWT subject; requests cannot provide another owner's ID. Deleted addresses are deactivated rather than physically removed.

Seller onboarding is available at `GET|POST /api/v1/seller-applications` (including `GET /me`). Administrators review applications under `/api/v1/admin/seller-applications`; approval grants the seller role and creates the initial store transactionally. Approved sellers can manage their store at `GET|PUT /api/v1/seller/store`.

Public catalog routes are `GET /api/v1/catalog/categories`, `GET /api/v1/catalog/products`, and `GET /api/v1/catalog/products/{id}`. Seller-owned product and inventory routes use `/api/v1/seller/products` and `/api/v1/seller/inventory`. New or edited products require admin moderation before they appear in public discovery. Admin category and product moderation routes use `/api/v1/admin/categories` and `/api/v1/admin/products`. Every seller/admin endpoint is role guarded, and seller product/stock operations resolve ownership from the authenticated principal.

The authenticated customer cart and wishlist routes use `/api/v1/cart` and `/api/v1/wishlist`. Cart prices are read from current product data; the cart does not reserve stock and is not an authoritative checkout quote. Cart and wishlist operations are scoped to the authenticated customer's IDs.

Checkout, customer orders, coupons, Razorpay payments/refunds, seller fulfillment/reports, admin customer/order queues, and platform reports are implemented. Partial/COD refunds and remaining admin controls are pending. Payment integration requires `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, and `RAZORPAY_WEBHOOK_SECRET`. See [Phase 6 seller management](../docs/phase-6-seller-management.md), [Phase 7 catalog and products](../docs/phase-7-catalog-and-products.md), [Phase 8 cart and wishlist](../docs/phase-8-cart-and-wishlist.md), [Phase 9 checkout and orders](../docs/phase-9-checkout-and-orders.md), [Phase 10 payments and fulfillment](../docs/phase-10-payments-and-fulfillment.md), and [Phase 11 admin refunds and reports](../docs/phase-11-admin-refunds-and-reports.md) for workflow details and boundaries.

## Run tests

```powershell
./mvnw.cmd test
```

The automated backend suite includes health, identity, address, seller application, catalog filter, and seller ownership tests. Running against MySQL additionally requires a configured local database; Flyway migrations and Hibernate schema validation run on application startup.
