# MarketHub - Multi-Vendor E-Commerce Marketplace

MarketHub is a portfolio project for a multi-vendor marketplace. Customers will discover and buy products from multiple sellers; sellers will manage their own stores and fulfillment; administrators will oversee seller approval and marketplace operations.

Project repository: [balajigundabattini/-MarketHub-Multi-Vendor-E-Commerce-Marketplace](https://github.com/balajigundabattini/-MarketHub-Multi-Vendor-E-Commerce-Marketplace)

The project is being built in phases as a modular monolith: a React frontend, a Java 21 / Spring Boot REST backend, and MySQL. The frontend shell and initial backend are in place, and customer registration/login with JWT access and rotating refresh tokens is implemented. Marketplace catalog and order workflows are still upcoming.

## Current status

- Project identity and initial requirements/scope are established.
- Frontend scaffold: React, Vite, JavaScript, React Router, Redux Toolkit, Axios, and Bootstrap.
- Backend scaffold: Java 21, Spring Boot 4.1.2, Maven Wrapper, Actuator, and a health endpoint.
- Initial identity/catalog/cart MySQL migration: drafted; it has not yet been applied to a local database.
- Authentication API: customer registration with email verification, login, refresh rotation, logout, password recovery, JWT resource-server validation, and frontend session restoration.
- Customer profile and address API: authenticated profile updates and owner-scoped delivery address management with default selection and soft deletion.
- Product workflows, cart/checkout, orders, payments, and seller/admin features: not yet implemented.

See [Phase 1 requirements and scope](docs/phase-1-requirements-and-scope.md) for roles, business rules, MVP boundaries, and implementation dependencies.

Authentication design, endpoints, security choices, and completion status are documented in [Phase 4 authentication](docs/phase-4-authentication.md).
Customer profile and address behavior is documented in [Phase 5 customer profile and addresses](docs/phase-5-customer-profile-and-addresses.md).
See the [frontend delivery roadmap](docs/frontend-roadmap.md) for the requested customer, seller, and admin screens and their backend dependencies.

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
