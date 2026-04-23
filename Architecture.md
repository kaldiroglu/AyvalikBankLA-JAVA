# Architecture — Ayvalık Bank LA-1

## Overview

Ayvalık Bank LA-1 implements the same banking domain as AyvalikBankHA1 but uses **Classic 3-Tier Layered Architecture** instead of Hexagonal Architecture. The goal is a side-by-side comparison that makes the trade-offs of each style concrete and visible.

Every anti-pattern described in this document is **intentional**. The project exists to show what a typical real-world Spring Boot codebase looks like when developers reach for the simplest path — and to contrast it with the deliberate isolation of Hexagonal Architecture.

---

## The 3-Tier Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│  PRESENTATION LAYER  (web/)                                      │
│                                                                  │
│  AdminController       CustomerController      AccountController │
│  ─────────────────     ──────────────────      ──────────────── │
│  POST /admin/customers  PUT /customers/{id}/   POST /accounts    │
│  DELETE /admin/...      password               GET  /accounts/.. │
│  PUT /admin/accounts/                          POST /accounts/.. │
│  freeze|unfreeze|close                                           │
│                                                                  │
│  Request DTOs (Jakarta @Valid)                                   │
│  Response DTOs (static from(Entity) factory methods)            │
│  GlobalExceptionHandler (@RestControllerAdvice)                  │
└───────────────────────────┬──────────────────────────────────────┘
                            │  direct dependency (no interface)
┌───────────────────────────▼──────────────────────────────────────┐
│  SERVICE LAYER  (service/)                                       │
│                                                                  │
│  CustomerService           AccountService                        │
│  ───────────────           ──────────────                        │
│  createCustomer            createAccount                         │
│  deleteCustomer            deposit / withdraw / transfer         │
│  listCustomers             freeze / unfreeze / close             │
│  changePassword            getBalance / getTransactions          │
│                            listAccounts / setTransferFeePercent  │
│                                                                  │
│  PasswordValidationService     TransferService                   │
│  ─────────────────────────     ───────────────                   │
│  validate(password)            calculateFee(amount, sameCustomer,│
│  8-16 chars, U+L+D+S rules     feePercent)                       │
│                                                                  │
│  ALL business logic lives here:                                  │
│  status guards · balance checks · fee calculation                │
│  password strength · history rotation · reuse detection          │
└───────────────────────────┬──────────────────────────────────────┘
                            │  direct dependency (no interface)
┌───────────────────────────▼──────────────────────────────────────┐
│  REPOSITORY / DATA LAYER  (repository/ + model/)                │
│                                                                  │
│  CustomerRepository    AccountRepository                         │
│  TransactionRepository SettingsRepository                        │
│  (Spring Data JPA — JpaRepository<Entity, UUID>)                │
│                                                                  │
│  @Entity classes — anemic, used across all layers:              │
│  Customer · Account · Transaction · Settings · PasswordHistory   │
│  Enums: AccountStatus · Currency · TransactionType              │
└──────────────────────────────────────────────────────────────────┘
```

The arrows show the only allowed direction of dependency: each layer may reference the layer below it, never above it. In practice, this discipline is enforced by convention only — there is no compile-time mechanism preventing a repository from importing a service class.

---

## Layer-by-Layer Breakdown

### Presentation Layer (`web/`)

**Controllers** are thin orchestrators. They parse the incoming HTTP request, delegate to the service layer, convert the result into a response DTO, and return an HTTP response. Controllers never contain business logic — they do not check account statuses, balances, or password rules.

Three controllers handle the full API surface:

- `AdminController` — 7 admin endpoints (create/delete/list customers, set fee, freeze/unfreeze/close accounts)
- `CustomerController` — 1 customer endpoint (change password)
- `AccountController` — 7 account endpoints (open, list, balance, deposit, withdraw, transfer, transactions)

**Request DTOs** are Java records annotated with Jakarta Bean Validation constraints (`@NotBlank`, `@NotNull`, `@DecimalMin`, `@DecimalMax`). Spring MVC validates them before the controller method is entered; any violation short-circuits with a `MethodArgumentNotValidException`.

**Response DTOs** are also Java records. Each carries a `static from(Entity)` factory method that maps an entity to the DTO inline. This is the deliberate anti-pattern: there are no dedicated mapper classes, no mapping framework, and no abstraction between the persistence model and the API contract.

**GlobalExceptionHandler** is a `@RestControllerAdvice` that catches all typed exceptions from the service layer and maps them to structured JSON error responses with the correct HTTP status code. It is the only place where exception-to-HTTP mapping is defined.

### Service Layer (`service/`)

This is where everything interesting happens in a layered architecture. All business logic lives here:

- `CustomerService` — creates customers (hashes passwords, validates format), deletes them, lists them, and handles password changes (validation, reuse check against history, hash rotation)
- `AccountService` — all account operations: opening accounts, deposits, withdrawals, transfers (with fee calculation), state-machine transitions (freeze/unfreeze/close), balance queries, and transaction history
- `PasswordValidationService` — a pure-Java service that enforces the password strength policy (8–16 characters, must contain at least one uppercase, one lowercase, one digit, and one special character)
- `TransferService` — a pure-Java service that calculates transfer fees: zero for same-customer transfers, `amount × feePercent / 100` for cross-customer transfers

Services call Spring Data repositories directly via constructor-injected interfaces. There are no port abstractions and no secondary adapters — the service layer couples directly to JPA.

### Repository / Data Layer (`repository/` + `model/`)

**Repositories** are Spring Data JPA interfaces extending `JpaRepository<Entity, UUID>`. Custom query methods are declared as interface methods and implemented by Spring at runtime:

- `CustomerRepository.findByEmail(String)` — used by `BankUserDetailsService` for authentication
- `AccountRepository.findByOwnerId(UUID)` — returns all accounts belonging to a customer

**Model entities** are annotated with `@Entity` and carry the JPA mapping (`@Id`, `@Column`, `@Enumerated`, `@OneToMany`, `@ManyToOne`). They are plain data containers — no business methods, no validation, no domain behavior. The same entity object that JPA loads from the database is passed all the way up to the controller where it is transformed into a DTO.

This is the clearest expression of the **anemic domain model** anti-pattern. The entity `Account` knows nothing about what it means to freeze an account or validate a deposit currency. That knowledge lives entirely in `AccountService`.

---

## Contrast with AyvalikBankHA1

| Concern | HA1 (Hexagonal) | LA1 (Layered) |
|---------|-----------------|---------------|
| Controller depends on | Use-case interface | Service class directly |
| Service depends on | Port-out interface | Spring Data repo directly |
| Domain model | Rich entities (behavior + data) | Anemic @Entity beans (data only) |
| Business logic location | Domain entities + domain services | Service layer only |
| Port abstractions | 15 use-case + 5 repo-out interfaces | None |
| Mappers | Persistence mappers per entity | None — entity IS the object |
| Testability strategy | Ports enable isolation | Mockito at constructor injection |

The structural consequence of removing all interfaces is that **every layer is coupled to its concrete neighbour**. Swapping the database would require changing `AccountService` because it imports `AccountRepository` directly. In HA1, `AccountService` only imports a port interface; the JPA adapter sits behind that port and can be replaced without touching the domain.

---

## Key Anti-Patterns Explained

### 1. Anemic Domain Model

An anemic domain model is one where the data objects (entities) carry no behavior. All five `@Entity` classes in this project — `Customer`, `Account`, `Transaction`, `Settings`, and `PasswordHistory` — have only fields, getters, and setters.

The consequence: business rules that logically belong to the domain concept are scattered into services. The rule "an account can only be frozen if it is currently ACTIVE" lives in `AccountService.freezeAccount()`, not on the `Account` entity itself. If `Account` had a `freeze()` method, the rule would be co-located with the data it governs, and the service would not need to inspect the status field externally.

### 2. No Port Interfaces

In Hexagonal Architecture, the service layer defines interfaces (ports) for everything it depends on. The JPA repositories, external APIs, and messaging systems are all hidden behind these ports. In LA1, there are no ports. `AccountService` imports `AccountRepository`, `CustomerRepository`, `TransactionRepository`, and `SettingsRepository` directly.

The practical consequence during testing: because there are no interfaces to stub with fake implementations, tests must use **Mockito** to mock the concrete repository classes. This works, but it means that the test is tied to the specific method signatures of `AccountRepository`. Adding or renaming a repository method requires updating tests.

### 3. JPA Entities as Domain Objects

The `Account` entity that JPA loads from the database is the same object that `AccountService` mutates (e.g., `account.setStatus(AccountStatus.FROZEN)`), that `AccountRepository.save(account)` persists, and that `AccountController` passes to `AccountResponse.from(account)`.

This simplicity is appealing, but it creates hidden coupling: the JPA mapping annotations (`@Entity`, `@Column`, `@Enumerated`) are visible throughout the codebase, the entity lifecycle (managed/detached/transient) can cause surprising behaviour in tests, and any change to the database schema forces changes to the object passed around in all layers.

### 4. Business Logic in Services

Because the domain model is anemic, every business rule must live somewhere else. In LA1, that place is the service layer. `CustomerService.changePassword()` contains the full sequence: validate format, load customer, check current password hash, iterate password history for reuse, rotate history (keeping only the last 3), hash the new password, and save.

In a rich domain model (as in HA1), many of these steps would be methods on the `Customer` entity (`customer.changePassword(newPassword, encoder, validator)`), with the orchestration remaining in the service. The difference becomes significant as the codebase grows: a service class that contains all business logic for a domain concept tends to become large and difficult to navigate.

---

## Why These Anti-Patterns Are Common

Despite being labelled anti-patterns, these choices appear in the vast majority of real-world Spring Boot applications. Several forces drive teams toward them:

**Lower initial cognitive load.** An anemic model with a service layer is the default teaching style for Spring. The Spring documentation itself uses this pattern in its guides. Developers who have not been exposed to alternative styles naturally replicate what they have seen.

**Framework alignment.** Spring Data JPA is designed to work with `@Entity` classes. Using those same classes as your domain model requires zero additional code. Introducing a separate domain model requires writing mappers, which feels like unnecessary ceremony when the team is focused on delivering features.

**Faster initial development.** With no port interfaces and no mappers, there are fewer files to create for each new use case. A developer can add a new endpoint in two files (controller + service method) rather than five (controller, use-case interface, use-case implementation, port interface, adapter).

**Tool and framework support.** IDEs, code generators, and many libraries assume the JPA entity is the central object. Spring's `@Valid` can validate a request body directly. Jackson can serialize an entity directly. These conveniences work without any additional structure.

The cost appears later, as the application grows and requirements change. At that point, the tight coupling makes it difficult to test individual components in isolation, to replace infrastructure (e.g., switching from JPA to a document store), or to reuse business logic outside of the HTTP context. AyvalikBankHA1 demonstrates one way to pay that complexity cost upfront in exchange for a more maintainable long-term structure.
