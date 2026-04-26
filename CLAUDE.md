# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Always work in smaller pieces when developing code.

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
model/               → @Entity classes (anemic) + enums (AccountStatus, AccountType, CustomerTier, Currency, TransactionType)
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
- **Account types via discriminator**: `Account` is a single anemic entity with a `type` column (`CHECKING`, `SAVINGS`, `TIME_DEPOSIT`) and nullable type-specific columns (`overdraftLimit`, `interestRate`, `lastAccrualDate`, `principal`, `openedOn`, `maturityDate`, `matured`). `AccountService` branches on `type` with `if/else` — deliberately no sealed hierarchy and no State pattern (those would contradict the anemic+fat-service style).
- **Customer tiers via enum policy data**: `CustomerTier` (`STANDARD`, `PREMIUM`, `PRIVATE`) carries `feeMultiplier()` (1.0× / 0.5× / 0.0×) and per-transaction caps (`maxPerTransfer`, `maxPerWithdrawal`; null = unlimited for `PRIVATE`). `TransferService.calculateFee` scales the admin's fee by the source customer's tier; `requireTransferWithinLimit` / `requireWithdrawalWithinLimit` throw `LimitExceededException` (HTTP 422) when caps are exceeded.
- **Defensive `data.sql` migration**: `ALTER TABLE ... ADD COLUMN IF NOT EXISTS ... NOT NULL DEFAULT '...'` for `accounts.type`, `accounts.overdraft_limit`, and `customers.tier` — Hibernate `ddl-auto=update` cannot add a `NOT NULL` column to a populated table, so we add it ourselves with a default that PostgreSQL applies atomically.

## REST API Summary

| Method | Path | Role | Purpose |
|--------|------|------|---------|
| POST | `/api/admin/customers` | ADMIN | Create customer |
| DELETE | `/api/admin/customers/{id}` | ADMIN | Delete customer |
| GET | `/api/admin/customers` | ADMIN | List all customers |
| PUT | `/api/admin/customers/{id}/tier` | ADMIN | Change customer tier (STANDARD / PREMIUM / PRIVATE) |
| PUT | `/api/admin/settings/transfer-fee` | ADMIN | Set transfer fee % |
| PUT | `/api/admin/accounts/{id}/freeze` | ADMIN | Freeze account |
| PUT | `/api/admin/accounts/{id}/unfreeze` | ADMIN | Unfreeze account |
| PUT | `/api/admin/accounts/{id}/close` | ADMIN | Close account (terminal) |
| PUT | `/api/admin/accounts/{id}/accrue-interest` | ADMIN | Credit monthly interest to a savings account |
| PUT | `/api/admin/accounts/{id}/mature` | ADMIN | Mature a time deposit and credit accrued interest |
| PUT | `/api/customers/{id}/password` | CUSTOMER | Change password |
| POST | `/api/accounts/checking?ownerId=` | CUSTOMER | Open checking account (with optional overdraft) |
| POST | `/api/accounts/savings?ownerId=` | CUSTOMER | Open savings account (with annual interest rate) |
| POST | `/api/accounts/time-deposit?ownerId=` | CUSTOMER | Open time deposit (principal locked until maturity) |
| GET | `/api/customers/{id}/accounts` | CUSTOMER | List accounts |
| GET | `/api/accounts/{id}/balance` | CUSTOMER | Get balance |
| POST | `/api/accounts/{id}/deposit` | CUSTOMER | Deposit |
| POST | `/api/accounts/{id}/withdraw` | CUSTOMER | Withdraw |
| POST | `/api/accounts/{id}/transfer` | CUSTOMER | Transfer to another account |
| GET | `/api/accounts/{id}/transactions` | CUSTOMER | Transaction history |

## Default Admin

Email: `admin@ayvalikbank.dev` / Password: `Admin@123!` (seeded by `AdminDataInitializer` on first startup)
