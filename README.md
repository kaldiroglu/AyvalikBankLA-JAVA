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
