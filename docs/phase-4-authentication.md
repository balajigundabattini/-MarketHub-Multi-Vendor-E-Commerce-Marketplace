# Phase 4: Authentication and account recovery

## Business workflow

Customers register with an email address and password. The account stays `PENDING_VERIFICATION` until the owner follows a time-limited link. Only active accounts can log in. A reset request sends a short-lived, one-time link; a successful reset changes the BCrypt password and revokes all refresh sessions.

Seller onboarding is deliberately separate: public registration cannot select the `SELLER` or `ADMIN` role. A later seller application and administrator approval workflow will grant seller access.

## API

| Method and path | Purpose |
|---|---|
| `POST /api/v1/auth/register` | Create a pending customer and send verification email; returns `202` |
| `POST /api/v1/auth/verify-email` | Verify a one-time action token; returns `204` |
| `POST /api/v1/auth/resend-verification` | Resend verification without confirming whether the address exists; returns `202` |
| `POST /api/v1/auth/login` | Authenticate an active account and issue tokens |
| `POST /api/v1/auth/refresh` | Rotate the refresh token using the refresh and CSRF cookies |
| `POST /api/v1/auth/logout` | Revoke the refresh token and clear cookies |
| `POST /api/v1/auth/forgot-password` | Request a password reset; response does not disclose account existence |
| `POST /api/v1/auth/reset-password` | Change password with a one-time token and revoke refresh sessions |

Registration JSON: `{ "displayName": "A Buyer", "email": "buyer@example.com", "password": "a-long-password" }`.

Verification JSON: `{ "token": "<value from email link>" }`.

Reset request JSON: `{ "email": "buyer@example.com" }`. Reset JSON: `{ "token": "<value from email link>", "password": "a-new-long-password" }`.

## Security and persistence

- Passwords use BCrypt. Passwords must be at least 12 characters and no more than 72 UTF-8 bytes.
- Access tokens are short-lived HS256 JWTs kept in browser memory. Refresh tokens are opaque, rotated, and stored in MySQL as SHA-256 hashes.
- Refresh/logout use a JavaScript-readable signed double-submit CSRF cookie/header pair; the refresh cookie itself is HttpOnly, SameSite Strict, and Secure in HTTPS deployments.
- Verification and recovery tokens are random, single-use, expire after 24 hours and 30 minutes respectively, and are stored only as SHA-256 hashes in `identity_action_tokens` (Flyway V2).
- SMTP connection details are environment variables. Local account flows need a local mail catcher or SMTP server configured at `SMTP_HOST`/`SMTP_PORT`.
- The forgot-password and resend-verification responses do not reveal whether an address is registered. Delivery failures are logged server-side.

## Verification status

Unit tests cover JWT claims, CSRF signing, and action-token hashing, expiry, purpose, and one-time use. Backend and frontend automated suites pass. MySQL migration execution, SMTP delivery, and full registration/login browser integration still require local MySQL and SMTP services to be configured.

## Walkthrough

1. Start MySQL with the `markethub` database and set the database environment variables described in [backend setup](../backend/README.md).
2. Start a local SMTP catcher on the configured host and port.
3. Set a fresh `JWT_SECRET_BASE64`, start the backend and frontend, then register a customer.
4. Open the verification link in the mail catcher, log in, and confirm the request returns a Bearer access token and refresh cookies.
5. Request a password reset, use the emailed link, then log in with the new password. Reusing either action link should be rejected.

## Interview explanation

Registration does not create an authenticated session before ownership of the email address is proven. Random action tokens are shown only to the recipient; persisting their hashes means a database read alone cannot redeem them. The refresh token is rotated on every refresh and families are revoked on reuse. Password recovery revokes persisted refresh sessions after the password changes; any already-issued access token has a maximum remaining lifetime of 15 minutes.
