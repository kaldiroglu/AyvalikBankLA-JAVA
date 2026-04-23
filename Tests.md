# Tests — Ayvalık Bank LA-1

**87 tests across 11 test classes — all passing.**

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
              │  Controller / Web  (43 tests)                    │
              │  @WebMvcTest + MockMvc + @MockitoBean            │
              │  AdminControllerTest    (19)                     │
              │  AccountControllerTest  (17)                     │
              │  CustomerControllerTest  (7)                     │
              └──────────────────────────────────────────────────┘
     ┌────────────────────────────────────────────────────────────────┐
     │  Service Unit  (18 tests)                                      │
     │  Mockito (@ExtendWith(MockitoExtension.class))                 │
     │  AccountServiceTest   (12)                                     │
     │  CustomerServiceTest   (6)                                     │
     └────────────────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────────────────┐
│  Repository Integration  (7 tests)                                       │
│  @DataJpaTest + H2                                                       │
│  CustomerRepositoryTest  (4)                                             │
│  AccountRepositoryTest   (3)                                             │
└──────────────────────────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────────────────┐
│  Pure Unit  (11 tests)                                                   │
│  Plain JUnit 5 — no Spring context, no mocks                             │
│  PasswordValidationServiceTest  (8)                                      │
│  TransferServiceTest            (3)                                      │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Per-Class Test Table

| Class | Type | Tool | Focus | Tests |
|-------|------|------|-------|------:|
| `PasswordValidationServiceTest` | Pure unit | JUnit 5 + AssertJ | Password strength rules: length, upper, lower, digit, special, null | 8 |
| `TransferServiceTest` | Pure unit | JUnit 5 + AssertJ | Fee calculation: same-customer free, cross-customer %, zero fee rate | 3 |
| `CustomerServiceTest` | Service unit | Mockito | createCustomer (happy + weak password), deleteCustomer (happy + not found), changePassword (happy + reuse) | 6 |
| `AccountServiceTest` | Service unit | Mockito | createAccount, deposit, withdraw (insufficient funds), transfer (same-customer free + cross-customer fee), freeze/unfreeze/close (happy + invalid transitions) | 12 |
| `AdminControllerTest` | Web | @WebMvcTest | createCustomer (201, missing name, invalid email, 403, 401), deleteCustomer (204, 404), listCustomers (200 with list, empty), setTransferFee (200, negative, >100), freeze/unfreeze/close (200, 422, 403) | 19 |
| `CustomerControllerTest` | Web | @WebMvcTest | changePassword (200, blank password, weak password → 400, reuse → 409, unknown customer → 404, admin role → 403, no credentials → 401) | 7 |
| `AccountControllerTest` | Web | @WebMvcTest | createAccount (201, missing currency, admin role → 403), listAccounts (200), getBalance (200, 404), deposit (201, negative → 400, 404), withdraw (201, insufficient → 422), transfer (200, missing target → 400, insufficient → 422), getTransactions (200, 404, 401) | 17 |
| `CustomerRepositoryTest` | Integration | @DataJpaTest + H2 | findByEmail (found, not found), cascade delete of PasswordHistory, unique email constraint | 4 |
| `AccountRepositoryTest` | Integration | @DataJpaTest + H2 | findByOwnerId (returns matching only), empty list for unknown owner, enum fields persisted correctly | 3 |
| `CustomerE2ETest` | E2E | @SpringBootTest + H2 | createCustomer full HTTP stack, deleteCustomer with DB assertion, listCustomers, 401 without credentials, 403 customer accessing admin endpoint | 5 |
| `AccountE2ETest` | E2E | @SpringBootTest + H2 | createAccount + deposit + balance check with DB assertion, freeze + unfreeze with DB assertion, cross-customer transfer with fee applied | 3 |
| **Total** | | | | **87** |

---

## Exception-to-HTTP Mapping

`GlobalExceptionHandler` (`@RestControllerAdvice`) defines these 9 mappings:

| Exception | HTTP Status | Typical Cause |
|-----------|-------------|---------------|
| `CustomerNotFoundException` | 404 Not Found | Customer UUID not in database |
| `AccountNotFoundException` | 404 Not Found | Account UUID not in database |
| `AccountNotOperableException` | 422 Unprocessable Entity | Invalid state-machine transition (e.g., freeze a FROZEN account) |
| `InsufficientFundsException` | 422 Unprocessable Entity | Withdraw or transfer exceeds balance |
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
