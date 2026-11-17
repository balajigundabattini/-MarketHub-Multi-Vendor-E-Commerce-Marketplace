# Phase 9: Checkout, coupons, and customer orders

## Implemented

- `POST /api/v1/checkout/quote` calculates totals from current cart, catalog prices, stock, delivery address, and coupon rules. Quotes expire after ten minutes and are tied to a customer, address, and cart fingerprint.
- `POST /api/v1/checkout/orders` locks products and inventory in ID order, rechecks the quote and coupon, reserves stock, snapshots the delivery address and item details, splits the purchase into seller orders, records status history, and clears the cart in one transaction.
- `GET /api/v1/orders` and `GET /api/v1/orders/{id}` expose only the signed-in customer's orders. Item cancellation and return requests check ownership and current status.
- Admin coupon list/create/update endpoints support the coupon management screen.
- Migration `V4__checkout_orders_and_coupons.sql` adds coupons, quotes, orders, seller orders, items, reservations, request/redemption records, and status histories.

## Pricing and payment boundary

Shipping defaults to 99.00 below 999.00, then free; tax defaults to 0. Configure `markethub.checkout.shipping-flat`, `markethub.checkout.free-shipping-threshold`, and `markethub.checkout.tax-rate` for deployment. Values are decimal amounts and tax rate is a fraction from 0 to 1.

COD orders are confirmed after checkout. RAZORPAY orders remain `PENDING_PAYMENT`; this phase does not create a provider session or accept payment callbacks. Payment API integration must finalize or cancel these orders and commit or release their inventory reservations.

## Verification

The backend unit suite and frontend lint/build passed for this phase. Database migration application and Hibernate schema validation require the configured MySQL database and must be checked in that environment.
