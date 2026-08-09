# Comparison 2 — Slide Deck Source

> **How to use:** open Keynote, create a deck with any theme, then for each `## Slide N — Title` below, add a slide with that title and paste the bullets as the body. Each slide is sized to fit a 16:9 frame at default body font. Code blocks and tables are intentional — Keynote will paste them as monospace text.

---

## Slide 1 — Title

**Hexagonal vs. Layered**
Two architectures, one banking domain
Ayvalık Bank HA-1 vs. LA-1
A side-by-side study with emphasis on architectural approach and testing strategy

---

## Slide 2 — Why this comparison exists

- Same use cases, same tech stack (Java 25, Spring Boot 3.4, PostgreSQL)
- Same REST API surface
- Two completely different ways of organizing the code
- Goal: make the trade-offs of each style **visible** rather than abstract
- Every "anti-pattern" in LA-1 is **intentional** — it's the most common shape of real-world Spring Boot apps

---

## Slide 3 — The two approaches at a glance

| | HA-1 (Hexagonal) | LA-1 (Layered) |
|---|---|---|
| **Organizing principle** | Concentric rings — domain at the centre | Horizontal layers — presentation on top |
| **Dependency direction** | Always inward, never outward | Strictly downward: web → service → repo |
| **What's in the centre** | Pure-Java domain model with business logic | Anemic JPA entities — just data |
| **Where business logic lives** | On the entities and in domain services | In the service layer (fat services) |
| **What protects the inside** | Ports (interfaces) at every boundary | Convention only — no compile-time enforcement |
| **Production .java files** | ~95 | ~47 |
| **Total tests** | 176 | 119 |

---

## Slide 4 — HA-1 architectural approach: Ports & Adapters

```
                ┌─────────────────────────────┐
                │   ADAPTERS (outer ring)     │
                │   REST · JPA · BCrypt       │
                └──────────┬──────────────────┘
                           │ depends on
                ┌──────────▼──────────────────┐
                │   PORTS (interfaces)        │
                │   inbound (use cases) +     │
                │   outbound (repos, hashers) │
                └──────────┬──────────────────┘
                           │ depends on
                ┌──────────▼──────────────────┐
                │   APPLICATION SERVICES      │
                │   orchestration, txns       │
                └──────────┬──────────────────┘
                           │ depends on
                ┌──────────▼──────────────────┐
                │   DOMAIN MODEL              │
                │   rich entities, value      │
                │   objects, domain services  │
                │   ZERO framework imports    │
                └─────────────────────────────┘
```

- The arrows always point inward
- The **domain knows nothing** about Spring, JPA, or HTTP
- Adapters can be swapped without touching the centre

---

## Slide 5 — LA-1 architectural approach: Classic 3-Tier

```
            ┌──────────────────────────────────┐
            │   PRESENTATION (web/)            │
            │   Controllers + DTOs + Handler   │
            └──────────────┬───────────────────┘
                           │ direct call
            ┌──────────────▼───────────────────┐
            │   SERVICE (service/)             │
            │   ALL business logic here        │
            │   AccountService is ~290 lines   │
            └──────────────┬───────────────────┘
                           │ direct call
            ┌──────────────▼───────────────────┐
            │   REPOSITORY + MODEL             │
            │   Spring Data JPA interfaces     │
            │   @Entity classes (anemic)       │
            │   used throughout all layers     │
            └──────────────────────────────────┘
```

- Each layer calls the layer below directly — no interfaces
- The **same** `Account` object travels web → service → repo
- Convention enforces the dependency rule, not the type system

---

## Slide 6 — The dependency-rule difference

**HA-1:** every cross-layer dependency is an interface
- `AccountController` depends on 9 use-case interfaces
- `AccountApplicationService` depends on 4 outbound ports + 1 domain service
- `AccountPersistenceAdapter` implements `AccountRepositoryPort`
- Swapping JPA for MongoDB = write a new adapter, change one line in `SecurityConfig`

**LA-1:** every cross-layer dependency is a concrete class
- `AccountController` depends on `AccountService` (the class)
- `AccountService` depends on 4 Spring Data repositories (the interfaces, but they ARE the persistence boundary)
- Swapping JPA for MongoDB = rewrite `AccountService` and the entities

---

## Slide 7 — Domain model: rich vs. anemic

**HA-1: rich entities own their invariants**
```java
public final class CheckingAccount extends Account {
    public Transaction withdraw(Money amount) {
        requireActive();
        requireSameCurrency(amount);
        Money projected = balance.subtract(amount);
        if (projected.amount().compareTo(overdraftLimit.negate().amount()) < 0)
            throw new IllegalArgumentException("Withdrawal exceeds overdraft limit");
        balance = projected;
        return Transaction.create(...);
    }
}
```

**LA-1: anemic entity, all logic in service**
```java
public class Account {
    private BigDecimal balance;
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal b) { this.balance = b; }
    // ... no business methods, ever
}

// in AccountService.withdraw():
if (account.getType() == AccountType.CHECKING) {
    BigDecimal floor = account.getOverdraftLimit().negate();
    if (projected.compareTo(floor) < 0) throw new InsufficientFundsException(...);
}
account.setBalance(projected);
```

---

## Slide 8 — Polymorphism vs. discriminator

**HA-1: sealed Account hierarchy**
- `sealed abstract class Account permits CheckingAccount, SavingsAccount, TimeDepositAccount`
- Each subclass overrides `deposit`, `withdraw`, `transferOut` with its own rules
- Behavior dispatches polymorphically — the JVM picks the right method

**LA-1: single anemic entity with a `type` discriminator**
- One `Account` class with a `type` column and nullable type-specific columns (`overdraftLimit`, `interestRate`, `lastAccrualDate`, `principal`, `openedOn`, `maturityDate`, `matured`)
- `AccountService` does the dispatch by hand:
```java
if (account.getType() == AccountType.CHECKING) { ... overdraft branch ... }
else if (account.getType() == AccountType.TIME_DEPOSIT) { ... locked branch ... }
else { ... savings branch ... }
```

---

## Slide 9 — Account status: State pattern vs. if-statements

**HA-1: State pattern (4 dedicated classes)**
- `AccountState` sealed interface
- `ActiveState`, `FrozenState`, `ClosedState` — stateless singletons
- `Account.freeze()` is one line: `state = state.freeze()`
- Each state owns its valid transitions
- 20 dedicated unit tests for transition tables

**LA-1: if-statements scattered across `AccountService`**
```java
if (account.getStatus() != AccountStatus.ACTIVE)
    throw new AccountNotOperableException(...);
```
- Same check appears in `deposit`, `withdraw`, `transfer`, `accrueInterest`, ...
- No State class — would contradict the anemic+fat-service style
- Status is a string column, behavior is a service-method conditional

---

## Slide 10 — Customer tiers: where both projects converge

| | HA-1 | LA-1 |
|---|---|---|
| Enum lives in | `domain/model/customer/CustomerTier.java` | `model/CustomerTier.java` |
| Carries policy data | ✓ — `feeMultiplier()`, `maxPerTransfer()`, `maxPerWithdrawal()` | ✓ — same methods |
| Fee multiplier table | STANDARD 1.0× / PREMIUM 0.5× / PRIVATE 0.0× | identical |
| Per-transaction caps | STANDARD 5k / PREMIUM 50k transfer 25k withdrawal / PRIVATE unlimited | identical |
| Fee calculated by | `TransferDomainService.calculateFee(amount, sameCustomer, percent, tier)` | `TransferService.calculateFee(...)` — same signature |
| Limit check | `requireTransferWithinLimit / requireWithdrawalWithinLimit` | same names, same shape |
| Service that fetches the tier | `AccountApplicationService.transfer / withdraw` | `AccountService.transfer / withdraw` |

**Key insight:** when both architectures arrive at "policy data on the enum, enforcement in a service", the implementations look almost identical. The architectural divergence shows up where the policy interacts with **state** and **identity** — not where it's just a calculation.

---

## Slide 11 — Production class counts side-by-side

| Layer / concern | HA-1 | LA-1 | Ratio |
|---|---:|---:|---:|
| Domain entities | 6 | 5 | ≈ |
| Domain value objects (records) | 5 | 0 | HA-only |
| Domain enums | 5 | 5 | = |
| State-pattern classes | 4 | 0 | HA-only |
| Domain services | 2 | 2 | = |
| Inbound ports (use cases) | 19 | 0 | HA-only |
| Outbound ports | 5 | 0 | HA-only |
| Application services | 2 | 2 | = |
| JPA entities | 5 | 0 (entity = domain) | LA absorbs |
| Persistence adapters | 4 | 0 | HA-only |
| Persistence mappers | 3 | 0 | HA-only |
| Spring Data interfaces | 4 | 4 | = |
| Controllers | 3 | 3 | = |
| Request DTOs | 11 | 11 | = |
| Response DTOs | 4 | 4 | = |
| Application exceptions | 9 | 8 | ≈ |
| Configuration | 3 | 3 | = |
| Security adapter | 1 | 0 | HA-only |
| **Total .java files** | **~95** | **~47** | **≈ 2×** |

**The 2× ratio is paid as boilerplate in HA-1 and saved as boilerplate in LA-1** — at the cost of LA-1's services growing wide.

---

## Slide 12 — Boilerplate per new operation

**Adding "PUT /api/admin/accounts/{id}/accrue-interest"**

**HA-1 needed 5 new files:**
1. `AccrueInterestUseCase.java` — interface + nested `Command` record
2. `AccrueInterestRequest.java` — Jakarta-validated request DTO
3. `SavingsAccount.accrueInterest()` — new method on the entity
4. `AccountApplicationService.accrueInterest()` — implements the use case
5. `AdminController.accrueInterest()` — HTTP handler

**LA-1 needed 2 new files + 2 method additions:**
1. `AccrueInterestRequest.java` — same DTO
2. `AccountService.accrueInterest()` — new method (touches the existing service)
3. `AdminController.accrueInterest()` — HTTP handler

**Same outcome, different cost.** LA-1 wins on initial speed; HA-1 wins on changeability — the use-case interface gives every consumer a stable contract to mock against.

---

## Slide 13 — Testing strategy: the headline numbers

| Test tier | HA-1 | LA-1 |
|---|---:|---:|
| Pure unit (domain model, value objects, state, enums, domain services) | **96** | 18 |
| Application / service unit (Mockito) | 29 | **32** |
| Web slice (`@WebMvcTest`) | 51 | 48 |
| Repository integration (`@DataJpaTest`) | 0 | **7** |
| End-to-end (`@SpringBootTest` + H2) | 0 | **8** |
| **Total** | **176** | **119** |

- HA-1 dominates pure-unit tests because the rich domain has its own pure-unit surface
- LA-1 needs `@DataJpaTest` and `@SpringBootTest` because nothing else verifies that the schema works
- HA-1 needs neither — the JPA adapter is small and proven through the application service tests

---

## Slide 14 — Pure-unit tests: the rich-domain advantage

**HA-1 has 96 pure-unit tests, no Spring, no mocks, no I/O — milliseconds to run:**

| Test class | Tests | What it tests |
|---|---:|---|
| `AccountTest` | 21 | Status state machine, balance math, currency invariants |
| `CheckingAccountTest` | 4 | Overdraft up to limit, rejection beyond limit |
| `SavingsAccountTest` | 7 | Monthly accrual math, frozen-allowed, double-accrual rejection |
| `TimeDepositAccountTest` | 9 | Principal lock, withdraw-before-maturity rejection, mature math |
| `AccountStateTest` | 20 | Every state's transition table, singleton invariant |
| `MoneyTest` | 9 | Negative balances, currency-mismatch guard, arithmetic |
| `CustomerTest` | 6 | Password history rotation, tier defaulting and change |
| `CustomerTierTest` | 3 | Fee multiplier and caps per tier (policy-data assertions) |
| `TransferDomainServiceTest` | 9 | Fee × tier multiplier, limit check boundaries |
| `PasswordValidationServiceTest` | 8 | Password strength rules |

**LA-1 cannot replicate any of these** at the entity level — there's no behavior on the entities. The closest LA-1 gets is `TransferServiceTest` (10) + `PasswordValidationServiceTest` (8) = **18 pure-unit tests**, all on the two utility services.

---

## Slide 15 — LA-1's compensating tests: integration + E2E

**LA-1 has 7 integration tests + 8 E2E tests that HA-1 doesn't need:**

**Repository integration (`@DataJpaTest` + H2):**
- `CustomerRepositoryTest` (4) — `findByEmail`, cascade delete of `PasswordHistory`, unique-email constraint
- `AccountRepositoryTest` (3) — `findByOwnerId`, enum field persistence

**End-to-end (`@SpringBootTest` + H2 + real `MockMvc`):**
- `CustomerE2ETest` (5) — full HTTP stack for create/delete/list with DB assertion, 401/403 paths
- `AccountE2ETest` (3) — open-deposit-balance flow, freeze/unfreeze with DB assertion, cross-customer transfer with fee applied

**Why LA-1 needs these and HA-1 doesn't:**
- LA-1's `AccountService` depends on `AccountRepository` directly. Mockito-mocked tests pass even if the repository's query method has a bug or the schema drifts.
- HA-1's `AccountApplicationService` depends on `AccountRepositoryPort` (an interface). The same Mockito-mocked test verifies the orchestration; the JPA adapter is a thin pass-through with no logic worth integration-testing in isolation.

---

## Slide 16 — Test redundancy in HA-1, focus in LA-1

**HA-1 asserts the same business rule at three levels of the stack:**

For "withdrawing more than balance fails with insufficient funds":
1. **Entity level** — `CheckingAccountTest.shouldRejectWithdrawalWhenNoOverdraftAndInsufficientFunds` (no mocks, runs in 1ms)
2. **Service level** — `AccountApplicationServiceTest.shouldThrowOnWithdrawExceedingBalance` (Mockito on the repository port)
3. **Web level** — `AccountControllerTest.withdraw_returnsUnprocessableEntityOnInsufficientFunds` (`@WebMvcTest` slice)

**LA-1 asserts it at two levels:**
1. **Service level** — `AccountServiceTest.shouldThrowInsufficientFundsOnWithdrawExceedingBalance` (Mockito on the repository class)
2. **Web level** — `AccountControllerTest.withdraw_returnsUnprocessableEntityOnInsufficientFunds`

The HA-1 redundancy isn't waste — each layer tests the rule against its own concerns:
- Entity test catches the math being wrong
- Service test catches the orchestration being wrong (e.g., forgetting to save)
- Web test catches the HTTP-status mapping being wrong

**LA-1's service test conflates the math + orchestration concerns.** When that test fails, it takes longer to localize the bug.

---

## Slide 17 — Per-feature test mapping (8 features)

| Feature | HA-1 test landings | LA-1 test landings |
|---|---|---|
| Account-type behavior | `CheckingAccountTest` (4), `SavingsAccountTest` (7), `TimeDepositAccountTest` (9) — pure JUnit | `AccountServiceTest` (~12 if/else branches with Mockito) |
| Status state machine | `AccountStateTest` (20) + `AccountTest` status tests | Indirect via `AccountServiceTest` + `AdminControllerTest` |
| Account opening (3 endpoints) | `AccountControllerTest` × 3, `AccountApplicationServiceTest` × 4 | `AccountControllerTest` × 3, `AccountServiceTest` × 4 |
| Savings interest accrual | `SavingsAccountTest` × 4 (math + edge cases), `AccountApplicationServiceTest` × 2, `AdminControllerTest` × 2 | `AccountServiceTest` × 2 (math + non-savings reject), `AdminControllerTest` × 2 |
| Time deposit maturation | `TimeDepositAccountTest` × 6, `AccountApplicationServiceTest` × 2, `AdminControllerTest` × 1 | `AccountServiceTest` × 5, `AdminControllerTest` × 1 |
| Tier fee multiplier | `CustomerTierTest`, `TransferDomainServiceTest`, `AccountApplicationServiceTest`, `AccountControllerTest` | `TransferServiceTest`, `AccountServiceTest`, `AccountControllerTest` |
| Tier per-transaction caps | `TransferDomainServiceTest` × 5, `AccountApplicationServiceTest` × 2 | `TransferServiceTest` × 5, `AccountServiceTest` × 2 |
| Tier change endpoint | `CustomerApplicationServiceTest` × 2, `CustomerTest` × 3, `AdminControllerTest` × 3 | `CustomerServiceTest` × 3, `AdminControllerTest` × 3 |

---

## Slide 18 — Test infrastructure required

| Concern | HA-1 | LA-1 |
|---|---|---|
| Pure unit tests need | JUnit 5 + AssertJ | JUnit 5 + AssertJ |
| Application service tests need | Mockito (on outbound ports) | Mockito (on Spring Data repos) |
| Controller tests need | `@WebMvcTest` + `@MockitoBean` (on use-case interfaces) | `@WebMvcTest` + `@MockitoBean` (on service classes) |
| Repository tests need | — | `@DataJpaTest` + H2 driver |
| End-to-end tests need | — | `@SpringBootTest` + H2 + ObjectMapper for response parsing |
| Test execution time | All 176 tests run in ~6s — most are pure JUnit | All 119 tests run in ~12s — `@SpringBootTest` startup dominates |

**LA-1 pays a wall-clock cost** for the integration coverage — the Spring context boots twice (once for `@DataJpaTest`, once for `@SpringBootTest`). HA-1's slowest tests are the controller slices because `@WebMvcTest` boots a partial Spring context for security wiring.

---

## Slide 19 — What testing reveals about the architectures

**Three observations from the test side-by-side:**

1. **Where assertions live tracks where logic lives.** HA-1 asserts savings accrual math in `SavingsAccountTest` (entity-level pure unit). LA-1 asserts the same math in `AccountServiceTest` (Mockito-backed, requires repo stubs). Same behavior, heavier infrastructure in LA-1.

2. **HA-1 has redundancy LA-1 cannot afford.** Account-type behavior is asserted three times in HA-1 (entity, service, controller). Each layer can be tested in isolation. LA-1 asserts at service + controller; the entity has nothing testable in isolation.

3. **LA-1 needs E2E that HA-1 doesn't.** Because LA-1's service depends on `AccountRepository` (a Spring Data interface), Mockito-mocked tests cannot detect schema or query-method bugs. `@DataJpaTest` and `@SpringBootTest` close that gap. HA-1 hides the schema behind `AccountRepositoryPort` and verifies the contract via the application service tests.

**The 47-test gap (176 vs. 119) is mostly the rich-domain pure-unit advantage.**
**The 15 LA-1 integration/E2E tests are the cost of not having that boundary.**

---

## Slide 20 — When to choose each style

**Choose HA-1 (Hexagonal) when:**
- Domain rules are non-trivial and likely to evolve
- You need to swap or add infrastructure (e.g., Kafka, S3, a second DB)
- Multiple teams own different layers
- The domain is the asset; the framework is incidental
- You're willing to pay for boilerplate to gain testability and isolation

**Choose LA-1 (Layered) when:**
- The domain is mostly CRUD with a thin layer of validation
- The team is small and ships features faster with less ceremony
- Spring Boot conventions are a feature, not a constraint
- You're comfortable with a fat service and integration tests
- Switching frameworks is not a realistic future requirement

---

## Slide 21 — The honest summary

| | HA-1 | LA-1 |
|---|---|---|
| What you write more of | Boilerplate (interfaces, mappers, adapters) | Single fat service methods |
| What you write less of | Test infrastructure (no integration tests needed) | Boilerplate |
| What's easier to test in isolation | Domain logic (no mocks) | HTTP slice |
| What's easier to read end-to-end | A single service method (LA-1) | The chain through ports (HA-1) |
| What's easier to change | Infrastructure (HA-1 — swap an adapter) | A new feature (LA-1 — add to the service) |
| What scales worse with size | Number of files | Service-class width |
| What real-world Spring Boot apps look like | Rare | Common |
| What this learning project demonstrates | The discipline | The default |

**Both are valid.** The choice is a calibration, not a verdict.

---

## Slide 22 — Q&A / further reading

- `Architecture.md` — full LA-1 layer breakdown
- `Comparison.md` — full HA-1 vs LA-1 contrast (longer-form)
- `Tests.md` — per-class test tables for LA-1
- `Flows.md` — sequence diagrams for every use case
- HA-1 mirror docs at `/Users/akin/Development/Claude/AyvalikBankHA-JAVA/`

Source repos:
- `AyvalikBankHA-JAVA` — Hexagonal Architecture
- `AyvalikBankLA-JAVA` — Layered Architecture (this project)
