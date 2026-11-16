# MarketHub - Multi-Vendor E-Commerce Marketplace

MarketHub is a portfolio project for a multi-vendor marketplace. Customers will discover and buy products from multiple sellers; sellers will manage their own stores and fulfillment; administrators will oversee seller approval and marketplace operations.

Project repository: [balajigundabattini/-MarketHub-Multi-Vendor-E-Commerce-Marketplace](https://github.com/balajigundabattini/-MarketHub-Multi-Vendor-E-Commerce-Marketplace)

The project is being built in phases as a modular monolith: a React frontend, a Java 21 / Spring Boot REST backend, and MySQL. This repository currently contains the frontend starter and project planning document. Marketplace workflows and the backend are not implemented yet.

## Current status

- Project identity and initial requirements/scope are established.
- Frontend scaffold: React, Vite, JavaScript, React Router, Redux Toolkit, Axios, and Bootstrap.
- Backend, database schema, and marketplace features: planned, not yet implemented.

See [Phase 1 requirements and scope](docs/phase-1-requirements-and-scope.md) for roles, business rules, MVP boundaries, and implementation dependencies.

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

The frontend does not yet have a working MarketHub API. Authentication pages from the original starter are scaffolding only and will be replaced as the API and security design are implemented.

## Planned technology

- **Frontend:** React, JavaScript, React Router, Redux Toolkit, Axios, Bootstrap
- **Backend:** Java 21, Spring Boot, Spring MVC, Spring Security, Spring Data JPA, Hibernate
- **Database:** MySQL
- **Payments:** Razorpay test mode
- **Later, when justified:** Redis, Kafka, Docker, and cloud deployment

## Planned project areas

```text
frontend (this repository)   React customer, seller, and admin applications
backend                      Spring Boot modular monolith (to be added)
docs                         Requirements, architecture, API, and decision records
```

The implementation roadmap is phase-based. Each module will be designed and reviewed before dependent workflows are added.
