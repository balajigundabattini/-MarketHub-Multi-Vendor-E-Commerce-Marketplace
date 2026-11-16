# MarketHub backend

Java 21 and Spring Boot API for the MarketHub modular monolith. The initial API exposes a health endpoint; business modules will be added phase by phase.

## Prerequisites

- Java 21
- MySQL 8.x

Maven is provided by the checked-in Maven Wrapper, so a global Maven installation is not required. The wrapper downloads Maven 3.10.1 on first use.

## Configure the database

Create a local MySQL database named `markethub`, then set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the shell before starting the application. `backend/.env.example` documents the variable names; Spring Boot does not load that file automatically.

Flyway applies versioned SQL migrations. Hibernate uses `ddl-auto=validate`; it checks mapped entities against the migration-managed schema and does not create or mutate tables.

## Run

From this directory in PowerShell:

```powershell
./mvnw.cmd spring-boot:run
```

Health endpoint: `GET http://localhost:8080/api/v1/health`  
Actuator health: `GET http://localhost:8080/actuator/health`

## Run tests

```powershell
./mvnw.cmd test
```
