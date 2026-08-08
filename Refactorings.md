# Refactorings

Claude Opus 5 (1M context) — created 2026-08-08

A log of significant refactorings applied to Ayvalık Bank LA-JAVA. Each entry records what the code
looked like before, what it looks like after, and — most importantly — *why* the change was worth
making.

For further enquiry please contact Akin Kaldiroglu at akin@kaldiroglu.dev

**Relationship to the other implementations.** This repository is one of six: hexagonal and layered,
in Java, .NET and Python. Five refactorings were designed in `AyvalikBankHA-JAVA`; **only two apply
here**, and the reason is the most interesting thing in this file — see *Refactorings that do not
apply* at the end. All six are held to one HTTP contract by `AyvalikBankContractTests`.

---

## Entry 1 — Ownership authorization: a rule that could not be said

**Baseline:** `44dc5d8` · **Commit:** `50cb992`

### The symptom

Any authenticated customer could operate on any other customer's data:

- `PUT /api/customers/{id}/password` took its target from the path, gated only on the CUSTOMER role.
  **Any customer could set any other customer's password, then log in as them.**
- Given an account id, any customer could deposit to it, withdraw from it, transfer out of it, and
  read its balance and full transaction history.
- `createCheckingAccount(@RequestParam UUID ownerId, ...)` let a customer open accounts owned by
  anyone.

### The tell

`UnauthorizedAccessException` existed. `GlobalExceptionHandler` mapped it to 403. **No production
code threw it.**

> **An exception nothing throws is a rule nothing enforces.** Grepping for exception types that
> appear only in a handler and a test is a two-minute audit. Across the six repositories it found
> three real holes.

### The root cause

No service method took the caller:

```java
public Transaction deposit(UUID accountId, BigDecimal amount, Currency currency)
```

There was no caller to compare against, so "the caller must own this account" was not merely
unenforced — it was **inexpressible**. A signature declares what an operation is permitted to
consider; omit something and no amount of care downstream can restore it.

### The change

`BankUserPrincipal` carries the caller's id from authentication, where `BankUserDetailsService`
already loaded the whole customer and discarded everything but email and role. Every customer-facing
service method takes `UUID callerId` first, and `requireOwner` / `requireSelf` enforce the rule.

| Situation | Technique |
|---|---|
| The resource *is* the caller's | **Delete the parameter** — `?ownerId=` is gone; the caller is the owner |
| The path names a customer | **Require self** — path id must equal the caller |
| The path names an account | **Require ownership** — load, compare `ownerId` |

**Prefer deleting a parameter to validating it.** A validated parameter must be validated everywhere,
forever; a deleted one is gone.

### The transfer asymmetry

The caller must own the **source**. The target is deliberately unchecked — sending money to other
people is the entire product. `shouldRejectTransferFromAnotherCustomersAccount` and its permissive
counterpart pin both halves, so the obvious-looking hardening ("the caller must own both") fails
loudly instead of quietly breaking the product.

### The test-fixture cost

`@WithMockUser` builds a plain Spring `User`, under which `@AuthenticationPrincipal
BankUserPrincipal` resolves to `null`. A `@WithBankUser` annotation plus a
`WithSecurityContextFactory` was therefore required, and 26 annotations changed.

> A security fix whose production diff is small can still carry most of its weight in test
> infrastructure. Budget for that.

---

## Entry 2 — Optimistic locking

**Baseline:** `50cb992` · **Commit:** `1eda13d`

### The symptom

| Step | Transaction A | Transaction B |
|---|---|---|
| 1 | read balance 100 | |
| 2 | | read balance 100 |
| 3 | withdraw 50 → 50 in memory | |
| 4 | | withdraw 50 → 50 in memory |
| 5 | save → row = 50 | |
| 6 | | save → row = 50 (overwrites) |

Balance ends at **50** where it should be **0**, and **both** `Transaction` rows are written. Money
is created from nothing and the ledger contradicts the account — the worst failure available to a
bank.

### Why this port was the easy one

In a layered design the service loads the entity through the repository inside its transaction and
mutates that managed instance directly. The version loaded at the start of the transaction is still
the one checked at commit, so **`@Version` alone closes the hole** — no persistence restructuring.

Compare the hexagonal implementations, where an adapter sits between the service and the ORM:

- `AyvalikBankHA-JAVA` rebuilt a **detached** entity on every save, so its version was always null.
  Its write path had to change.
- `AyvalikBankHA-NET` read with `AsNoTracking()`, so its save re-read the current row and compared
  the version against itself. Its read path had to change.

> **An ORM can only protect a row you actually loaded.** The mapping layer that buys the hexagonal
> repos their independence is exactly what put that claim at risk.

### The test needs no threads

Two persistence contexts committing in a fixed order reproduce the bug deterministically — no
sleeps, no races, no flakiness. **A lost update is a stale-read problem, not a timing problem.**
Anyone writing a `Thread.sleep`-and-hope test for this has misdiagnosed it.

`OptimisticLockingFailureException` maps to **409 Conflict**, with a fixed message rather than
Hibernate's, which names the entity class and primary key.

---

## Refactorings that do not apply here — and why

Three of the five refactorings from `AyvalikBankHA-JAVA` were deliberately **not** ported. This is a
result, not an omission.

### `TransactionAmount` (HA entry 1)

`TransactionAmount` wraps a `Money` value object. **This repository has no `Money`** — amounts are
raw `BigDecimal` passed alongside a separate `Currency`:

```java
public Transaction deposit(UUID callerId, UUID accountId, BigDecimal amount, Currency currency)
```

Introducing it would mean first introducing `Money`, which moves the layered design toward the rich
domain model the hexagonal repositories exist to contrast with.

### Actor-shaped ports (HA entry 2)

Layered architecture has no ports. Controllers call services directly. There is nothing to group.

### A refusal vocabulary (HA entry 4)

**Zero catch blocks in `AccountService`. Zero raw `IllegalStateException`. Zero message matching.**

The hexagonal repositories needed this refactoring because the domain and application layers are
separate, and the domain's refusals must be *translated* across that seam. Translating positionally
(Java) or by message text (.NET, Python) is the defect. A layered service throws the mapped exception
directly — there is no seam, so there is no defect.

### The conclusion worth teaching

**Three of the five refactorings are artifacts of the hexagonal boundary.** The layered
implementation is not behind; it is structurally incapable of those three defects, and pays for that
elsewhere — an anemic model, and business logic concentrated in services rather than in the objects
it describes.

That trade is the entire point of maintaining both architectures side by side, and it is far more
visible in what *didn't* need fixing than in what did.

---

## Deliberate non-goals

- **`Customer` has the same lost-update exposure** as `Account`. The same fix applies; left out to
  keep entry 2 reviewable.
- **No retry-on-conflict.** A 409 tells the client to retry; automatic retry is a separate design
  with its own idempotency questions.
- **`changePassword` does not verify the current password.** Defensible under HTTP Basic, where the
  caller has already proven it on the same request; not defensible once sessions arrive.

## Discussion questions

1. Three refactorings did not apply here. For each, name the cost this architecture pays instead.
2. Entry 2 was trivial here and hard in both hexagonal repositories. What does that say about
   mapping layers generally?
3. Entry 1's hole existed in all six implementations. What kind of review would have caught it?
