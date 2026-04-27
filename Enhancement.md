# Enhancement Walkthrough — Daily Withdrawal Limits

A teaching example: add **per-account, per-calendar-day cumulative withdrawal limits** to the project, then study where the change lands.

This file describes the feature in this codebase (Java / Spring Boot / 3-tier layered). Sibling files in `AyvalikBankHA1`, `AyvalikBankHA-NET`, `AyvalikBankLA-NET`, `AyvalikBankHA-Python`, `AyvalikBankLA-Python` describe the same feature in their respective stacks so the impact can be compared side by side.

---

## The Feature

- Each `Account` carries a nullable `dailyWithdrawalLimit: BigDecimal`. Null = use a tier-derived default.
- Cumulative withdrawals (direct withdraw + the debit side of transfers) on a single UTC calendar day must not exceed that limit.
- Admin can set/clear the limit per account: `PUT /api/admin/accounts/{id}/daily-limit`.
- Reset at UTC midnight.
- A separate, additive constraint — the existing per-transaction tier caps still apply.

---

## Why this feature is good for teaching

It crosses every layer: model, repository, service, controller, validation. It introduces **state that lives across transactions** ("today's running total"), which is the interesting persistence question. And it sits at the intersection of `Customer`, `Account`, and `Transaction` — three aggregates — which is where the layered/anemic style starts to feel cramped.

---

## Impact on this project — Java 25 / Spring Boot 3.4 / Layered

### Files to add or modify

| # | Layer | Path | Change |
|---|---|---|---|
| 1 | Model | `model/Account.java` | Add `private BigDecimal dailyWithdrawalLimit;` + `@Column` mapping. Anemic — no method on the entity. |
| 2 | Repository | `repository/TransactionRepository.java` | Add `@Query("select coalesce(sum(t.amount), 0) from TransactionEntity t where t.accountId = :id and t.type = 'WITHDRAWAL' and t.timestamp >= :start and t.timestamp < :end") BigDecimal sumWithdrawalsBetween(...)` |
| 3 | Service | `service/AccountService.withdraw(...)` | **Inline** the query call, the comparison, the `throw new InsufficientFundsException(...)` (or new `DailyLimitExceededException`) into the existing method — interleaved with the existing overdraft / time-deposit / tier-cap branches |
| 4 | Service | `service/AccountService.transfer(...)` | Same inline insertion on the source-account debit path |
| 5 | Service | `service/AccountService.setDailyWithdrawalLimit(...)` *(new method)* | Loads account → mutates → flush via `repository.save(account)` |
| 6 | Web | `web/controller/AdminController.java` | New `PUT /api/admin/accounts/{id}/daily-limit` endpoint + `SetDailyLimitRequest` record |
| 7 | Web | `web/dto/request/SetDailyLimitRequest.java` *(new)* | DTO with validation annotations |
| 8 | Exception | `exception/DailyLimitExceededException.java` *(optional new)* | Subclass of existing `InsufficientFundsException` so the global handler still maps it to 422 |
| 9 | Tests | `test/java/.../AccountServiceWithdrawTest.java` | Extend with at-limit, just-over-limit, and after-midnight-reset cases — **must use `@DataJpaTest`** because the rule is computed from a SQL `SUM` |
| 10 | Tests | `AccountControllerTest`, `AdminControllerTest` | Add MockMvc cases for the new endpoint and the limit-exceeded response shape |
| 11 | Migration | `src/main/resources/data.sql` | `ALTER TABLE accounts ADD COLUMN IF NOT EXISTS daily_withdrawal_limit NUMERIC(19,2)` |

### Tech-stack-specific notes (Java)

- **Anemic JPA entity** — adding a field is a one-liner with `@Column(name = "daily_withdrawal_limit", precision = 19, scale = 2)`. No business method. The rule lives in the service.
- **Spring Data `@Query`** — the daily-sum query is a one-liner with HQL `coalesce(sum(...), 0)`. Same idiom as the HA sibling — but here the call site is *inside the service method*, not behind a port.
- **`@Transactional` boundary** — `AccountService.withdraw` and `transfer` are already `@Transactional`; the new `SUM` runs in the same transaction. Read consistency is automatic.
- **The `withdraw` method now has four branches**: overdraft, time-deposit-not-matured, tier cap, daily cap. Each was added separately; they accumulate in one place.
- **Date math** — `LocalDate utcDay = LocalDate.now(ZoneOffset.UTC); var start = utcDay.atStartOfDay().toInstant(ZoneOffset.UTC); var end = start.plus(1, ChronoUnit.DAYS);`.
- **No DI rewiring needed** — `AccountService` already has `TransactionRepository` injected; you just call a new method on it. **Faster to land than the HA version.**
- **Hibernate DDL caveat** — keep the new column nullable or add an explicit `ALTER TABLE` in `data.sql` (the project already uses this pattern for tier).

### Test impact

- **You cannot write a pure-unit test for the daily-limit rule in this architecture.** The rule is the SQL `SUM` plus the comparison plus the if-check plus the exception, all sitting inside a `@Transactional` service method. To test it, you need a `@DataJpaTest` (H2 / Postgres test container) so the `SUM` query actually returns a number.
- Compare against the HA sibling's `WithdrawalPolicyServiceTest` — pure JUnit, no Spring, no DB. That difference is the cost of the inlined approach.
- Existing service tests that mocked the repository now have to either stub out `sumWithdrawalsBetween(...)` or migrate to `@DataJpaTest`.

---

## Lesson Plan (apply to all six projects)

1. **Show both diffs side by side.** Count files; count *lines where the actual rule lives*.
2. **Change the rule** — "reset at customer's local midnight, not UTC." In HA you change one method on a domain service + one query in the adapter. In LA you edit a 40-line `withdraw` method that's already doing five other things; the change is wedged between the overdraft branch and the time-deposit-matured branch.
3. **Add a second consumer** — `GET /api/accounts/{id}/today-summary` showing withdrawn-so-far + remaining-limit. In HA: one controller method calling the existing port + policy. In LA: copy the `SUM` query + comparison into a new service method (or extract a helper — but that helper is a *de facto* domain service emerging from layered code, which is the architectural point).

The moral: **architecture is a bet about which kinds of change are likely.** Layered bets on rules being stable and local — it pays an entanglement tax later. The same feature shows the bet clearly.
