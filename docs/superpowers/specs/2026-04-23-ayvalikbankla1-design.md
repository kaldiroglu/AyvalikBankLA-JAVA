# Design Spec: AyvalikBankLA1

**Date:** 2026-04-23
**Author:** Claude Code (brainstorming session)
**Companion project:** AyvalikBankHA1 (Hexagonal Architecture)

---

## 1. Objective

Build AyvalikBankLA1 — a banking application with **identical use cases, endpoints, security, and database schema** as AyvalikBankHA1, but implemented in **Classic 3-Tier Layered Architecture** instead of Hexagonal Architecture.

The goal is a side-by-side comparison of the two styles. Every anti-pattern of the layered approach is intentional:
- Anemic domain model (JPA entities with only getters/setters)
- Services talk directly to Spring Data JPA repositories (no port interfaces)
- Controllers talk directly to services (no use-case interfaces)
- JPA entities are used as domain objects throughout all layers (no separate domain model, no mappers)

---

## 2. Tech Stack

| Concern | Technology |
|---------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.4 |
| Persistence | Spring Data JPA + PostgreSQL (prod) / H2 (tests) |
| Security | Spring Security (HTTP Basic Auth) |
| Validation | Jakarta Bean Validation |
| Testing | JUnit 5 · AssertJ · Mockito · MockMvc · H2 |
| Build | Maven |
| Infrastructure | Docker Compose (PostgreSQL) |

---

## 3. Architecture

### Pattern: Classic 3-Tier Layered Architecture

```
┌──────────────────────────────────────────────────────┐
│  PRESENTATION LAYER  (web/)                          │
│  Controllers call services directly                  │
│  Request/Response DTOs · GlobalExceptionHandler      │
└──────────────────────────┬───────────────────────────┘
                           │ direct dependency
┌──────────────────────────▼───────────────────────────┐
│  SERVICE LAYER  (service/)                           │
│  ALL business logic lives here                       │
│  Status guards, balance checks, fee calculation,     │
│  password history rotation, reuse validation         │
└──────────────────────────┬───────────────────────────┘
                           │ direct dependency
┌──────────────────────────▼───────────────────────────┐
│  REPOSITORY / DATA LAYER  (repository/ + model/)    │
│  Spring Data JPA repositories                        │
│  @Entity classes used as domain objects — no mappers │
└──────────────────────────────────────────────────────┘
```

### Contrast with AyvalikBankHA1

| Concern | HA1 (Hexagonal) | LA1 (Layered) |
|---------|-----------------|---------------|
| Controller depends on | Use-case interface | Service class directly |
| Service depends on | Port-out interface | Spring Data repo directly |
| Domain model | Rich entities (behavior + data) | Anemic @Entity beans (data only) |
| Business logic location | Domain entities + domain services | Service layer only |
| Port abstractions | 15 use-case + 5 repo-out interfaces | None |
| Mappers | Persistence mappers per entity | None — entity IS the object |
| Testability strategy | Ports enable isolation | Mockito at constructor injection |

---

## 4. Package Structure

```
dev.kaldiroglu.layered.ayvalikbank
├── AyvalikBankApplication.java
│
├── web/
│   ├── controller/
│   │   ├── AdminController.java
│   │   ├── CustomerController.java
│   │   └── AccountController.java
│   ├── dto/
│   │   ├── request/
│   │   │   ├── ChangePasswordRequest.java
│   │   │   ├── CreateAccountRequest.java
│   │   │   ├── CreateCustomerRequest.java
│   │   │   ├── MoneyOperationRequest.java
│   │   │   ├── SetTransferFeeRequest.java
│   │   │   └── TransferRequest.java
│   │   └── response/
│   │       ├── AccountResponse.java      (static from(Account))
│   │       ├── BalanceResponse.java
│   │       ├── CustomerResponse.java     (static from(Customer))
│   │       └── TransactionResponse.java  (static from(Transaction))
│   └── GlobalExceptionHandler.java
│
├── service/
│   ├── CustomerService.java
│   ├── AccountService.java
│   ├── PasswordValidationService.java
│   └── TransferService.java
│
├── repository/
│   ├── CustomerRepository.java    (JpaRepository<Customer, UUID>)
│   ├── AccountRepository.java     (JpaRepository<Account, UUID>)
│   ├── TransactionRepository.java
│   └── SettingsRepository.java
│
├── model/
│   ├── Customer.java        (@Entity — anemic)
│   ├── Account.java         (@Entity — anemic)
│   ├── Transaction.java     (@Entity — anemic)
│   ├── Settings.java        (@Entity — anemic)
│   ├── PasswordHistory.java (@Entity — anemic)
│   ├── AccountStatus.java   (enum: ACTIVE, FROZEN, CLOSED)
│   ├── Currency.java        (enum: USD, EUR, TRY)
│   └── TransactionType.java (enum: DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT)
│
├── exception/
│   ├── AccountNotFoundException.java
│   ├── AccountNotOperableException.java
│   ├── CustomerNotFoundException.java
│   ├── InsufficientFundsException.java
│   ├── InvalidPasswordException.java
│   ├── PasswordReusedException.java
│   └── UnauthorizedAccessException.java
│
└── config/
    ├── SecurityConfig.java
    ├── BankUserDetailsService.java
    └── AdminDataInitializer.java
```

---

## 5. Domain Model (Anemic @Entity Classes)

All five JPA entities live in `model/` and are used across all layers. They have fields and getters/setters only — no business methods.

### Customer
```
UUID id (PK)
String name
String email (unique)
String role  ("ADMIN" | "CUSTOMER")
String currentPassword   (BCrypt hash)
List<PasswordHistory> passwordHistory  (@OneToMany, EAGER, cascade ALL)
```

### Account
```
UUID id (PK)
UUID ownerId (FK → Customer)
Currency currency          (@Enumerated(EnumType.STRING))
BigDecimal balance
AccountStatus status       (@Enumerated(EnumType.STRING): ACTIVE | FROZEN | CLOSED)
```

### Transaction
```
UUID id (PK)
UUID accountId (FK → Account)
TransactionType type       (@Enumerated(EnumType.STRING))
BigDecimal amount
Currency currency          (@Enumerated(EnumType.STRING))
LocalDateTime createdAt
String description
```

### PasswordHistory
```
UUID id (PK)
Customer customer (@ManyToOne)
String hashedPassword
int position             (0 = most recent, ascending)
```

### Settings
```
String key (PK)
String value
```
Seeded with `('TRANSFER_FEE_PERCENT', '1.0')`.

---

## 6. Service Layer — Business Logic

All logic that HA1 encoded in rich domain entities moves here.

### CustomerService
| Method | Logic |
|--------|-------|
| `createCustomer` | Validate password format (via PasswordValidationService), hash, save Customer entity |
| `deleteCustomer` | Check exists, delete (cascade removes PasswordHistory) |
| `listCustomers` | Return all customers |
| `changePassword` | Validate format, load customer, check reuse against history (BCrypt.matches), hash, rotate history, save |

### AccountService
| Method | Logic |
|--------|-------|
| `createAccount` | Check owner exists, create Account entity (balance=0, status=ACTIVE), save |
| `deposit` | Load account, check status=ACTIVE (else `AccountNotOperableException`), check currency match, `balance += amount`, create Transaction, save both |
| `withdraw` | Load account, check status=ACTIVE (else `AccountNotOperableException`), check currency match, check `balance >= amount` (else `InsufficientFundsException`), `balance -= amount`, create Transaction, save both |
| `transfer` | Load source + target, check both ACTIVE (else `AccountNotOperableException`), check currency match, call TransferService.calculateFee(), check `source.balance >= amount+fee` (else `InsufficientFundsException`), deduct from source, credit target, create 2 Transactions, save all |
| `freezeAccount` | Load account, check `status == ACTIVE` (else `AccountNotOperableException`), set `status = FROZEN`, save |
| `unfreezeAccount` | Load account, check `status == FROZEN` (else `AccountNotOperableException`), set `status = ACTIVE`, save |
| `closeAccount` | Load account, check `status != CLOSED` (else `AccountNotOperableException`), set `status = CLOSED`, save |
| `getBalance` | Load account, return `balance` + `currency` |
| `getTransactions` | Check account exists, return transactions by accountId |
| `listAccounts` | Check customer exists, return accounts by ownerId |

### PasswordValidationService
Identical rules to HA1: 8–16 chars, upper + lower + digit + special character.

### TransferService
`calculateFee(amount, sameCustomer, feePercent)` — returns 0 if same customer, else `amount * feePercent / 100`.

---

## 7. REST API

Identical to HA1 — same 15 endpoints, same HTTP methods, same paths, same roles.

| Method | Path | Role | Purpose |
|--------|------|------|---------|
| POST | `/api/admin/customers` | ADMIN | Create customer |
| DELETE | `/api/admin/customers/{id}` | ADMIN | Delete customer |
| GET | `/api/admin/customers` | ADMIN | List all customers |
| PUT | `/api/admin/settings/transfer-fee` | ADMIN | Set transfer fee % |
| PUT | `/api/admin/accounts/{id}/freeze` | ADMIN | Freeze account |
| PUT | `/api/admin/accounts/{id}/unfreeze` | ADMIN | Unfreeze account |
| PUT | `/api/admin/accounts/{id}/close` | ADMIN | Close account |
| PUT | `/api/customers/{id}/password` | CUSTOMER | Change password |
| POST | `/api/accounts` | CUSTOMER | Open account |
| GET | `/api/customers/{id}/accounts` | CUSTOMER | List accounts |
| GET | `/api/accounts/{id}/balance` | CUSTOMER | Get balance |
| POST | `/api/accounts/{id}/deposit` | CUSTOMER | Deposit |
| POST | `/api/accounts/{id}/withdraw` | CUSTOMER | Withdraw |
| POST | `/api/accounts/{id}/transfer` | CUSTOMER | Transfer |
| GET | `/api/accounts/{id}/transactions` | CUSTOMER | Transaction history |

Response DTOs use `static from(Entity)` factory methods. No separate mapper classes.

---

## 8. Security

Identical to HA1:
- HTTP Basic Auth, stateless sessions
- `BankUserDetailsService` loads from `customers` table
- `SecurityConfig`: `/api/admin/**` → ROLE_ADMIN, `/api/customers/**` + `/api/accounts/**` → ROLE_CUSTOMER
- BCrypt factor 12
- `AdminDataInitializer` seeds `admin@ayvalikbank.dev` / `Admin@123!` on first startup

---

## 9. Exception Handling

`GlobalExceptionHandler` (@RestControllerAdvice) — identical mapping to HA1:

| Exception | HTTP Status |
|-----------|-------------|
| CustomerNotFoundException | 404 |
| AccountNotFoundException | 404 |
| AccountNotOperableException | 422 |
| InsufficientFundsException | 422 |
| InvalidPasswordException | 400 |
| PasswordReusedException | 409 |
| UnauthorizedAccessException | 403 |
| IllegalArgumentException | 400 |
| MethodArgumentNotValidException | 400 |

---

## 10. Test Strategy

### Test Pyramid

```
                 ┌──────────────────────────────────┐
                 │    E2E Tests (~10)               │  @SpringBootTest + H2
                 │    Full HTTP stack               │
                 └──────────────────────────────────┘
          ┌────────────────────────────────────────────┐
          │    Controller Tests (~43)                  │  @WebMvcTest
          │    MockMvc, mocked services                │
          └────────────────────────────────────────────┘
     ┌──────────────────────────────────────────────────────┐
     │    Service Unit Tests (~25)                          │  Mockito
     │    All business logic — mocked repositories          │
     └──────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────┐
│    Integration Tests (~10)                                   │  @DataJpaTest + H2
│    Repository queries, entity relationships, cascades        │
└──────────────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────┐
│    Domain / Service Unit Tests (~15)                         │  Pure Java
│    PasswordValidationService · TransferService               │
└──────────────────────────────────────────────────────────────┘
```

**Total: ~103 tests across ~12 test classes**

### Test Classes

| Class | Type | Tool | Focus |
|-------|------|------|-------|
| `PasswordValidationServiceTest` | Unit | Pure Java | 8 tests — same rules as HA1 |
| `TransferServiceTest` | Unit | Pure Java | Fee calculation: same-customer free, cross-customer % |
| `CustomerServiceTest` | Unit | Mockito | 6 tests — create, delete, list, changePassword happy/sad paths |
| `AccountServiceTest` | Unit | Mockito | 19 tests — all 10 operations × key success + failure scenarios |
| `AdminControllerTest` | Web | @WebMvcTest | 19 tests — identical HTTP contract to HA1 |
| `CustomerControllerTest` | Web | @WebMvcTest | 7 tests — identical HTTP contract to HA1 |
| `AccountControllerTest` | Web | @WebMvcTest | 17 tests — identical HTTP contract to HA1 |
| `CustomerRepositoryTest` | Integration | @DataJpaTest + H2 | findByEmail, cascade delete of PasswordHistory |
| `AccountRepositoryTest` | Integration | @DataJpaTest + H2 | findByOwnerId, status filtering |
| `CustomerServiceIntegrationTest` | E2E | @SpringBootTest + H2 | Full stack: HTTP → Service → H2 → response |
| `AccountServiceIntegrationTest` | E2E | @SpringBootTest + H2 | Deposit, withdraw, transfer with real DB |

### What the new tiers test

**Integration (@DataJpaTest + H2):**
- Repository custom queries work correctly against a real schema
- `@OneToMany` cascade on `PasswordHistory` fires correctly on delete
- `findByOwnerId` returns accounts filtered by owner

**E2E (@SpringBootTest + MockMvc + H2):**
- Full HTTP request enters Spring, traverses service → repository → H2, returns correct JSON
- Transfer fee is read from the seeded `settings` row
- Status-machine violations return 422 after a complete ACTIVE→FROZEN→FROZEN sequence

---

## 11. Build & Infrastructure

**pom.xml** — same dependencies as HA1 plus H2 (test scope):
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

**docker-compose.yml** — identical to HA1 (PostgreSQL 16, same credentials).

**application.properties** — same as HA1.

**application-test.properties** — H2 in-memory datasource, `ddl-auto=create-drop` for integration/E2E tests.

**data.sql** — seeds `TRANSFER_FEE_PERCENT = 1.0` (same as HA1).

---

## 12. Documentation Files to Create

| File | Contents |
|------|---------|
| `README.md` | Project overview, quick start, domain description |
| `CLAUDE.md` | Commands, package guide, API summary, design decisions |
| `Architecture.md` | Layer-by-layer breakdown, contrast table with HA1 |
| `Flows.md` | Sequence diagrams for each use case |
| `Tests.md` | Test pyramid, per-class tables, testing style analysis |
