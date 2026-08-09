# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Always work in smaller pieces when developing code.

## Project

**Ayvalık Bank LA-1** — a layered-architecture banking application in Java 21 / Spring Boot 3.4.

## Cross-repository invariants

This repo is one of six (hexagonal + layered × Java/.NET/Python) that must stay **functionally
identical**. `AyvalikBankContractTests` is one black-box HTTP suite run against all six, and CI runs
it on every push. Before changing any endpoint, status code, field name or JSON shape, check whether
the change belongs in all six.

- Wire format is **camelCase**; validation failures are **400** (not FastAPI's default 422).
- Enums travel as **strings** (`"USD"`), never numbers.
- Refactoring write-ups live in `Refactorings.md`; the Java hexagonal repo is the reference.
- The suite is 29 tests; all six implementations currently pass 29/29.

## Commands

```bash
# Browsable API docs once the app is running: /swagger-ui.html
# Shared contract suite (from AyvalikBankContractTests):
#   BANK_BASE_URL=http://localhost:8081 pytest tests/

# Start local PostgreSQL
docker compose up -d                 # PostgreSQL on host port 5438, database ayvalikbank_la_java

# Build & test
mvn clean verify

# Run a single test class
mvn test -Dtest=AccountServiceTest

# Run the application
mvn spring-boot:run

# Run without Docker (H2 in memory) — see Environment gotchas for why each flag is needed
mvn spring-boot:run -Dspring-boot.run.useTestClasspath=true \
  -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:h2:mem:bank;MODE=PostgreSQL;NON_KEYWORDS=KEY,VALUE \
  --spring.datasource.driver-class-name=org.h2.Driver --spring.jpa.hibernate.ddl-auto=create-drop \
  --spring.sql.init.mode=never"
```

## Environment gotchas

- **Running without Docker:** H2 is a *test-scoped* dependency, so `spring-boot:run` needs
  `-Dspring-boot.run.useTestClasspath=true` or startup fails with `Cannot load driver class:
  org.h2.Driver`. `data.sql` is PostgreSQL-specific — use `--spring.sql.init.mode=never` and let
  Hibernate create the schema; the admin is seeded in code.
- **Docker Desktop** stops on its own; if compose fails with a socket error, `open -a Docker` and wait.
- **Keep the datasource unique to this repo.** Both Java repos once pointed at
  `jdbc:postgresql://localhost:5432/ayvalikbank` — same server *and* same database name — each with
  `ddl-auto=update`. They shared one schema: whichever ran last reshaped it, and each saw the
  other's rows. `ddl-auto=update` only ever adds, so nothing ever failed to reveal it.

## Ports and databases

This repo: app **8081**, PostgreSQL **5438**, database `ayvalikbank_la_java`.

All six repos take distinct application and PostgreSQL ports so every one can run at the same
time; `README.md` carries the full table. **5432 is deliberately unused** — it is the default for
a native PostgreSQL (Postgres.app, Homebrew), and an application pointed at it connects to that
server instead of its own container, with no error to say so. Every compose service sets an
explicit `container_name`: without one Compose derives a name from the directory, and a container
can outlive the checkout that defined it while still holding its port.

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

## Design Decisions (2026-08 hardening pass)

- **Ownership authorization**: every customer-facing service method takes the caller's id, taken from the authenticated principal — never from a route or query parameter. Transfers check the **source only**; the target is deliberately unchecked. Opening an account takes no owner id: the caller is the owner. See `Refactorings.md`.
- **Optimistic locking**: accounts carry a version token. A conflict surfaces at commit and maps to HTTP 409.
- **Three hexagonal refactorings deliberately do not apply here** — `TransactionAmount` (no `Money` value object), actor-shaped ports (layered has no ports) and the domain refusal vocabulary (no domain/application seam to translate across). They are artifacts of the hexagonal boundary; see `Refactorings.md`.

## Default Admin

Email: `admin@ayvalikbank.dev` / Password: `Admin@123!` (seeded by `AdminDataInitializer` on first startup)
