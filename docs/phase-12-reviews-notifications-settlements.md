# Phase 12: reviews, notifications, and settlements

## Delivered

- Customer reviews require an owned, delivered order item and can be submitted once per item. Reviews remain pending until an admin approves or rejects them. Approved reviews appear on product detail pages.
- Customers can page through their notifications and mark one or all read. Order placement, payment capture/failure, delivery, return decisions, and review moderation create notifications.
- Sellers can mark shipped orders delivered, decide pending return requests, and record receipt of approved returns. Recording receipt restocks the returned quantity; online item refunds are handled separately by an admin.
- Admins can create settlement records for an approved seller and a bounded date range, then record a payout reference. Sellers see their own settlement history.
- Settlement amounts include delivered, paid orders only, reject mixed currencies, exclude refunded orders, and prevent overlapping periods for the same seller.

## API additions

- `POST /api/v1/products/{productId}/reviews`
- `GET /api/v1/catalog/products/{productId}/reviews`
- `GET /api/v1/notifications`, `PUT /api/v1/notifications/{id}/read`, `PUT /api/v1/notifications/read-all`
- `GET /api/v1/seller/settlements`
- `GET|POST /api/v1/admin/settlements`, `PATCH /api/v1/admin/settlements/{id}/paid`
- `GET|PATCH /api/v1/admin/reviews`

## Migration

`V7__reviews_notifications_and_settlements.sql` creates review, notification, and settlement tables. Apply it through the configured Flyway deployment before using these endpoints.

## Remaining operational limits

- Settlement records are payout ledger entries; this release does not initiate bank transfers.
- COD collection reconciliation and COD refunds remain unimplemented. Item-level online refunds are available through the admin refund workflow.
- Frontend Vitest/React Testing Library coverage currently checks protected-route behavior and cart interactions; broader form and shopping-flow coverage remains future work.
