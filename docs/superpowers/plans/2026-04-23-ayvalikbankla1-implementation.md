# AyvalikBankLA1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build AyvalikBankLA1 — a banking app with identical use cases as AyvalikBankHA1, implemented in Classic 3-Tier Layered Architecture with anemic JPA entities as domain objects.

**Architecture:** Presentation → Service → Repository. No port interfaces, no mappers. JPA @Entity classes serve as domain objects throughout all layers. All business logic lives in the service layer.

**Tech Stack:** Java 21, Spring Boot 3.4, Spring Data JPA, PostgreSQL (prod) / H2 (tests), Spring Security HTTP Basic, Jakarta Validation, JUnit 5, AssertJ, Mockito, MockMvc, Maven.

**Base package:** `dev.kaldiroglu.layered.ayvalikbank`

---

## Task 1: Maven project scaffold

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/dev/kaldiroglu/layered/ayvalikbank/AyvalikBankApplication.java`
- Create: `src/main/resources/application.properties`
- Create: `src/main/resources/data.sql`
- Create: `src/test/resources/application-test.properties`
- Create: `docker-compose.yml`

- [ ] **Step 1: Create `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.4</version>
        <relativePath/>
    </parent>
    <groupId>dev.kaldiroglu.layered</groupId>
    <artifactId>ayvalik-bank-la</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>Ayvalık Bank LA-1</name>
    <description>Layered Architecture Banking Application</description>
    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <argLine>-Dnet.bytebuddy.experimental=true</argLine>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create main application class**

`src/main/java/dev/kaldiroglu/layered/ayvalikbank/AyvalikBankApplication.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AyvalikBankApplication {
    public static void main(String[] args) {
        SpringApplication.run(AyvalikBankApplication.class, args);
    }
}
```

- [ ] **Step 3: Create `src/main/resources/application.properties`**

```properties
spring.application.name=ayvalik-bank-la

spring.datasource.url=jdbc:postgresql://localhost:5432/ayvalikbank
spring.datasource.username=bankuser
spring.datasource.password=bankpass
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true

spring.sql.init.mode=always
spring.jpa.defer-datasource-initialization=true
```

- [ ] **Step 4: Create `src/main/resources/data.sql`**

```sql
INSERT INTO settings (key, value)
VALUES ('TRANSFER_FEE_PERCENT', '1.0')
ON CONFLICT (key) DO NOTHING;
```

- [ ] **Step 5: Create `src/test/resources/application-test.properties`**

```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

spring.sql.init.mode=always
spring.jpa.defer-datasource-initialization=true
```

- [ ] **Step 6: Create `docker-compose.yml`**

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: ayvalikbank
      POSTGRES_USER: bankuser
      POSTGRES_PASSWORD: bankpass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

- [ ] **Step 7: Verify compilation**

```bash
mvn compile -q
```
Expected: `BUILD SUCCESS` with no errors.

- [ ] **Step 8: Commit**

```bash
git init
git add .
git commit -m "chore: scaffold Maven project for AyvalikBankLA1"
```

---

## Task 2: Model enums and @Entity classes

**Files:**
- Create: `src/main/java/dev/kaldiroglu/layered/ayvalikbank/model/AccountStatus.java`
- Create: `src/main/java/dev/kaldiroglu/layered/ayvalikbank/model/Currency.java`
- Create: `src/main/java/dev/kaldiroglu/layered/ayvalikbank/model/TransactionType.java`
- Create: `src/main/java/dev/kaldiroglu/layered/ayvalikbank/model/Customer.java`
- Create: `src/main/java/dev/kaldiroglu/layered/ayvalikbank/model/PasswordHistory.java`
- Create: `src/main/java/dev/kaldiroglu/layered/ayvalikbank/model/Account.java`
- Create: `src/main/java/dev/kaldiroglu/layered/ayvalikbank/model/Transaction.java`
- Create: `src/main/java/dev/kaldiroglu/layered/ayvalikbank/model/Settings.java`

- [ ] **Step 1: Create enums**

`src/main/java/dev/kaldiroglu/layered/ayvalikbank/model/AccountStatus.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.model;
public enum AccountStatus { ACTIVE, FROZEN, CLOSED }
```

`src/main/java/dev/kaldiroglu/layered/ayvalikbank/model/Currency.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.model;
public enum Currency { USD, EUR, TRY }
```

`src/main/java/dev/kaldiroglu/layered/ayvalikbank/model/TransactionType.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.model;
public enum TransactionType { DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT }
```

- [ ] **Step 2: Create `Customer` entity**

`src/main/java/dev/kaldiroglu/layered/ayvalikbank/model/Customer.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String role;

    @Column(name = "current_password", nullable = false)
    private String currentPassword;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("position ASC")
    private List<PasswordHistory> passwordHistory = new ArrayList<>();

    public Customer() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public List<PasswordHistory> getPasswordHistory() { return passwordHistory; }
    public void setPasswordHistory(List<PasswordHistory> passwordHistory) { this.passwordHistory = passwordHistory; }
}
```

- [ ] **Step 3: Create `PasswordHistory` entity**

`src/main/java/dev/kaldiroglu/layered/ayvalikbank/model/PasswordHistory.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "password_history")
public class PasswordHistory {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "hashed_password", nullable = false)
    private String hashedPassword;

    @Column(nullable = false)
    private int position;

    public PasswordHistory() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public String getHashedPassword() { return hashedPassword; }
    public void setHashedPassword(String hashedPassword) { this.hashedPassword = hashedPassword; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
```

- [ ] **Step 4: Create `Account` entity**

`src/main/java/dev/kaldiroglu/layered/ayvalikbank/model/Account.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Currency currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccountStatus status;

    public Account() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
}
```

- [ ] **Step 5: Create `Transaction` entity**

`src/main/java/dev/kaldiroglu/layered/ayvalikbank/model/Transaction.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Currency currency;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column
    private String description;

    public Transaction() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
```

- [ ] **Step 6: Create `Settings` entity**

`src/main/java/dev/kaldiroglu/layered/ayvalikbank/model/Settings.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.model;

import jakarta.persistence.*;

@Entity
@Table(name = "settings")
public class Settings {

    @Id
    private String key;

    @Column(nullable = false)
    private String value;

    public Settings() {}

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
```

- [ ] **Step 7: Verify compilation**

```bash
mvn compile -q
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/dev/kaldiroglu/layered/ayvalikbank/model/
git commit -m "feat: add model enums and anemic @Entity classes"
```

---

## Task 3: Exception classes

**Files (all in `src/main/java/dev/kaldiroglu/layered/ayvalikbank/exception/`):**
- Create: `AccountNotFoundException.java`
- Create: `AccountNotOperableException.java`
- Create: `CustomerNotFoundException.java`
- Create: `InsufficientFundsException.java`
- Create: `InvalidPasswordException.java`
- Create: `PasswordReusedException.java`
- Create: `UnauthorizedAccessException.java`

- [ ] **Step 1: Create all seven exception classes**

`AccountNotFoundException.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.exception;
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String message) { super(message); }
}
```

`AccountNotOperableException.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.exception;
public class AccountNotOperableException extends RuntimeException {
    public AccountNotOperableException(String message) { super(message); }
}
```

`CustomerNotFoundException.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.exception;
public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(String message) { super(message); }
}
```

`InsufficientFundsException.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.exception;
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) { super(message); }
}
```

`InvalidPasswordException.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.exception;
public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException(String message) { super(message); }
}
```

`PasswordReusedException.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.exception;
public class PasswordReusedException extends RuntimeException {
    public PasswordReusedException(String message) { super(message); }
}
```

`UnauthorizedAccessException.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.exception;
public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String message) { super(message); }
}
```

- [ ] **Step 2: Verify compilation**

```bash
mvn compile -q
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/dev/kaldiroglu/layered/ayvalikbank/exception/
git commit -m "feat: add typed exception classes"
```

---

## Task 4: Repository interfaces

**Files (all in `src/main/java/dev/kaldiroglu/layered/ayvalikbank/repository/`):**
- Create: `CustomerRepository.java`
- Create: `AccountRepository.java`
- Create: `TransactionRepository.java`
- Create: `SettingsRepository.java`

- [ ] **Step 1: Create repository interfaces**

`CustomerRepository.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.repository;

import dev.kaldiroglu.layered.ayvalikbank.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByEmail(String email);
}
```

`AccountRepository.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.repository;

import dev.kaldiroglu.layered.ayvalikbank.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByOwnerId(UUID ownerId);
}
```

`TransactionRepository.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.repository;

import dev.kaldiroglu.layered.ayvalikbank.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByAccountId(UUID accountId);
}
```

`SettingsRepository.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.repository;

import dev.kaldiroglu.layered.ayvalikbank.model.Settings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingsRepository extends JpaRepository<Settings, String> {
}
```

- [ ] **Step 2: Verify compilation**

```bash
mvn compile -q
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/dev/kaldiroglu/layered/ayvalikbank/repository/
git commit -m "feat: add Spring Data JPA repository interfaces"
```

---

## Task 5: PasswordValidationService (TDD)

**Files:**
- Create: `src/main/java/dev/kaldiroglu/layered/ayvalikbank/service/PasswordValidationService.java`
- Create: `src/test/java/dev/kaldiroglu/layered/ayvalikbank/service/PasswordValidationServiceTest.java`

- [ ] **Step 1: Write the failing tests**

`src/test/java/dev/kaldiroglu/layered/ayvalikbank/service/PasswordValidationServiceTest.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.service;

import dev.kaldiroglu.layered.ayvalikbank.exception.InvalidPasswordException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class PasswordValidationServiceTest {

    private PasswordValidationService service;

    @BeforeEach
    void setUp() { service = new PasswordValidationService(); }

    @Test
    void shouldAcceptValidPassword() {
        assertThatNoException().isThrownBy(() -> service.validate("Valid@123"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Short1!", "ThisIsWayTooLong1!"})
    void shouldRejectPasswordOutOfLengthRange(String password) {
        assertThatThrownBy(() -> service.validate(password))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("between 8 and 16");
    }

    @Test
    void shouldRejectPasswordWithoutUppercase() {
        assertThatThrownBy(() -> service.validate("nouppercase1!"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("uppercase");
    }

    @Test
    void shouldRejectPasswordWithoutLowercase() {
        assertThatThrownBy(() -> service.validate("NOLOWERCASE1!"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("lowercase");
    }

    @Test
    void shouldRejectPasswordWithoutDigit() {
        assertThatThrownBy(() -> service.validate("NoDigitHere!"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("digit");
    }

    @Test
    void shouldRejectPasswordWithoutSpecialCharacter() {
        assertThatThrownBy(() -> service.validate("NoSpecial123"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("special");
    }

    @Test
    void shouldRejectNullPassword() {
        assertThatThrownBy(() -> service.validate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Run tests — confirm they fail (class not found)**

```bash
mvn test -Dtest=PasswordValidationServiceTest -q 2>&1 | tail -5
```
Expected: compilation error — `PasswordValidationService` does not exist yet.

- [ ] **Step 3: Implement `PasswordValidationService`**

`src/main/java/dev/kaldiroglu/layered/ayvalikbank/service/PasswordValidationService.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.service;

import dev.kaldiroglu.layered.ayvalikbank.exception.InvalidPasswordException;
import org.springframework.stereotype.Service;

@Service
public class PasswordValidationService {

    public void validate(String password) {
        if (password == null)
            throw new IllegalArgumentException("Password must not be null");
        if (password.length() < 8 || password.length() > 16)
            throw new InvalidPasswordException("Password must be between 8 and 16 characters");
        if (!password.matches(".*[A-Z].*"))
            throw new InvalidPasswordException("Password must contain at least one uppercase letter");
        if (!password.matches(".*[a-z].*"))
            throw new InvalidPasswordException("Password must contain at least one lowercase letter");
        if (!password.matches(".*[0-9].*"))
            throw new InvalidPasswordException("Password must contain at least one digit");
        if (!password.matches(".*[^A-Za-z0-9].*"))
            throw new InvalidPasswordException("Password must contain at least one special character");
    }
}
```

- [ ] **Step 4: Run tests — confirm they pass**

```bash
mvn test -Dtest=PasswordValidationServiceTest -q
```
Expected: `Tests run: 8, Failures: 0, Errors: 0`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/kaldiroglu/layered/ayvalikbank/service/PasswordValidationService.java \
        src/test/java/dev/kaldiroglu/layered/ayvalikbank/service/PasswordValidationServiceTest.java
git commit -m "feat: add PasswordValidationService with 8 tests"
```

---

## Task 6: TransferService (TDD)

**Files:**
- Create: `src/main/java/dev/kaldiroglu/layered/ayvalikbank/service/TransferService.java`
- Create: `src/test/java/dev/kaldiroglu/layered/ayvalikbank/service/TransferServiceTest.java`

- [ ] **Step 1: Write the failing tests**

`src/test/java/dev/kaldiroglu/layered/ayvalikbank/service/TransferServiceTest.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TransferServiceTest {

    private TransferService service;

    @BeforeEach
    void setUp() { service = new TransferService(); }

    @Test
    void shouldReturnZeroFeeForSameCustomerTransfer() {
        BigDecimal fee = service.calculateFee(new BigDecimal("200"), true, new BigDecimal("1.0"));
        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldCalculateFeeForDifferentCustomers() {
        BigDecimal fee = service.calculateFee(new BigDecimal("200"), false, new BigDecimal("1.0"));
        assertThat(fee).isEqualByComparingTo("2.00");
    }

    @Test
    void shouldReturnZeroFeeWhenFeePercentIsZero() {
        BigDecimal fee = service.calculateFee(new BigDecimal("200"), false, BigDecimal.ZERO);
        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
```

- [ ] **Step 2: Run tests — confirm they fail**

```bash
mvn test -Dtest=TransferServiceTest -q 2>&1 | tail -5
```
Expected: compilation error — `TransferService` does not exist yet.

- [ ] **Step 3: Implement `TransferService`**

`src/main/java/dev/kaldiroglu/layered/ayvalikbank/service/TransferService.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TransferService {

    public BigDecimal calculateFee(BigDecimal amount, boolean sameCustomer, BigDecimal feePercent) {
        if (sameCustomer) return BigDecimal.ZERO;
        return amount.multiply(feePercent)
                     .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
```

- [ ] **Step 4: Run tests — confirm they pass**

```bash
mvn test -Dtest=TransferServiceTest -q
```
Expected: `Tests run: 3, Failures: 0, Errors: 0`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/kaldiroglu/layered/ayvalikbank/service/TransferService.java \
        src/test/java/dev/kaldiroglu/layered/ayvalikbank/service/TransferServiceTest.java
git commit -m "feat: add TransferService with fee calculation tests"
```

---

## Task 7: CustomerService (TDD — Mockito)

**Files:**
- Create: `src/main/java/dev/kaldiroglu/layered/ayvalikbank/service/CustomerService.java`
- Create: `src/test/java/dev/kaldiroglu/layered/ayvalikbank/service/CustomerServiceTest.java`

- [ ] **Step 1: Write the failing tests**

`src/test/java/dev/kaldiroglu/layered/ayvalikbank/service/CustomerServiceTest.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.service;

import dev.kaldiroglu.layered.ayvalikbank.exception.*;
import dev.kaldiroglu.layered.ayvalikbank.model.Customer;
import dev.kaldiroglu.layered.ayvalikbank.model.PasswordHistory;
import dev.kaldiroglu.layered.ayvalikbank.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private CustomerService service;

    @BeforeEach
    void setUp() {
        service = new CustomerService(customerRepository, passwordEncoder,
                new PasswordValidationService());
    }

    // ── createCustomer ────────────────────────────────────────────────────

    @Test
    void shouldCreateCustomerWithHashedPassword() {
        when(passwordEncoder.encode("Valid@123")).thenReturn("hashed");
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Customer result = service.createCustomer("Ali", "ali@test.com", "Valid@123");

        assertThat(result.getEmail()).isEqualTo("ali@test.com");
        assertThat(result.getCurrentPassword()).isEqualTo("hashed");
        assertThat(result.getRole()).isEqualTo("CUSTOMER");
        verify(customerRepository).save(any());
    }

    @Test
    void shouldThrowInvalidPasswordExceptionForWeakPassword() {
        assertThatThrownBy(() -> service.createCustomer("Ali", "ali@test.com", "weak"))
                .isInstanceOf(InvalidPasswordException.class);
        verifyNoInteractions(customerRepository);
    }

    // ── deleteCustomer ────────────────────────────────────────────────────

    @Test
    void shouldDeleteExistingCustomer() {
        UUID id = UUID.randomUUID();
        when(customerRepository.existsById(id)).thenReturn(true);

        service.deleteCustomer(id);

        verify(customerRepository).deleteById(id);
    }

    @Test
    void shouldThrowCustomerNotFoundOnDeleteOfMissingCustomer() {
        UUID id = UUID.randomUUID();
        when(customerRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteCustomer(id))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    // ── changePassword ────────────────────────────────────────────────────

    @Test
    void shouldChangePasswordSuccessfully() {
        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(id);
        customer.setCurrentPassword("old-hash");

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("Valid@123", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("Valid@123")).thenReturn("new-hash");
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.changePassword(id, "Valid@123");

        assertThat(customer.getCurrentPassword()).isEqualTo("new-hash");
        assertThat(customer.getPasswordHistory()).hasSize(1);
        assertThat(customer.getPasswordHistory().get(0).getHashedPassword()).isEqualTo("old-hash");
        verify(customerRepository).save(customer);
    }

    @Test
    void shouldThrowPasswordReusedExceptionWhenNewPasswordMatchesCurrent() {
        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(id);
        customer.setCurrentPassword("same-hash");

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("Valid@123", "same-hash")).thenReturn(true);

        assertThatThrownBy(() -> service.changePassword(id, "Valid@123"))
                .isInstanceOf(PasswordReusedException.class);
    }
}
```

- [ ] **Step 2: Run tests — confirm they fail**

```bash
mvn test -Dtest=CustomerServiceTest -q 2>&1 | tail -5
```
Expected: compilation error — `CustomerService` does not exist yet.

- [ ] **Step 3: Implement `CustomerService`**

`src/main/java/dev/kaldiroglu/layered/ayvalikbank/service/CustomerService.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.service;

import dev.kaldiroglu.layered.ayvalikbank.exception.CustomerNotFoundException;
import dev.kaldiroglu.layered.ayvalikbank.exception.PasswordReusedException;
import dev.kaldiroglu.layered.ayvalikbank.model.Customer;
import dev.kaldiroglu.layered.ayvalikbank.model.PasswordHistory;
import dev.kaldiroglu.layered.ayvalikbank.repository.CustomerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CustomerService {

    private static final int PASSWORD_HISTORY_SIZE = 3;

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidationService passwordValidationService;

    public CustomerService(CustomerRepository customerRepository,
                           PasswordEncoder passwordEncoder,
                           PasswordValidationService passwordValidationService) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidationService = passwordValidationService;
    }

    public Customer createCustomer(String name, String email, String rawPassword) {
        passwordValidationService.validate(rawPassword);
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setName(name);
        customer.setEmail(email);
        customer.setRole("CUSTOMER");
        customer.setCurrentPassword(passwordEncoder.encode(rawPassword));
        return customerRepository.save(customer);
    }

    public void deleteCustomer(UUID id) {
        if (!customerRepository.existsById(id))
            throw new CustomerNotFoundException("Customer not found: " + id);
        customerRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Customer> listCustomers() {
        return customerRepository.findAll();
    }

    public void changePassword(UUID customerId, String rawNewPassword) {
        passwordValidationService.validate(rawNewPassword);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        List<String> allHashes = new ArrayList<>();
        allHashes.add(customer.getCurrentPassword());
        customer.getPasswordHistory().stream()
                .sorted((a, b) -> a.getPosition() - b.getPosition())
                .forEach(ph -> allHashes.add(ph.getHashedPassword()));

        for (String hash : allHashes) {
            if (passwordEncoder.matches(rawNewPassword, hash))
                throw new PasswordReusedException("Password has been used recently");
        }

        // Build new history: old current + up to 2 previous entries
        List<String> newHistoryHashes = new ArrayList<>();
        newHistoryHashes.add(customer.getCurrentPassword());
        customer.getPasswordHistory().stream()
                .sorted((a, b) -> a.getPosition() - b.getPosition())
                .limit(PASSWORD_HISTORY_SIZE - 1)
                .forEach(ph -> newHistoryHashes.add(ph.getHashedPassword()));

        // Replace history (orphanRemoval handles DB deletes)
        customer.getPasswordHistory().clear();
        for (int i = 0; i < newHistoryHashes.size(); i++) {
            PasswordHistory ph = new PasswordHistory();
            ph.setId(UUID.randomUUID());
            ph.setCustomer(customer);
            ph.setHashedPassword(newHistoryHashes.get(i));
            ph.setPosition(i);
            customer.getPasswordHistory().add(ph);
        }

        customer.setCurrentPassword(passwordEncoder.encode(rawNewPassword));
        customerRepository.save(customer);
    }
}
```

- [ ] **Step 4: Run tests — confirm they pass**

```bash
mvn test -Dtest=CustomerServiceTest -q
```
Expected: `Tests run: 6, Failures: 0, Errors: 0`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/kaldiroglu/layered/ayvalikbank/service/CustomerService.java \
        src/test/java/dev/kaldiroglu/layered/ayvalikbank/service/CustomerServiceTest.java
git commit -m "feat: add CustomerService with 6 Mockito tests"
```

---

## Task 8: AccountService (TDD — Mockito)

**Files:**
- Create: `src/main/java/dev/kaldiroglu/layered/ayvalikbank/service/AccountService.java`
- Create: `src/test/java/dev/kaldiroglu/layered/ayvalikbank/service/AccountServiceTest.java`

- [ ] **Step 1: Write the failing tests**

`src/test/java/dev/kaldiroglu/layered/ayvalikbank/service/AccountServiceTest.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.service;

import dev.kaldiroglu.layered.ayvalikbank.exception.*;
import dev.kaldiroglu.layered.ayvalikbank.model.*;
import dev.kaldiroglu.layered.ayvalikbank.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private SettingsRepository settingsRepository;

    private AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService(accountRepository, customerRepository,
                transactionRepository, settingsRepository, new TransferService());
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private Account makeAccount(UUID ownerId, Currency currency) {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        a.setOwnerId(ownerId);
        a.setCurrency(currency);
        a.setBalance(BigDecimal.ZERO);
        a.setStatus(AccountStatus.ACTIVE);
        return a;
    }

    private Settings makeSettings(String value) {
        Settings s = new Settings();
        s.setKey("TRANSFER_FEE_PERCENT");
        s.setValue(value);
        return s;
    }

    // ── createAccount ─────────────────────────────────────────────────────

    @Test
    void shouldCreateAccountForExistingCustomer() {
        UUID ownerId = UUID.randomUUID();
        when(customerRepository.existsById(ownerId)).thenReturn(true);
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Account account = service.createAccount(ownerId, Currency.USD);

        assertThat(account.getCurrency()).isEqualTo(Currency.USD);
        assertThat(account.getOwnerId()).isEqualTo(ownerId);
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void shouldThrowCustomerNotFoundWhenOwnerMissing() {
        UUID ownerId = UUID.randomUUID();
        when(customerRepository.existsById(ownerId)).thenReturn(false);

        assertThatThrownBy(() -> service.createAccount(ownerId, Currency.EUR))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    // ── deposit ───────────────────────────────────────────────────────────

    @Test
    void shouldDepositMoneyToAccount() {
        Account account = makeAccount(UUID.randomUUID(), Currency.USD);
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transaction tx = service.deposit(account.getId(), new BigDecimal("200"), Currency.USD);

        assertThat(tx.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(account.getBalance()).isEqualByComparingTo("200.00");
    }

    @Test
    void shouldThrowAccountNotFoundOnDepositToMissingAccount() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deposit(id, new BigDecimal("100"), Currency.USD))
                .isInstanceOf(AccountNotFoundException.class);
    }

    // ── withdraw ──────────────────────────────────────────────────────────

    @Test
    void shouldThrowInsufficientFundsOnWithdrawExceedingBalance() {
        Account account = makeAccount(UUID.randomUUID(), Currency.USD);
        account.setBalance(new BigDecimal("100"));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.withdraw(account.getId(), new BigDecimal("500"), Currency.USD))
                .isInstanceOf(InsufficientFundsException.class);
    }

    // ── transfer ──────────────────────────────────────────────────────────

    @Test
    void shouldTransferBetweenAccountsOfSameCustomerFreeOfCharge() {
        UUID ownerId = UUID.randomUUID();
        Account source = makeAccount(ownerId, Currency.USD);
        source.setBalance(new BigDecimal("500"));
        Account target = makeAccount(ownerId, Currency.USD);

        when(accountRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(accountRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(settingsRepository.findById("TRANSFER_FEE_PERCENT"))
                .thenReturn(Optional.of(makeSettings("1.0")));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.transfer(source.getId(), target.getId(), new BigDecimal("200"), Currency.USD);

        assertThat(source.getBalance()).isEqualByComparingTo("300.00");
        assertThat(target.getBalance()).isEqualByComparingTo("200.00");
    }

    @Test
    void shouldDeductFeeForTransferBetweenDifferentCustomers() {
        Account source = makeAccount(UUID.randomUUID(), Currency.USD);
        source.setBalance(new BigDecimal("1000"));
        Account target = makeAccount(UUID.randomUUID(), Currency.USD);

        when(accountRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(accountRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(settingsRepository.findById("TRANSFER_FEE_PERCENT"))
                .thenReturn(Optional.of(makeSettings("1.0")));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.transfer(source.getId(), target.getId(), new BigDecimal("200"), Currency.USD);

        assertThat(source.getBalance()).isEqualByComparingTo("798.00");
        assertThat(target.getBalance()).isEqualByComparingTo("200.00");
    }

    // ── freeze / unfreeze / close ─────────────────────────────────────────

    @Test
    void shouldFreezeAccount() {
        Account account = makeAccount(UUID.randomUUID(), Currency.USD);
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.freezeAccount(account.getId());

        assertThat(account.getStatus()).isEqualTo(AccountStatus.FROZEN);
        verify(accountRepository).save(account);
    }

    @Test
    void shouldUnfreezeAccount() {
        Account account = makeAccount(UUID.randomUUID(), Currency.USD);
        account.setStatus(AccountStatus.FROZEN);
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.unfreezeAccount(account.getId());

        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(accountRepository).save(account);
    }

    @Test
    void shouldCloseAccount() {
        Account account = makeAccount(UUID.randomUUID(), Currency.USD);
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.closeAccount(account.getId());

        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
        verify(accountRepository).save(account);
    }

    @Test
    void shouldThrowAccountNotOperableWhenFreezingClosedAccount() {
        Account account = makeAccount(UUID.randomUUID(), Currency.USD);
        account.setStatus(AccountStatus.CLOSED);
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.freezeAccount(account.getId()))
                .isInstanceOf(AccountNotOperableException.class);
    }

    @Test
    void shouldThrowAccountNotFoundWhenFreezingMissingAccount() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.freezeAccount(id))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
```

- [ ] **Step 2: Run tests — confirm they fail**

```bash
mvn test -Dtest=AccountServiceTest -q 2>&1 | tail -5
```
Expected: compilation error — `AccountService` does not exist yet.

- [ ] **Step 3: Implement `AccountService`**

`src/main/java/dev/kaldiroglu/layered/ayvalikbank/service/AccountService.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.service;

import dev.kaldiroglu.layered.ayvalikbank.exception.*;
import dev.kaldiroglu.layered.ayvalikbank.model.*;
import dev.kaldiroglu.layered.ayvalikbank.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final SettingsRepository settingsRepository;
    private final TransferService transferService;

    public AccountService(AccountRepository accountRepository,
                          CustomerRepository customerRepository,
                          TransactionRepository transactionRepository,
                          SettingsRepository settingsRepository,
                          TransferService transferService) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.settingsRepository = settingsRepository;
        this.transferService = transferService;
    }

    public Account createAccount(UUID ownerId, Currency currency) {
        if (!customerRepository.existsById(ownerId))
            throw new CustomerNotFoundException("Customer not found: " + ownerId);
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setOwnerId(ownerId);
        account.setCurrency(currency);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        return accountRepository.save(account);
    }

    public Transaction deposit(UUID accountId, BigDecimal amount, Currency currency) {
        Account account = findAccountOrThrow(accountId);
        if (account.getStatus() != AccountStatus.ACTIVE)
            throw new AccountNotOperableException("Account is not active: " + account.getStatus());
        if (account.getCurrency() != currency)
            throw new IllegalArgumentException("Currency mismatch: expected " + account.getCurrency());
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        return saveTransaction(accountId, TransactionType.DEPOSIT, amount, currency, "Deposit");
    }

    public Transaction withdraw(UUID accountId, BigDecimal amount, Currency currency) {
        Account account = findAccountOrThrow(accountId);
        if (account.getStatus() != AccountStatus.ACTIVE)
            throw new AccountNotOperableException("Account is not active: " + account.getStatus());
        if (account.getCurrency() != currency)
            throw new IllegalArgumentException("Currency mismatch: expected " + account.getCurrency());
        if (account.getBalance().compareTo(amount) < 0)
            throw new InsufficientFundsException("Insufficient funds");
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
        return saveTransaction(accountId, TransactionType.WITHDRAWAL, amount, currency, "Withdrawal");
    }

    public void transfer(UUID sourceId, UUID targetId, BigDecimal amount, Currency currency) {
        Account source = findAccountOrThrow(sourceId);
        Account target = findAccountOrThrow(targetId);
        if (source.getStatus() != AccountStatus.ACTIVE)
            throw new AccountNotOperableException("Source account is not active");
        if (target.getStatus() != AccountStatus.ACTIVE)
            throw new AccountNotOperableException("Target account is not active");
        if (source.getCurrency() != currency)
            throw new IllegalArgumentException("Currency mismatch with source account");
        if (target.getCurrency() != currency)
            throw new IllegalArgumentException("Currency mismatch with target account");

        boolean sameCustomer = source.getOwnerId().equals(target.getOwnerId());
        BigDecimal feePercent = getFeePercent();
        BigDecimal fee = transferService.calculateFee(amount, sameCustomer, feePercent);
        BigDecimal totalDebit = amount.add(fee);

        if (source.getBalance().compareTo(totalDebit) < 0)
            throw new InsufficientFundsException("Insufficient funds for transfer including fee");

        source.setBalance(source.getBalance().subtract(totalDebit));
        target.setBalance(target.getBalance().add(amount));
        accountRepository.save(source);
        accountRepository.save(target);

        String outDesc = "Transfer out to " + targetId +
                (fee.compareTo(BigDecimal.ZERO) > 0 ? " (fee: " + fee + ")" : "");
        saveTransaction(sourceId, TransactionType.TRANSFER_OUT, amount, currency, outDesc);
        saveTransaction(targetId, TransactionType.TRANSFER_IN, amount, currency,
                "Transfer in from " + sourceId);
    }

    @Transactional(readOnly = true)
    public Account getAccount(UUID accountId) {
        return findAccountOrThrow(accountId);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactions(UUID accountId) {
        findAccountOrThrow(accountId);
        return transactionRepository.findByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public List<Account> listAccounts(UUID ownerId) {
        if (!customerRepository.existsById(ownerId))
            throw new CustomerNotFoundException("Customer not found: " + ownerId);
        return accountRepository.findByOwnerId(ownerId);
    }

    public void freezeAccount(UUID accountId) {
        Account account = findAccountOrThrow(accountId);
        if (account.getStatus() != AccountStatus.ACTIVE)
            throw new AccountNotOperableException("Cannot freeze account with status: " + account.getStatus());
        account.setStatus(AccountStatus.FROZEN);
        accountRepository.save(account);
    }

    public void unfreezeAccount(UUID accountId) {
        Account account = findAccountOrThrow(accountId);
        if (account.getStatus() != AccountStatus.FROZEN)
            throw new AccountNotOperableException("Account is not frozen: " + account.getStatus());
        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);
    }

    public void closeAccount(UUID accountId) {
        Account account = findAccountOrThrow(accountId);
        if (account.getStatus() == AccountStatus.CLOSED)
            throw new AccountNotOperableException("Account is already closed");
        account.setStatus(AccountStatus.CLOSED);
        accountRepository.save(account);
    }

    public void setTransferFeePercent(BigDecimal feePercent) {
        Settings settings = settingsRepository.findById("TRANSFER_FEE_PERCENT")
                .orElseGet(() -> { Settings s = new Settings(); s.setKey("TRANSFER_FEE_PERCENT"); return s; });
        settings.setValue(feePercent.toPlainString());
        settingsRepository.save(settings);
    }

    private Account findAccountOrThrow(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
    }

    private Transaction saveTransaction(UUID accountId, TransactionType type,
                                        BigDecimal amount, Currency currency, String description) {
        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID());
        tx.setAccountId(accountId);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setCurrency(currency);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setDescription(description);
        return transactionRepository.save(tx);
    }

    private BigDecimal getFeePercent() {
        return settingsRepository.findById("TRANSFER_FEE_PERCENT")
                .map(s -> new BigDecimal(s.getValue()))
                .orElse(BigDecimal.ZERO);
    }
}
```

- [ ] **Step 4: Run tests — confirm they pass**

```bash
mvn test -Dtest=AccountServiceTest -q
```
Expected: `Tests run: 11, Failures: 0, Errors: 0`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/kaldiroglu/layered/ayvalikbank/service/AccountService.java \
        src/test/java/dev/kaldiroglu/layered/ayvalikbank/service/AccountServiceTest.java
git commit -m "feat: add AccountService with 11 Mockito tests"
```

---

## Task 9: Web DTOs (request + response) and GlobalExceptionHandler

**Files:**
- Create: `src/main/java/dev/kaldiroglu/layered/ayvalikbank/web/dto/request/` (6 files)
- Create: `src/main/java/dev/kaldiroglu/layered/ayvalikbank/web/dto/response/` (4 files)
- Create: `src/main/java/dev/kaldiroglu/layered/ayvalikbank/web/GlobalExceptionHandler.java`

- [ ] **Step 1: Create request DTOs**

`web/dto/request/CreateCustomerRequest.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateCustomerRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String password) {}
```

`web/dto/request/ChangePasswordRequest.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(@NotBlank String newPassword) {}
```

`web/dto/request/CreateAccountRequest.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.web.dto.request;

import dev.kaldiroglu.layered.ayvalikbank.model.Currency;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(@NotNull Currency currency) {}
```

`web/dto/request/MoneyOperationRequest.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.web.dto.request;

import dev.kaldiroglu.layered.ayvalikbank.model.Currency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record MoneyOperationRequest(
        @NotNull @Positive BigDecimal amount,
        @NotNull Currency currency) {}
```

`web/dto/request/SetTransferFeeRequest.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.web.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SetTransferFeeRequest(
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal feePercent) {}
```

`web/dto/request/TransferRequest.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.web.dto.request;

import dev.kaldiroglu.layered.ayvalikbank.model.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank String targetAccountId,
        @NotNull @Positive BigDecimal amount,
        @NotNull Currency currency) {}
```

- [ ] **Step 2: Create response DTOs**

`web/dto/response/CustomerResponse.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.web.dto.response;

import dev.kaldiroglu.layered.ayvalikbank.model.Customer;

import java.util.UUID;

public record CustomerResponse(UUID id, String name, String email, String role) {
    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(c.getId(), c.getName(), c.getEmail(), c.getRole());
    }
}
```

`web/dto/response/AccountResponse.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.web.dto.response;

import dev.kaldiroglu.layered.ayvalikbank.model.Account;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(UUID id, UUID ownerId, String currency, BigDecimal balance, String status) {
    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getId(), a.getOwnerId(),
                a.getCurrency().name(), a.getBalance(), a.getStatus().name());
    }
}
```

`web/dto/response/BalanceResponse.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.web.dto.response;

import dev.kaldiroglu.layered.ayvalikbank.model.Account;

import java.math.BigDecimal;

public record BalanceResponse(BigDecimal amount, String currency) {
    public static BalanceResponse from(Account a) {
        return new BalanceResponse(a.getBalance(), a.getCurrency().name());
    }
}
```

`web/dto/response/TransactionResponse.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.web.dto.response;

import dev.kaldiroglu.layered.ayvalikbank.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(UUID id, UUID accountId, String type,
                                   BigDecimal amount, String currency,
                                   LocalDateTime createdAt) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(t.getId(), t.getAccountId(),
                t.getType().name(), t.getAmount(),
                t.getCurrency().name(), t.getCreatedAt());
    }
}
```

- [ ] **Step 3: Create `GlobalExceptionHandler`**

`src/main/java/dev/kaldiroglu/layered/ayvalikbank/web/GlobalExceptionHandler.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.web;

import dev.kaldiroglu.layered.ayvalikbank.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomerNotFoundException.class)
    public ProblemDetail handleCustomerNotFound(CustomerNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleAccountNotFound(AccountNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ProblemDetail handleInsufficientFunds(InsufficientFundsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ProblemDetail handleInvalidPassword(InvalidPasswordException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(PasswordReusedException.class)
    public ProblemDetail handlePasswordReused(PasswordReusedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AccountNotOperableException.class)
    public ProblemDetail handleAccountNotOperable(AccountNotOperableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ProblemDetail handleUnauthorized(UnauthorizedAccessException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "; " + b);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }
}
```

- [ ] **Step 4: Verify compilation**

```bash
mvn compile -q
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/kaldiroglu/layered/ayvalikbank/web/
git commit -m "feat: add web DTOs and GlobalExceptionHandler"
```

---

## Task 10: Security config and admin seeder

**Files:**
- Create: `src/main/java/dev/kaldiroglu/layered/ayvalikbank/config/SecurityConfig.java`
- Create: `src/main/java/dev/kaldiroglu/layered/ayvalikbank/config/BankUserDetailsService.java`
- Create: `src/main/java/dev/kaldiroglu/layered/ayvalikbank/config/AdminDataInitializer.java`

- [ ] **Step 1: Create `SecurityConfig`**

`src/main/java/dev/kaldiroglu/layered/ayvalikbank/config/SecurityConfig.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final BankUserDetailsService userDetailsService;

    public SecurityConfig(BankUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/customers/**").hasRole("CUSTOMER")
                .requestMatchers("/api/accounts/**").hasRole("CUSTOMER")
                .anyRequest().authenticated())
            .httpBasic(basic -> {});
        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
}
```

- [ ] **Step 2: Create `BankUserDetailsService`**

`src/main/java/dev/kaldiroglu/layered/ayvalikbank/config/BankUserDetailsService.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.config;

import dev.kaldiroglu.layered.ayvalikbank.repository.CustomerRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BankUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    public BankUserDetailsService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return new User(
                customer.getEmail(),
                customer.getCurrentPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + customer.getRole())));
    }
}
```

- [ ] **Step 3: Create `AdminDataInitializer`**

`src/main/java/dev/kaldiroglu/layered/ayvalikbank/config/AdminDataInitializer.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.config;

import dev.kaldiroglu.layered.ayvalikbank.model.Customer;
import dev.kaldiroglu.layered.ayvalikbank.repository.CustomerRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AdminDataInitializer implements ApplicationRunner {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminDataInitializer(CustomerRepository customerRepository,
                                PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (customerRepository.findByEmail("admin@ayvalikbank.dev").isEmpty()) {
            Customer admin = new Customer();
            admin.setId(UUID.randomUUID());
            admin.setName("Admin");
            admin.setEmail("admin@ayvalikbank.dev");
            admin.setRole("ADMIN");
            admin.setCurrentPassword(passwordEncoder.encode("Admin@123!"));
            customerRepository.save(admin);
        }
    }
}
```

- [ ] **Step 4: Verify compilation**

```bash
mvn compile -q
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/kaldiroglu/layered/ayvalikbank/config/
git commit -m "feat: add security config, user details service, admin seeder"
```

---

## Task 11: AdminController (TDD — @WebMvcTest)

**Files:**
- Create: `src/main/java/dev/kaldiroglu/layered/ayvalikbank/web/controller/AdminController.java`
- Create: `src/test/java/dev/kaldiroglu/layered/ayvalikbank/web/controller/AdminControllerTest.java`

- [ ] **Step 1: Create stub `AdminController` so tests compile**

`src/main/java/dev/kaldiroglu/layered/ayvalikbank/web/controller/AdminController.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.web.controller;

import dev.kaldiroglu.layered.ayvalikbank.service.AccountService;
import dev.kaldiroglu.layered.ayvalikbank.service.CustomerService;
import dev.kaldiroglu.layered.ayvalikbank.web.dto.request.CreateCustomerRequest;
import dev.kaldiroglu.layered.ayvalikbank.web.dto.request.SetTransferFeeRequest;
import dev.kaldiroglu.layered.ayvalikbank.web.dto.response.CustomerResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CustomerService customerService;
    private final AccountService accountService;

    public AdminController(CustomerService customerService, AccountService accountService) {
        this.customerService = customerService;
        this.accountService = accountService;
    }

    @PostMapping("/customers")
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        var customer = customerService.createCustomer(
                request.name(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(CustomerResponse.from(customer));
    }

    @DeleteMapping("/customers/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable UUID id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerResponse>> listCustomers() {
        var list = customerService.listCustomers().stream()
                .map(CustomerResponse::from).toList();
        return ResponseEntity.ok(list);
    }

    @PutMapping("/settings/transfer-fee")
    public ResponseEntity<Void> setTransferFee(
            @Valid @RequestBody SetTransferFeeRequest request) {
        accountService.setTransferFeePercent(request.feePercent());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/accounts/{id}/freeze")
    public ResponseEntity<Void> freezeAccount(@PathVariable UUID id) {
        accountService.freezeAccount(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/accounts/{id}/unfreeze")
    public ResponseEntity<Void> unfreezeAccount(@PathVariable UUID id) {
        accountService.unfreezeAccount(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/accounts/{id}/close")
    public ResponseEntity<Void> closeAccount(@PathVariable UUID id) {
        accountService.closeAccount(id);
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 2: Write the failing tests**

`src/test/java/dev/kaldiroglu/layered/ayvalikbank/web/controller/AdminControllerTest.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kaldiroglu.layered.ayvalikbank.config.BankUserDetailsService;
import dev.kaldiroglu.layered.ayvalikbank.config.SecurityConfig;
import dev.kaldiroglu.layered.ayvalikbank.exception.AccountNotOperableException;
import dev.kaldiroglu.layered.ayvalikbank.exception.CustomerNotFoundException;
import dev.kaldiroglu.layered.ayvalikbank.model.Customer;
import dev.kaldiroglu.layered.ayvalikbank.service.AccountService;
import dev.kaldiroglu.layered.ayvalikbank.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean BankUserDetailsService userDetailsService;
    @MockitoBean CustomerService customerService;
    @MockitoBean AccountService accountService;

    private Customer stubCustomer(String name, String email) {
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setName(name);
        c.setEmail(email);
        c.setRole("CUSTOMER");
        c.setCurrentPassword("hash");
        return c;
    }

    @Test @WithMockUser(roles = "ADMIN")
    void createCustomer_returnsCreated() throws Exception {
        when(customerService.createCustomer(any(), any(), any()))
                .thenReturn(stubCustomer("Alice", "alice@test.com"));

        mockMvc.perform(post("/api/admin/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice","email":"alice@test.com","password":"Valid@123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test @WithMockUser(roles = "ADMIN")
    void createCustomer_returnsBadRequestOnMissingName() throws Exception {
        mockMvc.perform(post("/api/admin/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@test.com","password":"Valid@123"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(customerService);
    }

    @Test @WithMockUser(roles = "ADMIN")
    void createCustomer_returnsBadRequestOnInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/admin/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice","email":"not-an-email","password":"Valid@123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "CUSTOMER")
    void createCustomer_returnsForbiddenForCustomerRole() throws Exception {
        mockMvc.perform(post("/api/admin/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice","email":"alice@test.com","password":"Valid@123"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCustomer_returnsUnauthorizedWithoutCredentials() throws Exception {
        mockMvc.perform(post("/api/admin/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice","email":"alice@test.com","password":"Valid@123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void deleteCustomer_returnsNoContent() throws Exception {
        doNothing().when(customerService).deleteCustomer(any());
        mockMvc.perform(delete("/api/admin/customers/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void deleteCustomer_returnsNotFoundWhenMissing() throws Exception {
        doThrow(new CustomerNotFoundException("Customer not found"))
                .when(customerService).deleteCustomer(any());
        mockMvc.perform(delete("/api/admin/customers/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void listCustomers_returnsOkWithList() throws Exception {
        when(customerService.listCustomers()).thenReturn(List.of(
                stubCustomer("Alice", "alice@test.com"),
                stubCustomer("Bob", "bob@test.com")));
        mockMvc.perform(get("/api/admin/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value("alice@test.com"));
    }

    @Test @WithMockUser(roles = "ADMIN")
    void listCustomers_returnsEmptyList() throws Exception {
        when(customerService.listCustomers()).thenReturn(List.of());
        mockMvc.perform(get("/api/admin/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test @WithMockUser(roles = "ADMIN")
    void setTransferFee_returnsOk() throws Exception {
        doNothing().when(accountService).setTransferFeePercent(any());
        mockMvc.perform(put("/api/admin/settings/transfer-fee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feePercent":1.5}
                                """))
                .andExpect(status().isOk());
        verify(accountService).setTransferFeePercent(any());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void setTransferFee_returnsBadRequestForNegativeValue() throws Exception {
        mockMvc.perform(put("/api/admin/settings/transfer-fee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feePercent":-1.0}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(accountService);
    }

    @Test @WithMockUser(roles = "ADMIN")
    void setTransferFee_returnsBadRequestForValueAbove100() throws Exception {
        mockMvc.perform(put("/api/admin/settings/transfer-fee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feePercent":101.0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void freezeAccount_returnsOk() throws Exception {
        doNothing().when(accountService).freezeAccount(any());
        mockMvc.perform(put("/api/admin/accounts/{id}/freeze", UUID.randomUUID()))
                .andExpect(status().isOk());
        verify(accountService).freezeAccount(any());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void freezeAccount_returnsUnprocessableEntityForInvalidTransition() throws Exception {
        doThrow(new AccountNotOperableException("Already frozen"))
                .when(accountService).freezeAccount(any());
        mockMvc.perform(put("/api/admin/accounts/{id}/freeze", UUID.randomUUID()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void unfreezeAccount_returnsOk() throws Exception {
        doNothing().when(accountService).unfreezeAccount(any());
        mockMvc.perform(put("/api/admin/accounts/{id}/unfreeze", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void unfreezeAccount_returnsUnprocessableEntityForInvalidTransition() throws Exception {
        doThrow(new AccountNotOperableException("Not frozen"))
                .when(accountService).unfreezeAccount(any());
        mockMvc.perform(put("/api/admin/accounts/{id}/unfreeze", UUID.randomUUID()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void closeAccount_returnsOk() throws Exception {
        doNothing().when(accountService).closeAccount(any());
        mockMvc.perform(put("/api/admin/accounts/{id}/close", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void closeAccount_returnsUnprocessableEntityForAlreadyClosed() throws Exception {
        doThrow(new AccountNotOperableException("Already closed"))
                .when(accountService).closeAccount(any());
        mockMvc.perform(put("/api/admin/accounts/{id}/close", UUID.randomUUID()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test @WithMockUser(roles = "CUSTOMER")
    void closeAccount_returnsForbiddenForCustomerRole() throws Exception {
        mockMvc.perform(put("/api/admin/accounts/{id}/close", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 3: Run tests — confirm they fail (no implementation yet, just stubs)**

```bash
mvn test -Dtest=AdminControllerTest -q 2>&1 | tail -8
```
Expected: some tests fail because controller methods return stubs (or all pass if stubs happen to match — in that case proceed).

- [ ] **Step 4: Run tests — confirm all pass (controller is already implemented in Step 1)**

```bash
mvn test -Dtest=AdminControllerTest -q
```
Expected: `Tests run: 19, Failures: 0, Errors: 0`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/kaldiroglu/layered/ayvalikbank/web/controller/AdminController.java \
        src/test/java/dev/kaldiroglu/layered/ayvalikbank/web/controller/AdminControllerTest.java
git commit -m "feat: add AdminController with 19 @WebMvcTest tests"
```

---

## Task 12: CustomerController (TDD — @WebMvcTest)

**Files:**
- Create: `src/main/java/dev/kaldiroglu/layered/ayvalikbank/web/controller/CustomerController.java`
- Create: `src/test/java/dev/kaldiroglu/layered/ayvalikbank/web/controller/CustomerControllerTest.java`

- [ ] **Step 1: Create `CustomerController`**

`src/main/java/dev/kaldiroglu/layered/ayvalikbank/web/controller/CustomerController.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.web.controller;

import dev.kaldiroglu.layered.ayvalikbank.service.CustomerService;
import dev.kaldiroglu.layered.ayvalikbank.web.dto.request.ChangePasswordRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(@PathVariable UUID id,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        customerService.changePassword(id, request.newPassword());
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 2: Write the failing tests**

`src/test/java/dev/kaldiroglu/layered/ayvalikbank/web/controller/CustomerControllerTest.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.web.controller;

import dev.kaldiroglu.layered.ayvalikbank.config.BankUserDetailsService;
import dev.kaldiroglu.layered.ayvalikbank.config.SecurityConfig;
import dev.kaldiroglu.layered.ayvalikbank.exception.*;
import dev.kaldiroglu.layered.ayvalikbank.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CustomerController.class)
@Import(SecurityConfig.class)
class CustomerControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean BankUserDetailsService userDetailsService;
    @MockitoBean CustomerService customerService;

    @Test @WithMockUser(roles = "CUSTOMER")
    void changePassword_returnsOk() throws Exception {
        doNothing().when(customerService).changePassword(any(), any());
        mockMvc.perform(put("/api/customers/{id}/password", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPassword":"Valid@123"}
                                """))
                .andExpect(status().isOk());
        verify(customerService).changePassword(any(), any());
    }

    @Test @WithMockUser(roles = "CUSTOMER")
    void changePassword_returnsBadRequestOnBlankPassword() throws Exception {
        mockMvc.perform(put("/api/customers/{id}/password", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPassword":""}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(customerService);
    }

    @Test @WithMockUser(roles = "CUSTOMER")
    void changePassword_returnsBadRequestOnWeakPassword() throws Exception {
        doThrow(new InvalidPasswordException("Password must contain uppercase"))
                .when(customerService).changePassword(any(), any());
        mockMvc.perform(put("/api/customers/{id}/password", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPassword":"weak"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "CUSTOMER")
    void changePassword_returnsConflictOnPasswordReuse() throws Exception {
        doThrow(new PasswordReusedException("Password has been used recently"))
                .when(customerService).changePassword(any(), any());
        mockMvc.perform(put("/api/customers/{id}/password", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPassword":"Valid@123"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser(roles = "CUSTOMER")
    void changePassword_returnsNotFoundForUnknownCustomer() throws Exception {
        doThrow(new CustomerNotFoundException("Customer not found"))
                .when(customerService).changePassword(any(), any());
        mockMvc.perform(put("/api/customers/{id}/password", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPassword":"Valid@123"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void changePassword_returnsForbiddenForAdminRole() throws Exception {
        mockMvc.perform(put("/api/customers/{id}/password", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPassword":"Valid@123"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void changePassword_returnsUnauthorizedWithoutCredentials() throws Exception {
        mockMvc.perform(put("/api/customers/{id}/password", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPassword":"Valid@123"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 3: Run tests — confirm they pass**

```bash
mvn test -Dtest=CustomerControllerTest -q
```
Expected: `Tests run: 7, Failures: 0, Errors: 0`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/dev/kaldiroglu/layered/ayvalikbank/web/controller/CustomerController.java \
        src/test/java/dev/kaldiroglu/layered/ayvalikbank/web/controller/CustomerControllerTest.java
git commit -m "feat: add CustomerController with 7 @WebMvcTest tests"
```

---

## Task 13: AccountController (TDD — @WebMvcTest)

**Files:**
- Create: `src/main/java/dev/kaldiroglu/layered/ayvalikbank/web/controller/AccountController.java`
- Create: `src/test/java/dev/kaldiroglu/layered/ayvalikbank/web/controller/AccountControllerTest.java`

- [ ] **Step 1: Create `AccountController`**

`src/main/java/dev/kaldiroglu/layered/ayvalikbank/web/controller/AccountController.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.web.controller;

import dev.kaldiroglu.layered.ayvalikbank.service.AccountService;
import dev.kaldiroglu.layered.ayvalikbank.web.dto.request.CreateAccountRequest;
import dev.kaldiroglu.layered.ayvalikbank.web.dto.request.MoneyOperationRequest;
import dev.kaldiroglu.layered.ayvalikbank.web.dto.request.TransferRequest;
import dev.kaldiroglu.layered.ayvalikbank.web.dto.response.AccountResponse;
import dev.kaldiroglu.layered.ayvalikbank.web.dto.response.BalanceResponse;
import dev.kaldiroglu.layered.ayvalikbank.web.dto.response.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> createAccount(
            @RequestParam UUID ownerId,
            @Valid @RequestBody CreateAccountRequest request) {
        var account = accountService.createAccount(ownerId, request.currency());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @GetMapping("/customers/{customerId}/accounts")
    public ResponseEntity<List<AccountResponse>> listAccounts(@PathVariable UUID customerId) {
        var accounts = accountService.listAccounts(customerId).stream()
                .map(AccountResponse::from).toList();
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/accounts/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable UUID accountId) {
        return ResponseEntity.ok(BalanceResponse.from(accountService.getAccount(accountId)));
    }

    @PostMapping("/accounts/{accountId}/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable UUID accountId,
            @Valid @RequestBody MoneyOperationRequest request) {
        var tx = accountService.deposit(accountId, request.amount(), request.currency());
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(tx));
    }

    @PostMapping("/accounts/{accountId}/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @PathVariable UUID accountId,
            @Valid @RequestBody MoneyOperationRequest request) {
        var tx = accountService.withdraw(accountId, request.amount(), request.currency());
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(tx));
    }

    @PostMapping("/accounts/{accountId}/transfer")
    public ResponseEntity<Void> transfer(
            @PathVariable UUID accountId,
            @Valid @RequestBody TransferRequest request) {
        accountService.transfer(accountId, UUID.fromString(request.targetAccountId()),
                request.amount(), request.currency());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(@PathVariable UUID accountId) {
        var txs = accountService.getTransactions(accountId).stream()
                .map(TransactionResponse::from).toList();
        return ResponseEntity.ok(txs);
    }
}
```

- [ ] **Step 2: Write the failing tests**

`src/test/java/dev/kaldiroglu/layered/ayvalikbank/web/controller/AccountControllerTest.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.web.controller;

import dev.kaldiroglu.layered.ayvalikbank.config.BankUserDetailsService;
import dev.kaldiroglu.layered.ayvalikbank.config.SecurityConfig;
import dev.kaldiroglu.layered.ayvalikbank.exception.*;
import dev.kaldiroglu.layered.ayvalikbank.model.*;
import dev.kaldiroglu.layered.ayvalikbank.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AccountController.class)
@Import(SecurityConfig.class)
class AccountControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean BankUserDetailsService userDetailsService;
    @MockitoBean AccountService accountService;

    private Account stubAccount(UUID ownerId, Currency currency) {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        a.setOwnerId(ownerId);
        a.setCurrency(currency);
        a.setBalance(BigDecimal.ZERO);
        a.setStatus(AccountStatus.ACTIVE);
        return a;
    }

    private Transaction stubTransaction(UUID accountId, TransactionType type,
                                        BigDecimal amount, Currency currency) {
        Transaction t = new Transaction();
        t.setId(UUID.randomUUID());
        t.setAccountId(accountId);
        t.setType(type);
        t.setAmount(amount);
        t.setCurrency(currency);
        t.setCreatedAt(LocalDateTime.now());
        t.setDescription("desc");
        return t;
    }

    // ── POST /api/accounts ────────────────────────────────────────────────

    @Test @WithMockUser(roles = "CUSTOMER")
    void createAccount_returnsCreated() throws Exception {
        UUID ownerId = UUID.randomUUID();
        when(accountService.createAccount(any(), any())).thenReturn(stubAccount(ownerId, Currency.USD));

        mockMvc.perform(post("/api/accounts")
                        .param("ownerId", ownerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currency":"USD"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test @WithMockUser(roles = "CUSTOMER")
    void createAccount_returnsBadRequestOnMissingCurrency() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .param("ownerId", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(accountService);
    }

    @Test @WithMockUser(roles = "ADMIN")
    void createAccount_returnsForbiddenForAdminRole() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .param("ownerId", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currency":"USD"}
                                """))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/customers/{id}/accounts ─────────────────────────────────

    @Test @WithMockUser(roles = "CUSTOMER")
    void listAccounts_returnsOkWithList() throws Exception {
        UUID ownerId = UUID.randomUUID();
        when(accountService.listAccounts(any())).thenReturn(List.of(
                stubAccount(ownerId, Currency.USD),
                stubAccount(ownerId, Currency.EUR)));

        mockMvc.perform(get("/api/customers/{id}/accounts", ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].currency").value("USD"))
                .andExpect(jsonPath("$[1].currency").value("EUR"));
    }

    // ── GET /api/accounts/{id}/balance ───────────────────────────────────

    @Test @WithMockUser(roles = "CUSTOMER")
    void getBalance_returnsOk() throws Exception {
        Account a = stubAccount(UUID.randomUUID(), Currency.USD);
        a.setBalance(new BigDecimal("250.00"));
        when(accountService.getAccount(any())).thenReturn(a);

        mockMvc.perform(get("/api/accounts/{id}/balance", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(250.0))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test @WithMockUser(roles = "CUSTOMER")
    void getBalance_returnsNotFoundForUnknownAccount() throws Exception {
        when(accountService.getAccount(any()))
                .thenThrow(new AccountNotFoundException("Account not found"));

        mockMvc.perform(get("/api/accounts/{id}/balance", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/accounts/{id}/deposit ──────────────────────────────────

    @Test @WithMockUser(roles = "CUSTOMER")
    void deposit_returnsCreated() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountService.deposit(any(), any(), any()))
                .thenReturn(stubTransaction(accountId, TransactionType.DEPOSIT,
                        new BigDecimal("100"), Currency.USD));

        mockMvc.perform(post("/api/accounts/{id}/deposit", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100,"currency":"USD"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DEPOSIT"));
    }

    @Test @WithMockUser(roles = "CUSTOMER")
    void deposit_returnsBadRequestOnNegativeAmount() throws Exception {
        mockMvc.perform(post("/api/accounts/{id}/deposit", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":-50,"currency":"USD"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(accountService);
    }

    @Test @WithMockUser(roles = "CUSTOMER")
    void deposit_returnsNotFoundForUnknownAccount() throws Exception {
        when(accountService.deposit(any(), any(), any()))
                .thenThrow(new AccountNotFoundException("Account not found"));

        mockMvc.perform(post("/api/accounts/{id}/deposit", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100,"currency":"USD"}
                                """))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/accounts/{id}/withdraw ─────────────────────────────────

    @Test @WithMockUser(roles = "CUSTOMER")
    void withdraw_returnsCreated() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountService.withdraw(any(), any(), any()))
                .thenReturn(stubTransaction(accountId, TransactionType.WITHDRAWAL,
                        new BigDecimal("50"), Currency.USD));

        mockMvc.perform(post("/api/accounts/{id}/withdraw", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":50,"currency":"USD"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"));
    }

    @Test @WithMockUser(roles = "CUSTOMER")
    void withdraw_returnsUnprocessableEntityOnInsufficientFunds() throws Exception {
        when(accountService.withdraw(any(), any(), any()))
                .thenThrow(new InsufficientFundsException("Insufficient funds"));

        mockMvc.perform(post("/api/accounts/{id}/withdraw", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":999,"currency":"USD"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── POST /api/accounts/{id}/transfer ─────────────────────────────────

    @Test @WithMockUser(roles = "CUSTOMER")
    void transfer_returnsOk() throws Exception {
        doNothing().when(accountService).transfer(any(), any(), any(), any());

        mockMvc.perform(post("/api/accounts/{id}/transfer", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetAccountId":"%s","amount":100,"currency":"USD"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk());
        verify(accountService).transfer(any(), any(), any(), any());
    }

    @Test @WithMockUser(roles = "CUSTOMER")
    void transfer_returnsBadRequestOnMissingTarget() throws Exception {
        mockMvc.perform(post("/api/accounts/{id}/transfer", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100,"currency":"USD"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(accountService);
    }

    @Test @WithMockUser(roles = "CUSTOMER")
    void transfer_returnsUnprocessableEntityOnInsufficientFunds() throws Exception {
        doThrow(new InsufficientFundsException("Insufficient funds"))
                .when(accountService).transfer(any(), any(), any(), any());

        mockMvc.perform(post("/api/accounts/{id}/transfer", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetAccountId":"%s","amount":9999,"currency":"USD"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── GET /api/accounts/{id}/transactions ──────────────────────────────

    @Test @WithMockUser(roles = "CUSTOMER")
    void getTransactions_returnsOkWithList() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountService.getTransactions(any())).thenReturn(List.of(
                stubTransaction(accountId, TransactionType.DEPOSIT, new BigDecimal("100"), Currency.USD),
                stubTransaction(accountId, TransactionType.WITHDRAWAL, new BigDecimal("50"), Currency.USD)));

        mockMvc.perform(get("/api/accounts/{id}/transactions", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[1].type").value("WITHDRAWAL"));
    }

    @Test @WithMockUser(roles = "CUSTOMER")
    void getTransactions_returnsNotFoundForUnknownAccount() throws Exception {
        when(accountService.getTransactions(any()))
                .thenThrow(new AccountNotFoundException("Account not found"));

        mockMvc.perform(get("/api/accounts/{id}/transactions", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTransactions_returnsUnauthorizedWithoutCredentials() throws Exception {
        mockMvc.perform(get("/api/accounts/{id}/transactions", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 3: Run tests — confirm they pass**

```bash
mvn test -Dtest=AccountControllerTest -q
```
Expected: `Tests run: 17, Failures: 0, Errors: 0`.

- [ ] **Step 4: Run the full test suite so far**

```bash
mvn test -q
```
Expected: `Tests run: 71, Failures: 0, Errors: 0` (8 + 3 + 6 + 11 + 7 + 19 + 17).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/kaldiroglu/layered/ayvalikbank/web/controller/AccountController.java \
        src/test/java/dev/kaldiroglu/layered/ayvalikbank/web/controller/AccountControllerTest.java
git commit -m "feat: add AccountController with 17 @WebMvcTest tests"
```

---

## Task 14: Repository integration tests (@DataJpaTest + H2)

**Files:**
- Create: `src/test/java/dev/kaldiroglu/layered/ayvalikbank/repository/CustomerRepositoryTest.java`
- Create: `src/test/java/dev/kaldiroglu/layered/ayvalikbank/repository/AccountRepositoryTest.java`

- [ ] **Step 1: Write `CustomerRepositoryTest`**

`src/test/java/dev/kaldiroglu/layered/ayvalikbank/repository/CustomerRepositoryTest.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.repository;

import dev.kaldiroglu.layered.ayvalikbank.model.Customer;
import dev.kaldiroglu.layered.ayvalikbank.model.PasswordHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class CustomerRepositoryTest {

    @Autowired CustomerRepository customerRepository;

    private Customer makeCustomer(String name, String email) {
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setName(name);
        c.setEmail(email);
        c.setRole("CUSTOMER");
        c.setCurrentPassword("hash");
        return c;
    }

    @Test
    void shouldFindCustomerByEmail() {
        customerRepository.save(makeCustomer("Alice", "alice@test.com"));

        var found = customerRepository.findByEmail("alice@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Alice");
    }

    @Test
    void shouldReturnEmptyForUnknownEmail() {
        var found = customerRepository.findByEmail("nobody@test.com");
        assertThat(found).isEmpty();
    }

    @Test
    void shouldCascadeDeletePasswordHistoryOnCustomerDelete() {
        Customer c = makeCustomer("Bob", "bob@test.com");
        PasswordHistory ph = new PasswordHistory();
        ph.setId(UUID.randomUUID());
        ph.setCustomer(c);
        ph.setHashedPassword("old-hash");
        ph.setPosition(0);
        c.getPasswordHistory().add(ph);
        customerRepository.save(c);

        UUID customerId = c.getId();
        customerRepository.deleteById(customerId);

        assertThat(customerRepository.findById(customerId)).isEmpty();
    }

    @Test
    void shouldEnforceUniqueEmail() {
        customerRepository.save(makeCustomer("A", "dup@test.com"));
        Customer dup = makeCustomer("B", "dup@test.com");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            customerRepository.saveAndFlush(dup);
        }).isInstanceOf(Exception.class);
    }
}
```

- [ ] **Step 2: Write `AccountRepositoryTest`**

`src/test/java/dev/kaldiroglu/layered/ayvalikbank/repository/AccountRepositoryTest.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.repository;

import dev.kaldiroglu.layered.ayvalikbank.model.Account;
import dev.kaldiroglu.layered.ayvalikbank.model.AccountStatus;
import dev.kaldiroglu.layered.ayvalikbank.model.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class AccountRepositoryTest {

    @Autowired AccountRepository accountRepository;

    private Account makeAccount(UUID ownerId, Currency currency) {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        a.setOwnerId(ownerId);
        a.setCurrency(currency);
        a.setBalance(BigDecimal.ZERO);
        a.setStatus(AccountStatus.ACTIVE);
        return a;
    }

    @Test
    void shouldFindAccountsByOwnerId() {
        UUID ownerId = UUID.randomUUID();
        accountRepository.saveAll(List.of(
                makeAccount(ownerId, Currency.USD),
                makeAccount(ownerId, Currency.EUR),
                makeAccount(UUID.randomUUID(), Currency.TRY)));

        List<Account> accounts = accountRepository.findByOwnerId(ownerId);

        assertThat(accounts).hasSize(2);
        assertThat(accounts).extracting(Account::getOwnerId).containsOnly(ownerId);
    }

    @Test
    void shouldReturnEmptyListWhenNoAccountsForOwner() {
        List<Account> accounts = accountRepository.findByOwnerId(UUID.randomUUID());
        assertThat(accounts).isEmpty();
    }

    @Test
    void shouldPersistEnumFieldsCorrectly() {
        UUID ownerId = UUID.randomUUID();
        Account saved = accountRepository.save(makeAccount(ownerId, Currency.EUR));

        Account loaded = accountRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getCurrency()).isEqualTo(Currency.EUR);
        assertThat(loaded.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }
}
```

- [ ] **Step 3: Run integration tests**

```bash
mvn test -Dtest="CustomerRepositoryTest,AccountRepositoryTest" -q
```
Expected: `Tests run: 7, Failures: 0, Errors: 0`.

- [ ] **Step 4: Commit**

```bash
git add src/test/java/dev/kaldiroglu/layered/ayvalikbank/repository/
git commit -m "test: add @DataJpaTest repository integration tests"
```

---

## Task 15: End-to-end tests (@SpringBootTest + MockMvc + H2)

**Files:**
- Create: `src/test/java/dev/kaldiroglu/layered/ayvalikbank/e2e/CustomerE2ETest.java`
- Create: `src/test/java/dev/kaldiroglu/layered/ayvalikbank/e2e/AccountE2ETest.java`

- [ ] **Step 1: Write `CustomerE2ETest`**

`src/test/java/dev/kaldiroglu/layered/ayvalikbank/e2e/CustomerE2ETest.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.e2e;

import dev.kaldiroglu.layered.ayvalikbank.model.Account;
import dev.kaldiroglu.layered.ayvalikbank.model.Customer;
import dev.kaldiroglu.layered.ayvalikbank.repository.AccountRepository;
import dev.kaldiroglu.layered.ayvalikbank.repository.CustomerRepository;
import dev.kaldiroglu.layered.ayvalikbank.repository.SettingsRepository;
import dev.kaldiroglu.layered.ayvalikbank.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class CustomerE2ETest {

    @Autowired MockMvc mockMvc;
    @Autowired CustomerRepository customerRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired SettingsRepository settingsRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String adminAuth;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        customerRepository.deleteAll();

        Customer admin = new Customer();
        admin.setId(UUID.randomUUID());
        admin.setName("Admin");
        admin.setEmail("admin@ayvalikbank.dev");
        admin.setRole("ADMIN");
        admin.setCurrentPassword(passwordEncoder.encode("Admin@123!"));
        customerRepository.save(admin);

        adminAuth = "Basic " + Base64.getEncoder()
                .encodeToString("admin@ayvalikbank.dev:Admin@123!".getBytes());
    }

    @Test
    void shouldCreateCustomerAndReturnCreated() throws Exception {
        mockMvc.perform(post("/api/admin/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", adminAuth)
                        .content("""
                                {"name":"Alice","email":"alice@test.com","password":"Valid@123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        assertThat(customerRepository.findByEmail("alice@test.com")).isPresent();
    }

    @Test
    void shouldDeleteCustomerAndReturnNoContent() throws Exception {
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setName("ToDelete");
        c.setEmail("delete@test.com");
        c.setRole("CUSTOMER");
        c.setCurrentPassword(passwordEncoder.encode("Valid@123"));
        customerRepository.save(c);

        mockMvc.perform(delete("/api/admin/customers/{id}", c.getId())
                        .header("Authorization", adminAuth))
                .andExpect(status().isNoContent());

        assertThat(customerRepository.findById(c.getId())).isEmpty();
    }

    @Test
    void shouldListCustomers() throws Exception {
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setName("Listed");
        c.setEmail("listed@test.com");
        c.setRole("CUSTOMER");
        c.setCurrentPassword(passwordEncoder.encode("Valid@123"));
        customerRepository.save(c);

        mockMvc.perform(get("/api/admin/customers")
                        .header("Authorization", adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)); // admin + listed
    }

    @Test
    void shouldReturn401WithoutCredentials() throws Exception {
        mockMvc.perform(get("/api/admin/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenCustomerAccessesAdminEndpoint() throws Exception {
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setName("Regular");
        c.setEmail("regular@test.com");
        c.setRole("CUSTOMER");
        c.setCurrentPassword(passwordEncoder.encode("Valid@123"));
        customerRepository.save(c);

        String customerAuth = "Basic " + Base64.getEncoder()
                .encodeToString("regular@test.com:Valid@123".getBytes());

        mockMvc.perform(get("/api/admin/customers")
                        .header("Authorization", customerAuth))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2: Write `AccountE2ETest`**

`src/test/java/dev/kaldiroglu/layered/ayvalikbank/e2e/AccountE2ETest.java`:
```java
package dev.kaldiroglu.layered.ayvalikbank.e2e;

import dev.kaldiroglu.layered.ayvalikbank.model.*;
import dev.kaldiroglu.layered.ayvalikbank.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class AccountE2ETest {

    @Autowired MockMvc mockMvc;
    @Autowired CustomerRepository customerRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired SettingsRepository settingsRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private Customer customer;
    private String customerAuth;
    private String adminAuth;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        customerRepository.deleteAll();

        Customer admin = new Customer();
        admin.setId(UUID.randomUUID());
        admin.setName("Admin");
        admin.setEmail("admin@ayvalikbank.dev");
        admin.setRole("ADMIN");
        admin.setCurrentPassword(passwordEncoder.encode("Admin@123!"));
        customerRepository.save(admin);

        customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setName("Alice");
        customer.setEmail("alice@test.com");
        customer.setRole("CUSTOMER");
        customer.setCurrentPassword(passwordEncoder.encode("Valid@123"));
        customerRepository.save(customer);

        customerAuth = "Basic " + Base64.getEncoder()
                .encodeToString("alice@test.com:Valid@123".getBytes());
        adminAuth = "Basic " + Base64.getEncoder()
                .encodeToString("admin@ayvalikbank.dev:Admin@123!".getBytes());

        // Seed transfer fee
        Settings settings = new Settings();
        settings.setKey("TRANSFER_FEE_PERCENT");
        settings.setValue("1.0");
        settingsRepository.save(settings);
    }

    @Test
    void shouldCreateAccountDepositAndCheckBalance() throws Exception {
        // Create account
        String createResponse = mockMvc.perform(post("/api/accounts")
                        .param("ownerId", customer.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", customerAuth)
                        .content("""
                                {"currency":"USD"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.balance").value(0))
                .andReturn().getResponse().getContentAsString();

        String accountId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(createResponse).get("id").asText();

        // Deposit
        mockMvc.perform(post("/api/accounts/{id}/deposit", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", customerAuth)
                        .content("""
                                {"amount":500,"currency":"USD"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DEPOSIT"));

        // Check balance
        mockMvc.perform(get("/api/accounts/{id}/balance", accountId)
                        .header("Authorization", customerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(500.0))
                .andExpect(jsonPath("$.currency").value("USD"));

        Account saved = accountRepository.findById(UUID.fromString(accountId)).orElseThrow();
        assertThat(saved.getBalance()).isEqualByComparingTo("500.00");
    }

    @Test
    void shouldFreezeAndUnfreezeAccount() throws Exception {
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setOwnerId(customer.getId());
        account.setCurrency(Currency.USD);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);

        mockMvc.perform(put("/api/admin/accounts/{id}/freeze", account.getId())
                        .header("Authorization", adminAuth))
                .andExpect(status().isOk());

        assertThat(accountRepository.findById(account.getId()).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.FROZEN);

        mockMvc.perform(put("/api/admin/accounts/{id}/unfreeze", account.getId())
                        .header("Authorization", adminAuth))
                .andExpect(status().isOk());

        assertThat(accountRepository.findById(account.getId()).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void shouldTransferWithFeeForDifferentCustomers() throws Exception {
        Customer other = new Customer();
        other.setId(UUID.randomUUID());
        other.setName("Bob");
        other.setEmail("bob@test.com");
        other.setRole("CUSTOMER");
        other.setCurrentPassword(passwordEncoder.encode("Valid@123"));
        customerRepository.save(other);

        Account source = new Account();
        source.setId(UUID.randomUUID());
        source.setOwnerId(customer.getId());
        source.setCurrency(Currency.USD);
        source.setBalance(new BigDecimal("1000"));
        source.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(source);

        Account target = new Account();
        target.setId(UUID.randomUUID());
        target.setOwnerId(other.getId());
        target.setCurrency(Currency.USD);
        target.setBalance(BigDecimal.ZERO);
        target.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(target);

        mockMvc.perform(post("/api/accounts/{id}/transfer", source.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", customerAuth)
                        .content("""
                                {"targetAccountId":"%s","amount":200,"currency":"USD"}
                                """.formatted(target.getId())))
                .andExpect(status().isOk());

        // 200 + 2 (1% fee) = 202 deducted
        assertThat(accountRepository.findById(source.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("798.00");
        assertThat(accountRepository.findById(target.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("200.00");
    }
}
```

- [ ] **Step 3: Run E2E tests**

```bash
mvn test -Dtest="CustomerE2ETest,AccountE2ETest" -q
```
Expected: `Tests run: 8, Failures: 0, Errors: 0`.

- [ ] **Step 4: Run the full test suite**

```bash
mvn test -q
```
Expected: all tests pass (service + controller + integration + E2E).

- [ ] **Step 5: Commit**

```bash
git add src/test/java/dev/kaldiroglu/layered/ayvalikbank/e2e/
git commit -m "test: add @SpringBootTest E2E tests with H2"
```

---

## Task 16: Documentation files

**Files:**
- Create: `README.md`
- Create: `CLAUDE.md`
- Create: `Architecture.md`
- Create: `Flows.md`
- Create: `Tests.md`

- [ ] **Step 1: Create `README.md`**

```markdown
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

## Documentation

| Document | Contents |
|----------|---------|
| [Architecture.md](Architecture.md) | Layer-by-layer breakdown and contrast with HA1 |
| [Flows.md](Flows.md) | Sequence diagrams for each use case |
| [Tests.md](Tests.md) | Test pyramid, per-class test tables, testing style analysis |
```

- [ ] **Step 2: Create `CLAUDE.md`**

```markdown
# CLAUDE.md

## Project

**Ayvalık Bank LA-1** — layered-architecture banking application in Java 21 / Spring Boot 3.4.

## Commands

```bash
docker compose up -d
mvn clean verify
mvn test -Dtest=AccountServiceTest
mvn spring-boot:run
```

## Package Structure

```
web/controller/      → AdminController, CustomerController, AccountController
web/dto/request/     → 6 request record DTOs
web/dto/response/    → 4 response record DTOs (static from(entity) factory methods)
web/                 → GlobalExceptionHandler
service/             → CustomerService, AccountService, PasswordValidationService, TransferService
repository/          → 4 Spring Data JPA repository interfaces
model/               → @Entity classes (anemic) + enums
exception/           → 7 typed RuntimeException subclasses
config/              → SecurityConfig, BankUserDetailsService, AdminDataInitializer
```

## Key Design Decisions (and Anti-Patterns)

- **Anemic model**: `Account`, `Customer`, `Transaction` are @Entity classes with getters/setters only. No business methods.
- **Business logic in services**: status guards, balance checks, fee calculation, password history rotation — all in `AccountService` / `CustomerService`.
- **No port interfaces**: controllers call services directly; services call Spring Data repositories directly.
- **No mappers**: response DTOs have `static from(Entity)` factory methods; no separate mapper classes.
- **JPA entities as domain objects**: the same `Account` object travels from repository → service → controller.
- **PasswordEncoder in service**: `CustomerService` depends directly on Spring Security's `PasswordEncoder`.

## REST API

| Method | Path | Role | Purpose |
|--------|------|------|---------|
| POST | `/api/admin/customers` | ADMIN | Create customer |
| DELETE | `/api/admin/customers/{id}` | ADMIN | Delete customer |
| GET | `/api/admin/customers` | ADMIN | List customers |
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

## Default Admin

Email: `admin@ayvalikbank.dev` / Password: `Admin@123!`
```

- [ ] **Step 3: Create `Architecture.md`**

Write a full architecture breakdown following the same structure as HA1's Architecture.md but describing the layered pattern. Cover:
- The 3-tier diagram (Presentation → Service → Repository)
- Layer-by-layer breakdown (what lives in each layer)
- The contrast table with HA1 (same one from the design doc)
- Key anti-patterns and why they are common

- [ ] **Step 4: Create `Flows.md`**

Write sequence diagrams for the 7 key use cases (CreateCustomer, ChangePassword, Deposit, Transfer, DeleteCustomer, GetBalance, FreezeAccount). Use the same Mermaid format as HA1's Flows.md but with updated lane names:
- Replace `AppSvc` → `CustomerService` / `AccountService`
- Remove port/adapter lanes — the call goes directly `Controller → Service → Repository → DB`

- [ ] **Step 5: Create `Tests.md`**

Write the full test reference following HA1's Tests.md structure:
- Test pyramid diagram with counts
- Per-class test tables (what each test verifies)
- Exception-to-HTTP mapping table
- Testing style analysis (output / state / communication per test class)
- Note the new integration and E2E tiers not present in HA1

- [ ] **Step 6: Run full test suite one final time**

```bash
mvn clean verify -q
```
Expected: `BUILD SUCCESS`, all tests pass.

- [ ] **Step 7: Final commit**

```bash
git add README.md CLAUDE.md Architecture.md Flows.md Tests.md
git commit -m "docs: add README, CLAUDE.md, Architecture.md, Flows.md, Tests.md"
```

---

## Self-Review Checklist

Verified against the design spec:

| Spec requirement | Task that implements it |
|-----------------|------------------------|
| Anemic @Entity model | Task 2 |
| 7 exception types | Task 3 |
| 4 Spring Data repos | Task 4 |
| PasswordValidationService (8-16 chars, upper/lower/digit/special) | Task 5 |
| TransferService (fee = 0 same-customer, % cross-customer) | Task 6 |
| CustomerService (create, delete, list, changePassword + history) | Task 7 |
| AccountService (10 operations + setTransferFeePercent) | Task 8 |
| 6 request DTOs + 4 response DTOs with `from()` | Task 9 |
| GlobalExceptionHandler (9 exception mappings) | Task 9 |
| SecurityConfig + BankUserDetailsService + AdminDataInitializer | Task 10 |
| AdminController — 15 endpoints, 19 tests | Task 11 |
| CustomerController — 1 endpoint, 7 tests | Task 12 |
| AccountController — 7 endpoints, 17 tests | Task 13 |
| @DataJpaTest integration tests (H2) | Task 14 |
| @SpringBootTest E2E tests (H2) | Task 15 |
| README, CLAUDE.md, Architecture.md, Flows.md, Tests.md | Task 16 |


