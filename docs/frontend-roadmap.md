# Frontend delivery roadmap

This tracks the React screens requested in the MarketHub project brief. Screens become functional as their backend contracts are implemented; they should not use hardcoded commerce data to imitate completed workflows.

## Implemented

- Public home, customer registration/login, email verification, password recovery, and session restoration.
- In-memory token handling with a shared auth context, sign-out, and a role-aware protected-route component.
- Customer account page for profile and delivery address management, connected to the API.
- Shared responsive shell and API client.

## Customer application

- [x] Home, login, registration, profile, address book.
- [ ] Product listing, category navigation, search, sorting, filters, pagination, and compare.
- [ ] Product detail and image gallery.
- [ ] Cart and wishlist.
- [ ] Checkout, address selection, coupon entry, and payment status.
- [ ] Order history, seller suborders, tracking, cancellation, and returns.
- [ ] Purchase-verified reviews and customer notifications.

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

Build and verify matching backend contracts before wiring each commerce screen. The next data-backed frontend milestone is product listing/details and discovery after category/catalog APIs are available; checkout depends on cart, inventory, address, and pricing APIs, while seller and admin dashboards depend on their respective authorization and management services.
