# Ayvalık Bank LA-1

A banking application built as a learning project to demonstrate **Classic 3-Tier Layered Architecture**.
Companion project to AyvalikBankHA-JAVA (Hexagonal Architecture) — identical use cases, same tech stack, different structure.

For further enquiry please contact Akin Kaldiroglu at akin@kaldiroglu.dev

## Objective

Show how the same banking domain looks when built with layered architecture instead of hexagonal architecture.
Every characteristic anti-pattern of the layered style is intentional: anemic domain model, services calling
repositories directly, JPA entities used as domain objects throughout all layers.

## Tech Stack

| Concern | Technology |
|---------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.4 |
| Persistence | Spring Data JPA + PostgreSQL |
| Security | Spring Security (HTTP Basic Auth) |
| Validation | Jakarta Bean Validation |
| Testing | JUnit 5 · AssertJ · Mockito · MockMvc · H2 |
| Build | Maven |
| Infrastructure | Docker Compose (PostgreSQL) |

## Quick Start

```bash
docker compose up -d                 # PostgreSQL on host port 5438, database ayvalikbank_la_java
mvn clean verify
mvn spring-boot:run
```
It runs at http://localhost:8080/ 

Default admin: `admin@ayvalikbank.dev` / `Admin@123!`

## Domain

Same domain as AyvalikBankHA-JAVA:
- **Admin** — creates/deletes customers, sets transfer fee, changes customer tiers, freezes/unfreezes/closes accounts, accrues savings interest, matures time deposits
- **Customer** — opens accounts (checking / savings / time deposit), deposits, withdraws, transfers, changes password

Key domain rules:
- Accounts have a currency; all operations are currency-matched
- Accounts follow a state machine: `ACTIVE → FROZEN → ACTIVE`, `ACTIVE|FROZEN → CLOSED` (terminal)
- Transfers between accounts of the same customer are free; cross-customer transfers carry an admin-configured fee scaled by the source customer's tier
- Each customer has a tier — `STANDARD`, `PREMIUM`, or `PRIVATE` — that scales their cross-customer fee (1.0× / 0.5× / 0.0×) and caps per-transaction transfers and withdrawals (PRIVATE = unlimited)
- Passwords must meet a strength policy and cannot reuse the last 3 passwords

Three account types are supported, each with its own behavior (handled by `if/else` branches in `AccountService` — preserving the anemic+fat-service style):
- **CHECKING** — general-purpose account with a configurable overdraft limit; withdrawals may take the balance negative up to that limit
- **SAVINGS** — no overdraft; supports monthly interest accrual (admin endpoint `accrueInterest`) at a configurable annual rate; accrual works on ACTIVE and FROZEN accounts
- **TIME_DEPOSIT** — principal locked at opening; deposits are rejected; the account must be matured (admin endpoint `mature`) on or after the maturity date, which credits the full annual interest; withdrawals are only permitted after maturity

## Documentation

| Document | Contents |
|----------|---------|
| [Architecture.md](Architecture.md) | Layer-by-layer breakdown and contrast with HA1 |
| [Flows.md](Flows.md) | Sequence diagrams for each use case |
| [Tests.md](Tests.md) | Test pyramid, per-class test tables, testing style analysis |

## Ports across the six repos

The six Ayvalık Bank implementations are meant to be compared side by side, so every one
publishes PostgreSQL on its own host port.

| Repo | App | PostgreSQL | Database |
|---|---|---|---|
| `AyvalikBankHA-JAVA` | 8080 | **5437** | `ayvalikbank_ha_java` |
| `AyvalikBankLA-JAVA` | 8080 | **5438** | `ayvalikbank_la_java` |
| `AyvalikBankHA-NET` | 5080 | **5434** | `ayvalikbank_ha_net` |
| `AyvalikBankLA-NET` | 5050 | **5433** | `ayvalikbank_la_net` |
| `AyvalikBankHA-Python` | 8000 | **5436** | `ayvalikbank` |
| `AyvalikBankLA-Python` | 8000 | **5435** | `ayvalikbank` |

- **PostgreSQL ports are all distinct**, so all six databases can run at the same time.
  **5432 is deliberately left free** for a native PostgreSQL install (Postgres.app, Homebrew) —
  a container bound to it would collide, and an application pointed at it would silently
  connect to the native server instead of its own container.
- **Application ports are not all distinct.** Repos sharing a language fall back to the same
  framework default — 8080 for Spring Boot, 8000 for uvicorn. To run two of the same language
  at once, pass an explicit port: `--server.port=8081`, `--port 8001`, or
  `--urls http://localhost:5081`.
- `AyvalikBankHA-NET` has no `launchSettings.json`, so 5080 is the convention used in these
  docs and must be passed with `--urls`. Without it Kestrel binds its own default, 5000.
