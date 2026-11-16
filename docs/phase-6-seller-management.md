# MarketHub – Phase 6: Seller Onboarding and Store Profile

**Status:** Seller application, admin review, and approved seller store profile APIs implemented  
**Package:** `com.markethub.seller`

## Business workflow

An authenticated customer submits legal and business names. The application starts in `PENDING_REVIEW`. An administrator can approve or reject it once. Approval records the reviewer and decision time, grants the `SELLER` role, and creates an active store in the same database transaction. A seller can read or edit only their own store after approval.

The seller role is granted by the approval service, never from public registration input. Admin and seller endpoint authorization is enforced by Spring Security; each seller store operation also resolves ownership from the authenticated JWT subject.

## API

### Customer application

- `GET /api/v1/seller-applications/me` returns the applicant's status and review data, or `404` when they have not applied.
- `POST /api/v1/seller-applications` accepts `{ "legalName": "...", "businessName": "..." }` and returns `201 Created`.
- Duplicate applications return `409 Conflict`.

### Admin review

- `GET /api/v1/admin/seller-applications?page=0&size=20&status=PENDING_REVIEW` returns a Spring `Page` of applications with applicant name and email.
- `PATCH /api/v1/admin/seller-applications/{id}/decision` accepts `{ "status": "APPROVED", "reason": "..." }` or `REJECTED`.
- Re-reviewing a decided application returns `409`; unsupported decisions return `400`.

### Seller store

- `GET /api/v1/seller/store` returns `{ id, name, slug, description, status }`.
- `PUT /api/v1/seller/store` accepts `{ "name": "...", "description": "..." }`.
- Both operations require an approved seller account and are scoped to the caller.

## Persistence and transaction boundary

`SellerEntity` maps `sellers`; `StoreEntity` maps `stores`. Both reference existing users/sellers through foreign keys already defined in Flyway V1. Seller approval, role grant, and initial store creation run in one `@Transactional` service method. If any write fails, all three changes roll back together.

The decision record uses the existing `reviewed_by`, `reviewed_at`, and `review_note` columns. Store slugs are normalized from the business name and suffixed with the seller ID to avoid collisions.

## Verification

`SellerManagementServiceTest` checks that approval grants a role and creates a store, while rejection does neither. Run all backend tests with `cd backend; .\mvnw.cmd test`.

## Remaining seller work

This phase does not yet implement product listings, image uploads, stock adjustments, seller orders, reports, suspension controls, or store suspension. Those use separate APIs and should be implemented before connecting catalog discovery and checkout to real inventory.
