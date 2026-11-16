# Phase 5: Customer profile and delivery addresses

## Business workflow

Customers maintain contact details and more than one delivery destination. Checkout will later use a selected saved address; this module stores address records and the customer's default preference. Addresses are deactivated on removal so existing orders can keep referring to the original destination.

## API routes

| Method and path | Purpose |
|---|---|
| `GET /api/v1/customer/profile` | Return name, email, phone, and birth date for the signed-in customer |
| `PUT /api/v1/customer/profile` | Update editable profile fields; email is read-only |
| `GET /api/v1/customer/addresses` | List active addresses, default first |
| `POST /api/v1/customer/addresses` | Add an address; first active address becomes default automatically |
| `PUT /api/v1/customer/addresses/{id}` | Replace editable address fields |
| `POST /api/v1/customer/addresses/{id}/default` | Select an owned active address as default |
| `DELETE /api/v1/customer/addresses/{id}` | Soft-delete an owned address |

Profile update example:

```json
{
  "displayName": "A Buyer",
  "phone": "+91 98765 43210",
  "birthDate": "1995-07-15"
}
```

Address create/update example:

```json
{
  "recipientName": "A Buyer",
  "phone": "+91 98765 43210",
  "line1": "10 Example Street",
  "line2": "Apartment 4B",
  "city": "Hyderabad",
  "region": "Telangana",
  "postalCode": "500001",
  "countryCode": "IN",
  "label": "Home",
  "isDefault": true
}
```

## Ownership and persistence rules

- Every route requires a valid JWT with the `CUSTOMER` role.
- Controllers take the user ID only from the verified JWT `sub` claim. Address queries include both the target address ID and authenticated user ID; an address owned by someone else returns `404`.
- The account email is read-only here; changing it requires a separate verification workflow.
- The first active address becomes the default. Setting another address as default clears the previous default while holding a pessimistic lock on the customer row, so concurrent updates for one account serialize.
- Deleting an address clears its default flag and sets `is_active = false` instead of removing the database row.
- The existing V1 migration already defines `customer_profiles` and `addresses`; this phase adds JPA mappings and API behavior without a schema migration.

## Verification and remaining integration

The React account page at `/account` edits profile details and supports adding, editing, choosing a default, and removing saved addresses. Backend unit tests include an owner-scoped address lookup check. Frontend lint and production build pass. Full repository, entity mapping, and MySQL validation require a configured local MySQL database.
