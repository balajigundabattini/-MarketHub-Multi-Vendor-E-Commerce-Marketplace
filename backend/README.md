# MarketHub backend

Java 21 and Spring Boot API for the MarketHub modular monolith. Implemented endpoints include health checks and customer registration, login, refresh, and logout.

## Prerequisites

- Java 21
- MySQL 8.x

Maven is provided by the checked-in Maven Wrapper, so a global Maven installation is not required. The wrapper downloads Maven 3.10.1 on first use.

## Configure the database

Create a local MySQL database named `markethub`, then set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the shell before starting the application. Configure an SMTP relay (for local development, Mailpit or MailHog can listen on port 1025) to deliver verification and reset links. Generate a random signing key as well. Spring Boot does not load `.env` files automatically.

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/markethub'
$env:DB_USERNAME = 'markethub'
$env:DB_PASSWORD = 'your-local-password'
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
$env:FRONTEND_ORIGIN = 'http://localhost:5173'
$env:REFRESH_COOKIE_SECURE = 'false' # localhost only; use true over HTTPS
$env:SMTP_HOST = 'localhost'
$env:SMTP_PORT = '1025'
$env:SMTP_AUTH = 'false'
$env:SMTP_STARTTLS = 'false'
```

Flyway applies versioned SQL migrations. Hibernate uses `ddl-auto=validate`; it checks mapped entities against the migration-managed schema and does not create or mutate tables.

## Run

From this directory in PowerShell:

```powershell
./mvnw.cmd spring-boot:run
```

Health endpoint: `GET http://localhost:8080/api/v1/health`  
Actuator health: `GET http://localhost:8080/actuator/health`

Authentication routes are `POST /api/v1/auth/register`, `/verify-email`, `/resend-verification`, `/login`, `/refresh`, `/logout`, `/forgot-password`, and `/reset-password`. Registration accepts `displayName`, `email`, and `password` and always creates a pending customer account. Access tokens are returned in JSON for in-memory frontend use after verification and login; the rotating refresh token is an HttpOnly cookie. Refresh and logout use the signed `XSRF-TOKEN` double-submit cookie/header. Action tokens are single-use and stored as hashes.

## Run tests

```powershell
./mvnw.cmd test
```

The automated backend suite includes health, JWT, and CSRF-token unit tests. Running against MySQL additionally requires a configured local database; Flyway migrations and Hibernate schema validation run on application startup.
