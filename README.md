# MarketHub - Multi-Vendor E-Commerce Marketplace

MarketHub is a portfolio project for a multi-vendor marketplace. Customers will discover and buy products from multiple sellers; sellers will manage their own stores and fulfillment; administrators will oversee seller approval and marketplace operations.

Project repository: [balajigundabattini/-MarketHub-Multi-Vendor-E-Commerce-Marketplace](https://github.com/balajigundabattini/-MarketHub-Multi-Vendor-E-Commerce-Marketplace)

The project is being built in phases as a modular monolith: a React frontend, a Java 21 / Spring Boot REST backend, and MySQL. Customer identity/profile/address, seller onboarding, catalog discovery, seller product/inventory, moderation, cart, wishlist, checkout, coupons, customer orders, Razorpay payment verification, fulfillment, refunds, seller reports, and core admin queues/dashboard are implemented. Some advanced admin workflows remain in progress.

## Current status

- Project identity and initial requirements/scope are established.
- Frontend scaffold: React, Vite, JavaScript, React Router, Redux Toolkit, Axios, and Bootstrap.
- Backend scaffold: Java 21, Spring Boot 4.1.2, Maven Wrapper, Actuator, and a health endpoint.
- Flyway migrations define identity, customer, seller, catalog, shopping, checkout, coupon, order, payment, shipment, refund, and audit tables; applying them and validating against a configured local database is still required.
- Authentication API: customer registration with email verification, login, refresh rotation, logout, password recovery, JWT resource-server validation, and frontend session restoration.
- Customer profile and address API: authenticated profile updates and owner-scoped delivery address management with default selection and soft deletion.
- Seller application review, store profiles, seller product and inventory APIs, public catalog discovery, and admin category/product moderation.
- Persistent customer cart and wishlist APIs with current price/stock checks.
- Partial/COD refunds, advanced admin controls, and other remaining workflows: not yet implemented.

See [Phase 1 requirements and scope](docs/phase-1-requirements-and-scope.md) for roles, business rules, MVP boundaries, and implementation dependencies.

Authentication design, endpoints, security choices, and completion status are documented in [Phase 4 authentication](docs/phase-4-authentication.md).
Customer profile and address behavior is documented in [Phase 5 customer profile and addresses](docs/phase-5-customer-profile-and-addresses.md).
See the [frontend delivery roadmap](docs/frontend-roadmap.md) for the requested customer, seller, and admin screens and their backend dependencies.
Seller onboarding and role grants are documented in [Phase 6](docs/phase-6-seller-management.md). Catalog, seller product/inventory, and moderation APIs are documented in [Phase 7](docs/phase-7-catalog-and-products.md).
Cart and wishlist behavior is documented in [Phase 8](docs/phase-8-cart-and-wishlist.md).
Checkout and customer orders are documented in [Phase 9](docs/phase-9-checkout-and-orders.md); provider payments and seller fulfillment are documented in [Phase 10](docs/phase-10-payments-and-fulfillment.md).
Refunds, admin queues, and seller/platform reports are documented in [Phase 11](docs/phase-11-admin-refunds-and-reports.md).

Architecture and data model decisions are tracked in [Phase 2 architecture](docs/phase-2-architecture.md) and [Phase 3 database design](docs/phase-3-database-design.md).

## Backend

Java 21 is required. Maven is bootstrapped by the wrapper; Maven itself does not need to be installed globally. See [backend setup](backend/README.md) for MySQL environment variables and commands.

```powershell
cd backend
./mvnw.cmd test
```

The frontend has build and lint scripts. It does not yet define a separate frontend test script.

## Run the frontend

Requirements: Node.js and npm.

```bash
npm install
npm run dev
```

Other available commands:

```bash
npm run build
npm run lint
npm run preview
```

The login and registration pages call the MarketHub authentication API. Configure the backend and `VITE_API_BASE_URL` before using them.

## Planned technology

- **Frontend:** React, JavaScript, React Router, Redux Toolkit, Axios, Bootstrap
- **Backend:** Java 21, Spring Boot, Spring MVC, Spring Security, Spring Data JPA, Hibernate
- **Database:** MySQL
- **Payments:** Razorpay test mode
- **Later, when justified:** Redis, Kafka, Docker, and cloud deployment

## Planned project areas

```text
frontend (this repository)   React customer, seller, and admin applications
backend                      Spring Boot modular monolith
docs                         Requirements, architecture, API, and decision records
```

The implementation roadmap is phase-based. Each module will be designed and reviewed before dependent workflows are added.
