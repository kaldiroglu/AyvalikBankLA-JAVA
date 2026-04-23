# Flows — Ayvalık Bank LA-1

Sequence diagrams for the 7 key use cases. Each diagram shows the full call chain from the HTTP client through the layered stack to the database and back.

Actors used in all diagrams:
- **Client** — HTTP caller (curl, browser, integration test)
- **Controller** — the Spring MVC controller that receives the request
- **Service** — the Spring service that contains the business logic
- **Repository** — the Spring Data JPA repository interface
- **DB** — the underlying PostgreSQL / H2 database

---

## 1. CreateCustomer (Admin creates a customer)

```mermaid
sequenceDiagram
    actor Client
    participant Controller as AdminController
    participant Service as CustomerService
    participant PVS as PasswordValidationService
    participant Repository as CustomerRepository
    participant DB

    Client->>Controller: POST /api/admin/customers<br/>{"name","email","password"}<br/>Authorization: Basic admin:...

    Note over Controller: @Valid validates<br/>@NotBlank name, @Email email,<br/>@NotBlank password

    Controller->>Service: createCustomer(name, email, password)
    Service->>PVS: validate(password)
    Note over PVS: 8-16 chars, upper+lower+digit+special
    PVS-->>Service: (ok or throws InvalidPasswordException)

    Service->>Service: passwordEncoder.encode(password)
    Service->>Service: build Customer entity<br/>role="CUSTOMER", balance=0

    Service->>Repository: save(customer)
    Repository->>DB: INSERT INTO customers ...
    DB-->>Repository: saved row
    Repository-->>Service: Customer (with id)

    Service-->>Controller: Customer entity
    Controller->>Controller: CustomerResponse.from(customer)
    Controller-->>Client: 201 Created<br/>{"id","name","email","role"}
```

---

## 2. ChangePassword (Customer changes their password)

```mermaid
sequenceDiagram
    actor Client
    participant Controller as CustomerController
    participant Service as CustomerService
    participant PVS as PasswordValidationService
    participant Repository as CustomerRepository
    participant DB

    Client->>Controller: PUT /api/customers/{id}/password<br/>{"newPassword":"Valid@123"}<br/>Authorization: Basic customer:...

    Note over Controller: @Valid: @NotBlank newPassword

    Controller->>Service: changePassword(id, newPassword)
    Service->>PVS: validate(newPassword)
    PVS-->>Service: (ok or throws InvalidPasswordException)

    Service->>Repository: findById(id)
    Repository->>DB: SELECT * FROM customers WHERE id=?
    DB-->>Repository: Customer row
    Repository-->>Service: Optional<Customer>

    Note over Service: Check reuse against current password<br/>passwordEncoder.matches(new, current)

    loop each PasswordHistory entry (up to 3)
        Service->>Service: passwordEncoder.matches(newPassword, history.hashedPassword)
        Note over Service: throws PasswordReusedException<br/>if any match found
    end

    Service->>Service: passwordEncoder.encode(newPassword)
    Service->>Service: rotate history:<br/>prepend old current hash,<br/>trim list to 3 entries
    Service->>Service: customer.setCurrentPassword(newHash)

    Service->>Repository: save(customer)
    Repository->>DB: UPDATE customers SET current_password=?<br/>+ cascade UPDATE password_history
    DB-->>Repository: updated
    Repository-->>Service: Customer

    Service-->>Controller: (void)
    Controller-->>Client: 200 OK
```

---

## 3. Deposit (Customer deposits money into an account)

```mermaid
sequenceDiagram
    actor Client
    participant Controller as AccountController
    participant Service as AccountService
    participant AccRepo as AccountRepository
    participant TxRepo as TransactionRepository
    participant DB

    Client->>Controller: POST /api/accounts/{id}/deposit<br/>{"amount":500,"currency":"USD"}<br/>Authorization: Basic customer:...

    Note over Controller: @Valid: @Positive amount,<br/>@NotNull currency

    Controller->>Service: deposit(accountId, amount, currency)

    Service->>AccRepo: findById(accountId)
    AccRepo->>DB: SELECT * FROM accounts WHERE id=?
    DB-->>AccRepo: Account row
    AccRepo-->>Service: Optional<Account>

    Note over Service: Check status == ACTIVE<br/>(else AccountNotOperableException)
    Note over Service: Check currency match<br/>(else IllegalArgumentException)

    Service->>Service: account.setBalance(balance + amount)

    Service->>AccRepo: save(account)
    AccRepo->>DB: UPDATE accounts SET balance=?
    DB-->>AccRepo: updated

    Service->>Service: build Transaction entity<br/>type=DEPOSIT, amount, currency, createdAt=now

    Service->>TxRepo: save(transaction)
    TxRepo->>DB: INSERT INTO transactions ...
    DB-->>TxRepo: saved row
    TxRepo-->>Service: Transaction (with id)

    Service-->>Controller: Transaction entity
    Controller->>Controller: TransactionResponse.from(transaction)
    Controller-->>Client: 201 Created<br/>{"id","type":"DEPOSIT","amount","currency","createdAt"}
```

---

## 4. Transfer (Cross-customer transfer with fee)

```mermaid
sequenceDiagram
    actor Client
    participant Controller as AccountController
    participant Service as AccountService
    participant TransferSvc as TransferService
    participant SettingsRepo as SettingsRepository
    participant AccRepo as AccountRepository
    participant TxRepo as TransactionRepository
    participant DB

    Client->>Controller: POST /api/accounts/{sourceId}/transfer<br/>{"targetAccountId","amount":200,"currency":"USD"}<br/>Authorization: Basic customer:...

    Controller->>Service: transfer(sourceId, targetId, amount, currency)

    Service->>AccRepo: findById(sourceId)
    AccRepo->>DB: SELECT * FROM accounts WHERE id=?
    DB-->>AccRepo: source Account
    AccRepo-->>Service: Optional<Account>

    Service->>AccRepo: findById(targetId)
    AccRepo->>DB: SELECT * FROM accounts WHERE id=?
    DB-->>AccRepo: target Account
    AccRepo-->>Service: Optional<Account>

    Note over Service: Check both accounts ACTIVE<br/>(else AccountNotOperableException)
    Note over Service: Check currency match on both<br/>(else IllegalArgumentException)

    Service->>SettingsRepo: findById("TRANSFER_FEE_PERCENT")
    SettingsRepo->>DB: SELECT value FROM settings WHERE key=?
    DB-->>SettingsRepo: "1.0"
    SettingsRepo-->>Service: Optional<Settings>

    Note over Service: sameCustomer = (source.ownerId == target.ownerId)

    Service->>TransferSvc: calculateFee(200, sameCustomer=false, feePercent=1.0)
    Note over TransferSvc: fee = 200 * 1.0 / 100 = 2.00
    TransferSvc-->>Service: BigDecimal(2.00)

    Note over Service: Check source.balance >= amount + fee<br/>(else InsufficientFundsException)

    Service->>Service: source.balance -= (amount + fee)  →  798.00
    Service->>Service: target.balance += amount           →  200.00

    Service->>AccRepo: save(source)
    AccRepo->>DB: UPDATE accounts SET balance=798.00 WHERE id=source
    Service->>AccRepo: save(target)
    AccRepo->>DB: UPDATE accounts SET balance=200.00 WHERE id=target

    Service->>TxRepo: save(TRANSFER_OUT tx for source)
    TxRepo->>DB: INSERT INTO transactions (type=TRANSFER_OUT, amount=202, ...)
    Service->>TxRepo: save(TRANSFER_IN tx for target)
    TxRepo->>DB: INSERT INTO transactions (type=TRANSFER_IN, amount=200, ...)

    Service-->>Controller: (void)
    Controller-->>Client: 200 OK
```

---

## 5. DeleteCustomer (Admin deletes a customer)

```mermaid
sequenceDiagram
    actor Client
    participant Controller as AdminController
    participant Service as CustomerService
    participant Repository as CustomerRepository
    participant DB

    Client->>Controller: DELETE /api/admin/customers/{id}<br/>Authorization: Basic admin:...

    Controller->>Service: deleteCustomer(id)

    Service->>Repository: existsById(id)
    Repository->>DB: SELECT COUNT(*) FROM customers WHERE id=?
    DB-->>Repository: 1 (or 0)
    Repository-->>Service: true (or false)

    alt customer not found
        Service-->>Controller: throws CustomerNotFoundException
        Controller-->>Client: 404 Not Found
    else customer exists
        Service->>Repository: deleteById(id)
        Repository->>DB: DELETE FROM customers WHERE id=?
        Note over DB: CASCADE DELETE fires:<br/>DELETE FROM password_history<br/>WHERE customer_id=?<br/>(via @OneToMany cascade=ALL, orphanRemoval)
        DB-->>Repository: deleted
        Repository-->>Service: (void)

        Service-->>Controller: (void)
        Controller-->>Client: 204 No Content
    end
```

---

## 6. GetBalance (Customer checks account balance)

```mermaid
sequenceDiagram
    actor Client
    participant Controller as AccountController
    participant Service as AccountService
    participant Repository as AccountRepository
    participant DB

    Client->>Controller: GET /api/accounts/{id}/balance<br/>Authorization: Basic customer:...

    Controller->>Service: getAccount(accountId)

    Service->>Repository: findById(accountId)
    Repository->>DB: SELECT * FROM accounts WHERE id=?
    DB-->>Repository: Account row (or empty)
    Repository-->>Service: Optional<Account>

    alt account not found
        Service-->>Controller: throws AccountNotFoundException
        Controller-->>Client: 404 Not Found
    else account found
        Service-->>Controller: Account entity

        Controller->>Controller: BalanceResponse.from(account)<br/>maps balance + currency

        Controller-->>Client: 200 OK<br/>{"amount":500.00,"currency":"USD"}
    end
```

---

## 7. FreezeAccount (Admin freezes an account — state machine)

```mermaid
sequenceDiagram
    actor Client
    participant Controller as AdminController
    participant Service as AccountService
    participant Repository as AccountRepository
    participant DB

    Client->>Controller: PUT /api/admin/accounts/{id}/freeze<br/>Authorization: Basic admin:...

    Controller->>Service: freezeAccount(accountId)

    Service->>Repository: findById(accountId)
    Repository->>DB: SELECT * FROM accounts WHERE id=?
    DB-->>Repository: Account row
    Repository-->>Service: Optional<Account>

    alt account not found
        Service-->>Controller: throws AccountNotFoundException
        Controller-->>Client: 404 Not Found
    else status == FROZEN or CLOSED
        Note over Service: Only ACTIVE accounts can be frozen
        Service-->>Controller: throws AccountNotOperableException
        Controller-->>Client: 422 Unprocessable Entity
    else status == ACTIVE
        Service->>Service: account.setStatus(FROZEN)

        Service->>Repository: save(account)
        Repository->>DB: UPDATE accounts SET status='FROZEN' WHERE id=?
        DB-->>Repository: updated
        Repository-->>Service: Account

        Service-->>Controller: (void)
        Controller-->>Client: 200 OK
    end
```

---

## State Machine Reference

```
              ┌──────────┐
  open()      │          │  freeze()
─────────────►│  ACTIVE  │──────────────┐
              │          │              │
              └────┬─────┘              ▼
                   │             ┌──────────────┐
                   │  close()    │    FROZEN    │
                   │    ┌────────│              │
                   │    │        └──────┬───────┘
                   │    │               │ unfreeze()
                   │    │               │
                   ▼    ▼               │
              ┌──────────┐             │
              │  CLOSED  │◄────────────┘
              │ (terminal)│  close()
              └──────────┘
```

Valid transitions:
- `ACTIVE → FROZEN` via `freeze()`
- `FROZEN → ACTIVE` via `unfreeze()`
- `ACTIVE → CLOSED` via `close()`
- `FROZEN → CLOSED` via `close()`
- Any attempt to transition from `CLOSED` throws `AccountNotOperableException`
- Freezing a `FROZEN` account or unfreezing an `ACTIVE` account throws `AccountNotOperableException`
