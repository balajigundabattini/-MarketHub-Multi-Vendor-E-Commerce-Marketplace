# Phase 11: Refunds, administration, and reporting

## Implemented

- Admin order queue pages and filters orders, shows customer/payment state, and exposes a full-refund action only for eligible captured payments.
- Refund requests are recorded before calling Razorpay. Provider `refund.processed` and `refund.failed` webhooks update the refund, payment, order, seller-order, and item states. Repeated webhook deliveries are idempotent. COD and already-refunded orders cannot be refunded through this endpoint.
- Admin customer listing and suspend/reactivate actions are role-scoped and audited. API `SUSPENDED` maps to the identity system's locked status.
- Seller date-range reports return gross sales, commission, estimated net earnings, daily totals, and seller dashboard metrics. New orders snapshot a configurable commission rate (`SELLER_COMMISSION_RATE`, default 10%).
- Admin dashboard and date-range reports return sales, customer/seller queues, moderation queue, commission, and processed refund totals.
- Coupon datetime fields now serialize as UTC instants when saved.
- Migration `V6__refunds_and_reporting.sql` adds refund tracking and admin audit records.

## Limits

The admin refund action is a full refund through Razorpay; partial refunds and COD refunds are not exposed. A refund is not retried automatically after provider failure. Reports currently present totals as INR and assume the marketplace uses INR consistently. The commission rate should be set to the marketplace's actual commercial agreement before launch.

Configure Razorpay webhooks for `payment.captured`, `payment.failed`, `refund.processed`, and `refund.failed`, and set `RAZORPAY_WEBHOOK_SECRET`. The refund endpoint follows Razorpay's API behavior of refunding the captured payment when no amount is supplied ([Razorpay refund API](https://hdfcbank-collectnow-docs.razorpay.com/api/refunds/create-instant/)).
