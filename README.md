# Ayvalık Bank LA-1

A banking application built as a learning project to demonstrate **Classic 3-Tier Layered Architecture**.
Companion project to AyvalikBankHA1 (Hexagonal Architecture) — identical use cases, same tech stack, different structure.

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
docker compose up -d
mvn clean verify
mvn spring-boot:run
```

Default admin: `admin@ayvalikbank.dev` / `Admin@123!`

## Domain

Same domain as AyvalikBankHA1:
- **Admin** — creates/deletes customers, sets transfer fee, freezes/unfreezes/closes accounts
- **Customer** — opens accounts, deposits, withdraws, transfers, changes password

Key domain rules:
- Accounts have a currency; all operations are currency-matched
- Accounts follow a state machine: `ACTIVE → FROZEN → ACTIVE`, `ACTIVE|FROZEN → CLOSED` (terminal)
- Transfers between accounts of the same customer are free; cross-customer transfers carry an admin-configured fee
- Passwords must meet a strength policy and cannot reuse the last 3 passwords

## Documentation

| Document | Contents |
|----------|---------|
| [Architecture.md](Architecture.md) | Layer-by-layer breakdown and contrast with HA1 |
| [Flows.md](Flows.md) | Sequence diagrams for each use case |
| [Tests.md](Tests.md) | Test pyramid, per-class test tables, testing style analysis |
