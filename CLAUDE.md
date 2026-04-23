# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**Ayvalık Bank LA-1** — a layered-architecture banking application in Java 21 / Spring Boot 3.4.

## Commands

```bash
# Start local PostgreSQL
docker compose up -d

# Build & test
mvn clean verify

# Run a single test class
mvn test -Dtest=AccountServiceTest

# Run the application
mvn spring-boot:run
```

## Architecture

Classic 3-Tier Layered Architecture. Direct dependencies: Controller → Service → Repository.

```
web/controller/      → AdminController, CustomerController, AccountController
web/dto/request/     → 6 request record DTOs with Jakarta Validation
web/dto/response/    → 4 response record DTOs with static from(entity) factory methods
web/                 → GlobalExceptionHandler (@RestControllerAdvice)
service/             → CustomerService, AccountService, PasswordValidationService, TransferService
repository/          → 4 Spring Data JPA repository interfaces
model/               → @Entity classes (anemic) + enums (AccountStatus, Currency, TransactionType)
exception/           → 7 typed RuntimeException subclasses
config/              → SecurityConfig, BankUserDetailsService, AdminDataInitializer
```

## Key Design Decisions (and Anti-Patterns)

- **Anemic model**: `Account`, `Customer`, `Transaction` are @Entity classes with getters/setters only. No business methods.
- **Business logic in services**: status guards, balance checks, fee calculation, password history rotation — all in `AccountService` / `CustomerService`.
- **No port interfaces**: controllers call services directly; services call Spring Data repositories directly.
- **No mappers**: response DTOs have `static from(Entity)` factory methods; no separate mapper classes.
- **JPA entities as domain objects**: the same `Account` object travels from repository → service → controller.
- **PasswordEncoder in service**: `CustomerService` depends directly on Spring Security's `PasswordEncoder`.

## REST API Summary

| Method | Path | Role | Purpose |
|--------|------|------|---------|
| POST | `/api/admin/customers` | ADMIN | Create customer |
| DELETE | `/api/admin/customers/{id}` | ADMIN | Delete customer |
| GET | `/api/admin/customers` | ADMIN | List all customers |
| PUT | `/api/admin/settings/transfer-fee` | ADMIN | Set transfer fee % |
| PUT | `/api/admin/accounts/{id}/freeze` | ADMIN | Freeze account |
| PUT | `/api/admin/accounts/{id}/unfreeze` | ADMIN | Unfreeze account |
| PUT | `/api/admin/accounts/{id}/close` | ADMIN | Close account (terminal) |
| PUT | `/api/customers/{id}/password` | CUSTOMER | Change password |
| POST | `/api/accounts?ownerId=` | CUSTOMER | Open account |
| GET | `/api/customers/{id}/accounts` | CUSTOMER | List accounts |
| GET | `/api/accounts/{id}/balance` | CUSTOMER | Get balance |
| POST | `/api/accounts/{id}/deposit` | CUSTOMER | Deposit |
| POST | `/api/accounts/{id}/withdraw` | CUSTOMER | Withdraw |
| POST | `/api/accounts/{id}/transfer` | CUSTOMER | Transfer to another account |
| GET | `/api/accounts/{id}/transactions` | CUSTOMER | Transaction history |

## Default Admin

Email: `admin@ayvalikbank.dev` / Password: `Admin@123!` (seeded by `AdminDataInitializer` on first startup)
