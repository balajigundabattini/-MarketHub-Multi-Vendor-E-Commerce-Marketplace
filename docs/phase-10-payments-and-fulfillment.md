# Phase 10: Payments and seller fulfillment

## Implemented

- RAZORPAY checkout creates a provider order and a customer-owned payment resource. The frontend opens Razorpay Checkout with the server-issued key and provider order ID.
- Customer callback signatures are verified on the server using the provider order ID stored in the database. A raw-body webhook endpoint validates `X-Razorpay-Signature`; captured and failed payment events update payment state. Captured payment commits the reserved inventory and confirms the parent and seller orders.
- Failed payments can be retried through an authenticated endpoint. Order/payment details are only available to the owning customer.
- Seller order listing, accept/reject, processing, and shipment creation are scoped to the approved seller. Status changes are audited. COD stock is committed at shipment; rejected unpaid orders release their reservations. Paid orders cannot be rejected until a refund flow exists.
- Migration `V5__payments_and_fulfillment.sql` creates payment and shipment tables.

## Configuration

Set `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, and `RAZORPAY_WEBHOOK_SECRET`. Configure the provider webhook to POST raw JSON to `/api/v1/payments/webhook` and subscribe to `payment.captured` and `payment.failed`. `RAZORPAY_BASE_URL` is optional and defaults to Razorpay's API endpoint.

The payment service is unavailable for RAZORPAY orders until the key ID and secret are configured. No provider secrets are stored in the repository. Pending payment reservations expire after 15 minutes and are released by a scheduled sweep. A late verified capture is recorded as `PAYMENT_REVIEW_REQUIRED` for manual fulfillment/refund handling instead of being discarded. Provider behavior still needs an end-to-end test account, and V5 must be applied to the configured MySQL database.
