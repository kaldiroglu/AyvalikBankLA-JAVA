# Tests — Ayvalık Bank LA-1

**119 tests across 11 test classes — all passing.**

---

## Test Pyramid

```
                        ┌────────────────────────────────────┐
                        │  E2E  (8 tests)                    │
                        │  @SpringBootTest + H2              │
                        │  CustomerE2ETest (5)               │
                        │  AccountE2ETest  (3)               │
                        └────────────────────────────────────┘
              ┌──────────────────────────────────────────────────┐
              │  Controller / Web  (48 tests)                    │
              │  @WebMvcTest + MockMvc + @MockitoBean            │
              │  AdminControllerTest    (22)                     │
              │  AccountControllerTest  (19)                     │
              │  CustomerControllerTest  (7)                     │
              └──────────────────────────────────────────────────┘
     ┌────────────────────────────────────────────────────────────────┐
     │  Service Unit  (32 tests)                                      │
     │  Mockito (@ExtendWith(MockitoExtension.class))                 │
     │  AccountServiceTest   (23)                                     │
     │  CustomerServiceTest   (9)                                     │
     └────────────────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────────────────┐
│  Repository Integration  (7 tests)                                       │
│  @DataJpaTest + H2                                                       │
│  CustomerRepositoryTest  (4)                                             │
│  AccountRepositoryTest   (3)                                             │
└──────────────────────────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────────────────┐
│  Pure Unit  (18 tests)                                                   │
│  Plain JUnit 5 — no Spring context, no mocks                             │
│  PasswordValidationServiceTest  (8)                                      │
│  TransferServiceTest           (10)                                      │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Per-Class Test Table

| Class | Type | Tool | Focus | Tests |
|-------|------|------|-------|------:|
| `PasswordValidationServiceTest` | Pure unit | JUnit 5 + AssertJ | Password strength rules: length, upper, lower, digit, special, null | 8 |
| `TransferServiceTest` | Pure unit | JUnit 5 + AssertJ | Fee calculation tier-aware (same-customer free, STANDARD full, PREMIUM half, PRIVATE zero, zero rate); per-transaction limit checks (transfer/withdraw caps, exact-cap boundary, PRIVATE unlimited) | 10 |
| `CustomerServiceTest` | Service unit | Mockito | createCustomer (happy + weak password + default tier STANDARD), deleteCustomer (happy + not found), changePassword (happy + reuse), changeCustomerTier (happy + missing customer) | 9 |
| `AccountServiceTest` | Service unit | Mockito | createCheckingAccount, deposit (happy + on-time-deposit rejection), withdraw (insufficient funds + checking-overdraft happy + checking-overdraft cap + on-unmatured-time-deposit rejection + tier cap), transfer (same-customer free + cross-customer fee + premium half-fee + tier cap + on-time-deposit rejection), freeze/unfreeze/close (happy + invalid transitions), createSavings, createTimeDeposit, accrueInterest (happy + non-savings rejection), matureTimeDeposit (happy + non-time-deposit rejection) | 23 |
| `AdminControllerTest` | Web | @WebMvcTest | createCustomer (201, missing name, invalid email, 403, 401), deleteCustomer (204, 404), listCustomers (200 with list, empty), setTransferFee (200, negative, >100), changeCustomerTier (200, missing tier → 400, customer role → 403), freeze/unfreeze/close (200, 422, 403), accrueInterest (200, 403), matureTimeDeposit (200) | 22 |
| `CustomerControllerTest` | Web | @WebMvcTest | changePassword (200, blank password, weak password → 400, reuse → 409, unknown customer → 404, admin role → 403, no credentials → 401) | 7 |
| `AccountControllerTest` | Web | @WebMvcTest | createCheckingAccount (201, missing currency, admin role → 403), createSavingsAccount (201), createTimeDepositAccount (201), listAccounts (200), getBalance (200, 404), deposit (201, negative → 400, 404), withdraw (201, insufficient → 422), transfer (200, missing target → 400, insufficient → 422), getTransactions (200, 404, 401) | 19 |
| `CustomerRepositoryTest` | Integration | @DataJpaTest + H2 | findByEmail (found, not found), cascade delete of PasswordHistory, unique email constraint | 4 |
| `AccountRepositoryTest` | Integration | @DataJpaTest + H2 | findByOwnerId (returns matching only), empty list for unknown owner, enum fields persisted correctly | 3 |
| `CustomerE2ETest` | E2E | @SpringBootTest + H2 | createCustomer full HTTP stack, deleteCustomer with DB assertion, listCustomers, 401 without credentials, 403 customer accessing admin endpoint | 5 |
| `AccountE2ETest` | E2E | @SpringBootTest + H2 | createAccount + deposit + balance check with DB assertion, freeze + unfreeze with DB assertion, cross-customer transfer with fee applied | 3 |
| **Total** | | | | **119** |

---

## Exception-to-HTTP Mapping

`GlobalExceptionHandler` (`@RestControllerAdvice`) defines these 10 mappings:

| Exception | HTTP Status | Typical Cause |
|-----------|-------------|---------------|
| `CustomerNotFoundException` | 404 Not Found | Customer UUID not in database |
| `AccountNotFoundException` | 404 Not Found | Account UUID not in database |
| `AccountNotOperableException` | 422 Unprocessable Entity | Invalid state-machine transition, deposit on time deposit, withdraw before maturity, accrue on non-savings, mature on non-time-deposit |
| `InsufficientFundsException` | 422 Unprocessable Entity | Withdraw or transfer exceeds balance (or overdraft limit on a checking account) |
| `LimitExceededException` | 422 Unprocessable Entity | Per-transaction transfer or withdrawal exceeds the source customer's tier cap |
| `InvalidPasswordException` | 400 Bad Request | Password fails strength policy |
| `PasswordReusedException` | 409 Conflict | New password matches one of the last 3 |
| `UnauthorizedAccessException` | 403 Forbidden | Customer attempting another customer's operation |
| `IllegalArgumentException` | 400 Bad Request | Currency mismatch on deposit/withdraw/transfer |
| `MethodArgumentNotValidException` | 400 Bad Request | Jakarta @Valid constraint on a request DTO failed |

Response body format for all errors:
```json
{
  "error": "descriptive message"
}
```

---

## Testing Style Analysis

Each test can be labelled by what it asserts:

- **output** — asserts the return value (what comes back from the method / HTTP endpoint)
- **state** — asserts the object's internal state after the call (field values)
- **communication** — asserts that a collaborator was called (Mockito `verify`)

### PasswordValidationServiceTest (pure unit)

| Test | Style |
|------|-------|
| `shouldAcceptValidPassword` | output (no exception) |
| `shouldRejectPasswordOutOfLengthRange` | output (exception type + message) |
| `shouldRejectPasswordWithoutUppercase` | output (exception message) |
| `shouldRejectPasswordWithoutLowercase` | output (exception message) |
| `shouldRejectPasswordWithoutDigit` | output (exception message) |
| `shouldRejectPasswordWithoutSpecialCharacter` | output (exception message) |
| `shouldRejectNullPassword` | output (exception type) |

### TransferServiceTest (pure unit)

| Test | Style |
|------|-------|
| `shouldReturnZeroFeeForSameCustomerTransfer` | output |
| `shouldCalculateFeeForDifferentCustomers` | output |
| `shouldReturnZeroFeeWhenFeePercentIsZero` | output |

### CustomerServiceTest (Mockito)

| Test | Style |
|------|-------|
| `shouldCreateCustomerWithHashedPassword` | state + communication |
| `shouldThrowInvalidPasswordExceptionForWeakPassword` | output + communication (verifyNoInteractions) |
| `shouldDeleteExistingCustomer` | communication |
| `shouldThrowCustomerNotFoundOnDeleteOfMissingCustomer` | output |
| `shouldChangePasswordSuccessfully` | state + communication |
| `shouldThrowPasswordReusedExceptionWhenNewPasswordMatchesCurrent` | output |

### AccountServiceTest (Mockito)

| Test | Style |
|------|-------|
| `shouldCreateAccountForExistingCustomer` | state |
| `shouldThrowCustomerNotFoundWhenOwnerMissing` | output |
| `shouldDepositMoneyToAccount` | state + output |
| `shouldThrowAccountNotFoundOnDepositToMissingAccount` | output |
| `shouldThrowInsufficientFundsOnWithdrawExceedingBalance` | output |
| `shouldTransferBetweenAccountsOfSameCustomerFreeOfCharge` | state |
| `shouldDeductFeeForTransferBetweenDifferentCustomers` | state |
| `shouldFreezeAccount` | state + communication |
| `shouldUnfreezeAccount` | state + communication |
| `shouldCloseAccount` | state + communication |
| `shouldThrowAccountNotOperableWhenFreezingClosedAccount` | output |
| `shouldThrowAccountNotFoundWhenFreezingMissingAccount` | output |

### AdminControllerTest (@WebMvcTest)

All tests assert HTTP status codes (output). Selected tests additionally use `verify` (communication) to confirm service methods were or were not invoked.

### CustomerControllerTest (@WebMvcTest)

All output tests asserting HTTP status codes. Notable: the `changePassword_returnsBadRequestOnBlankPassword` test verifies `verifyNoInteractions(customerService)` — confirming that @Valid short-circuits the controller before reaching the service.

### AccountControllerTest (@WebMvcTest)

Mix of output (HTTP status codes, JSON body assertions) and communication (`verify(accountService).transfer(...)` in the happy-path transfer test).

### Repository Tests (@DataJpaTest)

All state-oriented: insert data, call repository method, assert result. `CustomerRepositoryTest.shouldCascadeDeletePasswordHistoryOnCustomerDelete` is the only integration test that also asserts DB-level cascade behaviour.

### E2E Tests (@SpringBootTest)

All tests are output tests (HTTP status + JSON body) combined with state assertions against the actual repository (`accountRepository.findById(id).orElseThrow().getBalance()`). This combination is what makes E2E tests more expensive but more trustworthy than unit tests alone.

---

## What Makes Testing Layered Architecture Different from HA1

In AyvalikBankHA1, the service layer depends on port interfaces (`AccountRepositoryPort`, `CustomerRepositoryPort`, etc.). Test doubles are plain Java implementations of those interfaces — no mock framework required. The test controls the repository completely through ordinary polymorphism.

In LA1, there are no interfaces. `AccountService` takes `AccountRepository` (a Spring Data interface backed by JPA) in its constructor. To isolate the service from the database in a unit test, we must use **Mockito** to create a mock of the concrete Spring Data interface:

```java
@Mock AccountRepository accountRepository;
// ...
service = new AccountService(accountRepository, customerRepository,
        transactionRepository, settingsRepository, new TransferService());
```

This works because Spring Data repository interfaces are themselves interfaces, which Mockito can proxy. However, it couples the test to the exact method signatures of `AccountRepository`. If the repository method `findById` were replaced with a different lookup strategy, the Mockito `when(accountRepository.findById(...))` stubs in every service test would need to be updated.

In HA1, a port interface rename would only affect the port adapter and the test double — not the service test itself, because the service test interacts with the port interface, not the JPA implementation.

This difference in testability reflects the underlying architectural difference: LA1's layers are coupled to their concrete collaborators, while HA1's hexagon is isolated from its infrastructure by abstract ports.

---

## Code Coverage Report

Generated by **JaCoCo 0.8.13** via `mvn clean verify`. The figures below pre-date the account-types and customer-tiers features (rerun `mvn verify` to refresh). The JaCoCo bump from 0.8.12 to 0.8.13 was required because the older agent crashes on Mockito-generated classes for Java 25 (class file major version 69).

JaCoCo measures six distinct coverage techniques. Each captures a different dimension of how thoroughly the test suite exercises the production code.

---

### Coverage Techniques Explained

| Technique | What it counts | Why it matters |
|-----------|----------------|----------------|
| **Instruction** | Individual JVM bytecode instructions (finest grain) | Catches dead code that survives line-level analysis |
| **Branch** | Taken vs. not-taken paths through boolean decisions (`if`, `else`, `switch`, ternary) | Reveals untested conditions; complementary to line coverage |
| **Line** | Source lines that were executed at least once | Maps directly to what a developer reads; easiest to reason about |
| **Method** | Methods entered at least once (regardless of branches) | Finds entirely untouched methods |
| **Class** | Classes where at least one method was called | High-level indicator of dead classes |
| **Cyclomatic Complexity** | Independent execution paths (McCabe metric); each branch adds 1 | A branch-weighted alternative to counting decisions; correlates with how many test cases are needed |

Instruction and branch coverage are JaCoCo's primary metrics because they operate on the bytecode, not the source — they catch things like compiler-generated branches in `switch` expressions that source-line counting misses.

---

### Project Totals

| Metric | Covered | Missed | Total | Coverage |
|--------|--------:|-------:|------:|---------:|
| Instruction | 1 429 | 220 | 1 649 | **86.7 %** |
| Branch | 44 | 16 | 60 | **73.3 %** |
| Line | 323 | 36 | 359 | **90.0 %** |
| Method | 134 | 20 | 154 | **87.0 %** |
| Class | 36 | 1 | 37 | **97.3 %** |
| Cyclomatic Complexity | 149 | 35 | 184 | **81.0 %** |

The gap between line coverage (90 %) and branch coverage (73 %) is the most informative signal: lines inside `if` blocks execute once along the happy path, but the opposing branch may never be taken. The 17-point gap shows there are conditionals whose failure paths are not directly tested.

---

### Per-Package Summary

| Package | Line | Branch | Method |
|---------|-----:|-------:|-------:|
| `web/controller` | 100.0 % | — | 100.0 % |
| `web/dto/request` | 100.0 % | — | 100.0 % |
| `web/dto/response` | 100.0 % | — | 100.0 % |
| `config` | 100.0 % | 50.0 % | 92.3 % |
| `service` | 85.6 % | 75.0 % | 71.9 % |
| `web` (GlobalExceptionHandler) | 84.6 % | 50.0 % | 83.3 % |
| `exception` | 85.7 % | — | 85.7 % |
| `model` | 90.0 % | — | 89.3 % |
| `repository` | 0.0 % | — | 0.0 % |
| `(root)` (AyvalikBankApplication) | 33.3 % | — | 50.0 % |

**repository = 0 %** is expected and correct: Spring Data JPA repositories are interfaces. The actual implementation is a JPA proxy generated at runtime from bytecode that JaCoCo cannot instrument. The repository behaviour is covered indirectly through service tests (Mockito stubs) and integration tests (@DataJpaTest), but the generated proxy bytecode itself is not in scope.

---

### Per-Class Detail

| Class | Instruction | Branch | Line | Method |
|-------|------------:|-------:|-----:|-------:|
| `AccountController` | 100.0 % | — | 100.0 % | 100.0 % |
| `AdminController` | 100.0 % | — | 100.0 % | 100.0 % |
| `CustomerController` | 100.0 % | — | 100.0 % | 100.0 % |
| `GlobalExceptionHandler` | 82.2 % | 50.0 % | 84.6 % | 83.3 % |
| `SecurityConfig` | 100.0 % | — | 100.0 % | 100.0 % |
| `BankUserDetailsService` | 83.3 % | — | 100.0 % | 66.7 % |
| `AdminDataInitializer` | 100.0 % | 50.0 % | 100.0 % | 100.0 % |
| `PasswordValidationService` | 100.0 % | 100.0 % | 100.0 % | 100.0 % |
| `TransferService` | 100.0 % | 100.0 % | 100.0 % | 100.0 % |
| `CustomerService` | 85.5 % | 100.0 % | 100.0 % | 50.0 % |
| `AccountService` | 70.4 % | 56.3 % | 76.5 % | 77.8 % |
| `Account` | 100.0 % | — | 100.0 % | 100.0 % |
| `Customer` | 92.0 % | — | 92.9 % | 92.3 % |
| `Transaction` | 94.2 % | — | 93.3 % | 93.3 % |
| `Settings` | 82.4 % | — | 80.0 % | 80.0 % |
| `PasswordHistory` | 71.0 % | — | 66.7 % | 66.7 % |
| `AccountStatus` | 100.0 % | — | 100.0 % | 100.0 % |
| `Currency` | 100.0 % | — | 100.0 % | 100.0 % |
| `TransactionType` | 100.0 % | — | 100.0 % | 100.0 % |
| `AccountNotFoundException` | 100.0 % | — | 100.0 % | 100.0 % |
| `AccountNotOperableException` | 100.0 % | — | 100.0 % | 100.0 % |
| `CustomerNotFoundException` | 100.0 % | — | 100.0 % | 100.0 % |
| `InsufficientFundsException` | 100.0 % | — | 100.0 % | 100.0 % |
| `InvalidPasswordException` | 100.0 % | — | 100.0 % | 100.0 % |
| `PasswordReusedException` | 100.0 % | — | 100.0 % | 100.0 % |
| `UnauthorizedAccessException` | 0.0 % | — | 0.0 % | 0.0 % |
| All request/response DTOs | 100.0 % | — | 100.0 % | 100.0 % |
| `AyvalikBankApplication` | 37.5 % | — | 33.3 % | 50.0 % |

---

### Observations

**`AccountService` — lowest branch coverage at 56.3 %**

`AccountService` has the most conditional logic: it guards every operation with status checks (`ACTIVE`, `FROZEN`, `CLOSED`), currency-match checks, and balance checks. The 12 Mockito unit tests cover the most critical paths, but several branches remain untested:

- The currency-mismatch path in `deposit` and `withdraw` (IllegalArgumentException)
- The currency-mismatch paths in both source and target accounts during `transfer`
- The `unfreezeAccount` → `AccountNotOperableException` when status is `CLOSED` (not `FROZEN`)
- The `setTransferFeePercent` fallback `orElseGet` branch (settings key missing)
- `getTransactions` and `listAccounts` are covered by E2E tests but their internal `CustomerNotFoundException` guard is not triggered

The branch metric surfaces these gaps more precisely than the line metric (76.5 % line vs. 56.3 % branch), because the conditional lines themselves execute but the alternative paths do not.

**`UnauthorizedAccessException` — 0 % coverage**

This exception is declared in `GlobalExceptionHandler` and thrown conceptually when a customer accesses another customer's data. However, no controller currently throws it programmatically — the security layer (Spring Security role checks) returns 403 before the controller is entered. `UnauthorizedAccessException` is therefore dead production code: reachable by neither the tests nor the application's normal request path. It should either be removed or wired to an actual authorization check inside the service layer.

**`GlobalExceptionHandler` branch coverage at 50 %**

The handler for `MethodArgumentNotValidException` reduces field errors to a single string using `Stream.reduce`. JaCoCo detects a branch inside the lambda (`a.isEmpty() ? b : a + "; " + b`). Tests exercise the multi-field case through `@Valid` validation failures, but the single-field case (where `a` is empty on the first iteration) takes the other branch. This is a minor gap.

**`BankUserDetailsService` method coverage at 66.7 %**

The class has three methods: the constructor, `loadUserByUsername` (covered by E2E authentication), and the inherited `toString`/`equals`/`hashCode` from `Object` (never called). Method coverage reports these uncalled Object methods as missed.

**`AyvalikBankApplication` — 33.3 % line, 50.0 % method**

The `main()` method is never invoked in tests — Spring Boot tests start via `@SpringBootTest`, not via `main`. This is universal across Spring Boot projects and not a concern.

**`PasswordValidationService` and `TransferService` — 100 % on all metrics**

The two pure-domain services achieve perfect coverage across all six metrics. This is a direct consequence of the TDD approach used during their implementation: tests were written first, and the implementation was driven to make each test pass. These two classes have no dead branches.

---

### Coverage by Technique — Comparison

The six metrics do not simply tell the same story at different resolutions. They capture orthogonal dimensions:

```
Technique              Coverage    What it reveals
──────────────────────────────────────────────────────────────────────
Instruction (bytecode) 86.7 %      Execution at the finest level; closest to what the JVM actually runs
Line (source)          90.0 %      Lines executed; easiest to map to the source editor — slightly optimistic
Method                 87.0 %      Entirely uncalled methods; structurally unused code
Branch (decisions)     73.3 %      Untested conditions — the biggest gap and most actionable metric
Cyclomatic Complexity  81.0 %      Branch-weighted path coverage; inversely related to test adequacy
Class                  97.3 %      Dead classes — only UnauthorizedAccessException is unreached
```

The **branch / line gap of 16.7 percentage points** is the most important number in this table. It means that roughly one in six decision outcomes in the codebase is never exercised. In a banking application, these are the outcomes most likely to contain latent bugs: the `else` arm of a balance check, the fallback when a settings key is missing, the currency-mismatch guard on a transfer. Adding targeted tests for `AccountService`'s untested branches would close most of this gap and bring branch coverage above 85 %.
