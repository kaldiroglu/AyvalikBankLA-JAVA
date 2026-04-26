# AyvalikBankHA1 vs AyvalikBankLA1 — Architecture Comparison

> **Important note:** Both projects implement exactly the same banking application. They expose identical REST APIs (15 endpoints, 2 roles), share the same technology stack (Java 21, Spring Boot 3.4, Spring Security, Spring Data JPA, PostgreSQL, H2 for tests, JaCoCo, JUnit 5 / Mockito / AssertJ), and pass equivalent functional requirements. The only thing that differs is how the internal structure is organized and what design decisions were made to achieve that functionality.

---

## 1. Architecture

### AyvalikBankHA1 — Hexagonal Architecture (Ports & Adapters)

The application is organized in three concentric rings. The **domain** sits at the centre with no knowledge of the outside world: it contains the rich model classes (`Account`, `Customer`, `Money`, `Password`), value objects (`AccountId`, `Money`), two pure domain services, and two families of port interfaces. The **application** ring holds the use case orchestrators (`AccountApplicationService`, `CustomerApplicationService`), which implement named input port interfaces and depend only on output port interfaces. The **adapter** ring is the outermost layer and is divided into incoming adapters (three REST controllers and the exception handler) and outgoing adapters (four JPA persistence adapters, one security adapter, and their private JPA entity / mapper classes).

```
domain/
  model/            Account (rich), Customer, Money (value object), AccountId, CustomerId, …
  port/in/          15 use case interfaces (DepositMoneyUseCase, TransferMoneyUseCase, …)
  port/out/         5 repository/service ports (AccountRepositoryPort, PasswordHasherPort, …)
  service/          PasswordValidationService, TransferDomainService  ← no Spring, no JPA

application/
  service/          AccountApplicationService, CustomerApplicationService
  exception/        7 typed exceptions

adapter/
  in/web/           AccountController, AdminController, CustomerController, GlobalExceptionHandler
  out/persistence/  4 adapters + 5 JPA @Entity classes + 3 mappers + 4 Spring Data repos
  out/security/     BCryptPasswordHasherAdapter

config/             SecurityConfig, BankUserDetailsService, AdminDataInitializer
```

Production source files: **~77**

### AyvalikBankLA1 — Classic 3-Tier Layered Architecture

The application is divided into three horizontal layers stacked on top of each other. The **presentation layer** (web/controller) receives HTTP requests and delegates directly to **service layer** classes. Services call **Spring Data JPA repositories** directly. The same `@Entity` objects (`Account`, `Customer`, `Transaction`) travel unchanged through all three layers.

```
web/
  controller/       AccountController, AdminController, CustomerController
  dto/request/      6 request record DTOs
  dto/response/     4 response record DTOs (with static from(Entity) factory methods)
  GlobalExceptionHandler

service/            AccountService, CustomerService, PasswordValidationService, TransferService

repository/         AccountRepository, CustomerRepository, SettingsRepository, TransactionRepository

model/              Account (@Entity, anemic), Customer, Transaction, PasswordHistory, Settings, enums

exception/          7 typed exceptions

config/             SecurityConfig, BankUserDetailsService, AdminDataInitializer
```

Production source files: **~41**

### Side-by-Side Summary

| Aspect | HA1 (Hexagonal) | LA1 (Layered) |
|---|---|---|
| Organizing principle | Concentric rings; domain at centre | Horizontal layers; presentation on top |
| Dependency direction | Always inward, never outward | Strictly downward: web → service → repo |
| Number of production files | ~77 | ~41 |
| Framework annotations in domain | None | `@Entity`, `@Table`, `@Column` on every model |
| Boilerplate for one new operation | New port interface + service method + controller handler | New service method + controller handler |

---

## 2. Cohesion and Coupling

### At the Class Level

**HA1** achieves high cohesion because each class has exactly one reason to change.

- `Account` (domain model) knows only about account business rules: how to freeze, unfreeze, close, deposit, and withdraw. It does not know about databases or HTTP.
- `AccountApplicationService` knows only about use case orchestration: load → delegate to domain → persist → return. It does not contain business logic (business logic is in `Account`).
- `AccountPersistenceAdapter` knows only about translating between domain objects and JPA entities. It does not contain business rules or orchestration.
- `AccountPersistenceMapper` knows only about field-by-field conversion. It does not query the database or apply rules.

Each class is narrow and focused. Adding a new business rule (e.g., "accounts may not be frozen on weekdays") changes only the `Account` class; no other class needs to be touched.

**LA1** has lower cohesion in the service layer. `AccountService` (176 lines) is responsible simultaneously for: loading entities by ID and throwing the right exception, validating account status, validating currency, performing balance arithmetic, constructing `Transaction` objects manually, persisting both accounts and transactions, reading the fee setting from the database, delegating fee calculation, and persisting the settings update. When any of these concerns changes, `AccountService` changes — it has multiple reasons to change.

The model classes (`Account`, `Customer`, `Transaction`) are extremely cohesive — they contain only fields and getters/setters — but that is because all their responsibility has been moved elsewhere rather than because the responsibility was well-identified.

### At the Layer Level

**HA1** enforces layer isolation with port interfaces. The web adapter cannot call the persistence adapter directly; it must go through the domain port. The domain cannot call any adapter at all. This is a structural guarantee enforced by the Java type system: `AccountController` holds references to use case interfaces, not to any concrete service or repository class.

**LA1** enforces layering by convention only. Nothing in the Java type system prevents a controller from calling a repository directly. The architecture relies on developer discipline to maintain the downward-only rule. In a small team or well-reviewed codebase this works, but the convention is invisible and enforced only by code review rather than the compiler.

| Coupling metric | HA1 | LA1 |
|---|---|---|
| Controller depends on | Use case interfaces (abstract) | Concrete service classes |
| Service depends on | Port interfaces (abstract) | Concrete Spring Data repositories |
| Cross-layer call prevention | Enforced by the type system | Enforced by convention only |
| Domain model carries framework annotations | No | Yes (`@Entity`, `@Column`, etc.) |
| Number of inter-class coupling paths | Higher (more classes) but each connection is narrow | Lower class count but each service is a wide hub |

---

## 3. Dependency

### Dependency Inversion

HA1 applies the Dependency Inversion Principle throughout. Both input ports and output ports are abstractions owned by the **domain**. Adapters depend on domain abstractions; domain never depends on adapters. This is expressed clearly in the import statements: `AccountApplicationService` imports `AccountRepositoryPort` (a domain interface); `AccountPersistenceAdapter` imports `AccountRepositoryPort` and implements it. The arrow points inward on both sides.

LA1 does not apply DIP. `AccountService` has six concrete dependencies: `AccountRepository`, `CustomerRepository`, `TransactionRepository`, `SettingsRepository`, `TransferService`, and it knows about Spring Security's `PasswordEncoder` through `CustomerService`. These are all concrete classes or framework-provided interfaces. The dependencies point downward uniformly.

### External Technology Dependencies

HA1's domain and application rings are completely free of framework dependencies. `Account`, `Money`, `TransferDomainService`, and `PasswordValidationService` have no Spring, no JPA, no Jakarta imports. The technology choices (JPA, BCrypt, Spring MVC) live exclusively in the adapter ring.

LA1's model classes carry JPA annotations (`@Entity`, `@Table`, `@Column`, `@Enumerated`, `@OneToMany`, etc.). The service classes import Spring's `@Transactional` and `@Service`. Technology choices have penetrated to the innermost classes.

**Practical consequence:** In HA1, replacing PostgreSQL/JPA with MongoDB requires changing only the four persistence adapters and their private JPA entity / mapper files. No service method and no domain class needs to be touched. In LA1, replacing JPA would require rewriting every service method that builds entities by calling setters, every repository interface, and every model class annotation.

### Dependency Count per Key Class

| Class | HA1 direct dependencies | LA1 equivalent | LA1 direct dependencies |
|---|---|---|---|
| AccountController | 7 use case interfaces | AccountController | 1 (AccountService) |
| AccountApplicationService | 4 port interfaces + 1 domain service | AccountService | 4 repositories + 1 service |
| Persistence/Account | AccountRepositoryPort (interface) | AccountRepository | Spring Data JpaRepository (framework) |

HA1 AccountController's seven dependencies look like more coupling, but each dependency is narrow (one operation per interface) and points to an abstraction. LA1 AccountController's single dependency on `AccountService` looks simpler, but that one reference reaches a class of 176 lines that knows about every account operation.

---

## 4. Maintainability

### Ease of Understanding

**LA1 wins for new readers.** A developer who has never seen this codebase before can navigate it within minutes by following the pattern every Java Spring Boot tutorial demonstrates: find the controller, see what service it calls, find the service method, see what repository it calls. There is no need to understand port interfaces, adapters, or concentric ring thinking. The mental model required is "three horizontal layers stacked on top of each other."

**HA1 requires upfront investment.** A new developer must first understand the hexagonal architecture pattern, then locate where the entry point is (an input port interface), then find which concrete class implements it (the application service), then understand why domain objects and JPA entities are separate, then find the mappers. The codebase rewards this investment with clarity at scale but the initial friction is real.

### Ease of Change

**HA1 wins for isolated changes.**

- Adding a new REST endpoint: create a new input port interface → implement it in the application service → add a controller handler. No existing class is modified; each change is additive.
- Replacing the persistence layer: touch only the adapter/out/persistence package. Domain and application rings are unaffected.
- Adding a new input channel (e.g., a CLI or a message queue consumer): create a new incoming adapter that implements the existing input port interfaces. No service code changes.
- Writing a new business rule on accounts: modify only the `Account` domain class and its tests.

**LA1 changes are more concentrated but create wider ripples.**

- Adding a new account operation: add one service method and one controller handler. Straightforward, but the service class grows by another 20-30 lines each time.
- Changing persistence: requires touching all service classes because they import repository interfaces and build entity objects directly.
- Changing a field on the `Account` entity: this class is used at every layer simultaneously, so a field rename can ripple from the database schema through the service logic and up to the DTO factory method in one change.

### File Count and Navigation

HA1 has ~77 production source files versus LA1's ~41. The same functionality requires nearly twice the number of files in the hexagonal approach. Most of the additional files are port interfaces (20 files), JPA entity classes (5 files), persistence mappers (3 files), and persistence adapters (5 files) — all boilerplate that exists to decouple layers. This is a real ongoing cost: each new feature adds more files than the equivalent LA1 change would.

---

## 5. Testability

### What Can Be Tested Without Infrastructure

This is where the architectural difference is most visible.

**HA1 provides a pure domain test suite with no mocks and no infrastructure:**

- `AccountTest` (199 lines): tests freeze, unfreeze, close, deposit, withdraw, transfer logic entirely by instantiating `Account` and calling its methods. No Mockito, no Spring, no database.
- `MoneyTest` (63 lines): tests arithmetic, currency mismatch, negative amount rules by instantiating `Money` records directly.
- `CustomerTest` (50 lines): tests password history rotation by calling domain methods on a `Customer` instance directly.
- `PasswordValidationServiceTest` / `TransferDomainService` tests: instantiate the service with `new`, pass values in, assert results. No mocks at all.

These tests are the fastest possible tests: they run in milliseconds, require no test containers, no application context, and no Mockito framework involvement.

**LA1 cannot have equivalent tests** because the domain model contains no business logic. `Account` is a pure data holder. There is nothing to test on it — its only methods are getters and setters. Business logic lives in `AccountService`, which always requires mocked repositories.

### Service-Level Tests

Both projects test their service classes with Mockito mocks.

**HA1** mocks port interfaces (`AccountRepositoryPort`, `CustomerRepositoryPort`). These interfaces are owned by the domain and are narrow (4 methods on `AccountRepositoryPort`). A test that mocks `AccountRepositoryPort` cannot accidentally call an unrelated Spring Data method.

**LA1** mocks Spring Data repository interfaces (`AccountRepository`, `CustomerRepository`). These extend `JpaRepository` and inherit a large surface area. In practice, only a few methods are used in each test, but the mock object presents the full `JpaRepository` API. Additionally, test helpers must manually construct `Account` objects with 5 setter calls each time (`setId`, `setOwnerId`, `setCurrency`, `setBalance`, `setStatus`) because the entity has no factory method.

### Controller-Level Tests

Both projects use `@WebMvcTest` with `@MockitoBean` for controller tests. The patterns are nearly identical at this level. One difference: HA1 controllers depend on use case interfaces, so the `@MockitoBean` annotations name the specific use case (e.g., `@MockitoBean DepositMoneyUseCase depositMoney`). LA1 controllers depend on one service class, so tests declare a single `@MockitoBean AccountService accountService`. The LA1 approach requires fewer mock declarations, but the single mock serves as the entry point for all account operations rather than separating them.

### Integration and E2E Tests

| Test type | HA1 | LA1 |
|---|---|---|
| Domain model unit tests (no mocks) | Yes — 3 test classes, ~310 lines | Not applicable (anemic model) |
| Domain service unit tests (no mocks) | Yes | Yes (PasswordValidationService, TransferService) |
| Service unit tests (Mockito mocks) | Yes | Yes |
| Repository integration tests (@DataJpaTest) | No (hidden behind adapters) | Yes — 2 test classes |
| E2E tests (@SpringBootTest) | No | Yes — 2 test classes, ~306 lines |
| Controller tests (@WebMvcTest) | Yes | Yes |

LA1 requires `@DataJpaTest` repository tests and `@SpringBootTest` E2E tests because there is no other way to verify that the integration between the service, the repository, and the database schema works correctly. In HA1, the persistence adapter is an implementation detail verified only by the domain tests and service tests; the adapter itself is small and straightforward enough that an E2E test adds little additional confidence.

### Coverage

Despite these structural differences, both projects achieve similar instruction coverage (~87%). LA1's branch coverage (73%) lags behind its line coverage (90%) by 17 points because all conditional logic lives inside `AccountService` methods that require Mockito setup to reach alternative paths, and not all combinations were tested. HA1's domain branch coverage benefits from pure unit tests that trivially reach every `if` branch in `Account` without any test infrastructure.

---

## Summary Table

| Criterion | HA1 (Hexagonal) | LA1 (Layered) |
|---|---|---|
| **Architecture pattern** | Ports & Adapters, inside-out | 3-Tier, top-down |
| **Domain model** | Rich (business methods, value objects) | Anemic (getters/setters only) |
| **Business logic location** | Domain model + domain services | Service classes |
| **Class cohesion** | High — one responsibility per class | Mixed — services are wide |
| **Layer coupling enforcement** | Type system (interfaces) | Developer convention |
| **Technology in domain** | None | JPA annotations on every entity |
| **Production file count** | ~77 | ~41 |
| **Onboarding difficulty** | Higher (pattern knowledge required) | Low (standard Spring Boot) |
| **Adding a new operation** | More boilerplate (port + service + handler) | Less boilerplate (service + handler) |
| **Changing persistence technology** | Change adapters only | Change services and models |
| **Unit-testable without mocks** | Domain model + domain services | Domain services only |
| **Test types required** | Controller + service + domain unit | Controller + service + repository + E2E |
| **Branch coverage (JaCoCo)** | Higher (domain tests reach all branches) | 73% (service paths harder to reach) |

---

## Recent Feature Additions — Side by Side

The two projects share the same use cases, but each renders new behavior in its own architectural style. The features below were added to both:

| Feature | HA1 expression | LA1 expression |
|---|---|---|
| **Account types** (CHECKING / SAVINGS / TIME_DEPOSIT) | Sealed `Account` hierarchy: each subclass overrides `deposit`/`withdraw`/`transferOut`; behavior dispatches polymorphically | Single anemic `Account` with a `type` discriminator + nullable type-specific columns; `AccountService` does the dispatch with `if (type == ...)` branches |
| **Overdraft on checking** | `CheckingAccount.withdraw` enforces the overdraft floor itself | `if (account.getType() == CHECKING) { ... balance + overdraft check ... }` in `AccountService.withdraw` |
| **Savings monthly accrual** | `SavingsAccount.accrueInterest(YearMonth)` is a method on the entity | `AccountService.accrueInterest(accountId, YearMonth)` is a service method |
| **Time deposit maturation** | `TimeDepositAccount.mature(LocalDate)` lives on the entity | `AccountService.matureTimeDeposit(accountId)` lives on the service |
| **Account status invariants** | **State pattern**: `AccountState` sealed interface + `ActiveState`/`FrozenState`/`ClosedState` singletons; `Account.freeze()` is `state = state.freeze()` | `if (status != ACTIVE) throw ...` in every mutating service method |
| **Customer tiers — fee multiplier** | `TransferDomainService.calculateFee(..., CustomerTier)` scales by `tier.feeMultiplier()` | Same signature on `TransferService.calculateFee` — both projects converge here |
| **Customer tiers — per-transaction caps** | `TransferDomainService.requireTransferWithinLimit(...)` throws domain `IllegalStateException`, service rewraps as `LimitExceededException` (HTTP 422) | `TransferService.requireTransferWithinLimit(...)` throws `LimitExceededException` directly; service propagates |
| **Defensive `data.sql` migration** | `ALTER TABLE ... ADD COLUMN IF NOT EXISTS ... NOT NULL DEFAULT '...'` for all new columns | Identical — both ran into the same Hibernate-`ddl-auto`-on-populated-table problem |
| **JaCoCo on Java 25 mocks** | 0.8.12 crashes on Mockito-generated classes for class-file major-v69 → bumped to 0.8.13 | Same — bumped to 0.8.13 as part of the tier feature |

The pattern across all of these: behavior that lives **on the entity** in HA1 lives **in the fat service** in LA1. The REST surface (endpoints, request/response DTOs) and persistence schema are identical; the architectural realization is mirror-opposite.
