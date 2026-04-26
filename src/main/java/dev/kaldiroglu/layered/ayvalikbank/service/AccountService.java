package dev.kaldiroglu.layered.ayvalikbank.service;

import dev.kaldiroglu.layered.ayvalikbank.exception.*;
import dev.kaldiroglu.layered.ayvalikbank.model.*;
import dev.kaldiroglu.layered.ayvalikbank.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AccountService {

    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

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

    // ── Account opening (one method per type) ─────────────────────────────

    public Account createCheckingAccount(UUID ownerId, Currency currency, BigDecimal overdraftLimit) {
        if (!customerRepository.existsById(ownerId))
            throw new CustomerNotFoundException("Customer not found: " + ownerId);
        if (overdraftLimit == null) overdraftLimit = BigDecimal.ZERO;
        if (overdraftLimit.signum() < 0)
            throw new IllegalArgumentException("Overdraft limit cannot be negative");
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setOwnerId(ownerId);
        account.setCurrency(currency);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        account.setType(AccountType.CHECKING);
        account.setOverdraftLimit(overdraftLimit);
        return accountRepository.save(account);
    }

    public Account createSavingsAccount(UUID ownerId, Currency currency, BigDecimal annualInterestRate) {
        if (!customerRepository.existsById(ownerId))
            throw new CustomerNotFoundException("Customer not found: " + ownerId);
        if (annualInterestRate == null || annualInterestRate.signum() < 0)
            throw new IllegalArgumentException("Annual interest rate must be non-negative");
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setOwnerId(ownerId);
        account.setCurrency(currency);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        account.setType(AccountType.SAVINGS);
        account.setInterestRate(annualInterestRate);
        return accountRepository.save(account);
    }

    public Account createTimeDepositAccount(UUID ownerId, Currency currency, BigDecimal principal,
                                            LocalDate maturityDate, BigDecimal annualInterestRate) {
        if (!customerRepository.existsById(ownerId))
            throw new CustomerNotFoundException("Customer not found: " + ownerId);
        if (principal == null || principal.signum() <= 0)
            throw new IllegalArgumentException("Principal must be positive");
        if (annualInterestRate == null || annualInterestRate.signum() < 0)
            throw new IllegalArgumentException("Annual interest rate must be non-negative");
        LocalDate openedOn = LocalDate.now();
        if (maturityDate == null || !maturityDate.isAfter(openedOn))
            throw new IllegalArgumentException("Maturity date must be after today");
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setOwnerId(ownerId);
        account.setCurrency(currency);
        account.setBalance(principal);
        account.setStatus(AccountStatus.ACTIVE);
        account.setType(AccountType.TIME_DEPOSIT);
        account.setPrincipal(principal);
        account.setOpenedOn(openedOn);
        account.setMaturityDate(maturityDate);
        account.setInterestRate(annualInterestRate);
        account.setMatured(false);
        return accountRepository.save(account);
    }

    // ── Account operations ────────────────────────────────────────────────

    public Transaction deposit(UUID accountId, BigDecimal amount, Currency currency) {
        Account account = findAccountOrThrow(accountId);
        requireActive(account);
        if (account.getType() == AccountType.TIME_DEPOSIT)
            throw new AccountNotOperableException("Time deposit principal is locked — further deposits are not allowed");
        if (account.getCurrency() != currency)
            throw new IllegalArgumentException("Currency mismatch: expected " + account.getCurrency());
        if (amount.signum() < 0)
            throw new IllegalArgumentException("Deposit amount cannot be negative");
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        return saveTransaction(accountId, TransactionType.DEPOSIT, amount, currency, "Deposit");
    }

    public Transaction withdraw(UUID accountId, BigDecimal amount, Currency currency) {
        Account account = findAccountOrThrow(accountId);
        requireActive(account);
        if (account.getCurrency() != currency)
            throw new IllegalArgumentException("Currency mismatch: expected " + account.getCurrency());
        if (amount.signum() < 0)
            throw new IllegalArgumentException("Withdrawal amount cannot be negative");

        // Time deposits: rejected until matured
        if (account.getType() == AccountType.TIME_DEPOSIT && !Boolean.TRUE.equals(account.getMatured()))
            throw new AccountNotOperableException("Time deposit has not matured");

        BigDecimal projected = account.getBalance().subtract(amount);

        // Checking: allow balance to go negative down to -overdraftLimit
        if (account.getType() == AccountType.CHECKING) {
            BigDecimal floor = account.getOverdraftLimit() == null
                    ? BigDecimal.ZERO
                    : account.getOverdraftLimit().negate();
            if (projected.compareTo(floor) < 0) {
                if (account.getOverdraftLimit() == null || account.getOverdraftLimit().signum() == 0)
                    throw new InsufficientFundsException("Insufficient funds");
                throw new InsufficientFundsException("Withdrawal exceeds overdraft limit");
            }
        } else {
            // Savings + matured time-deposit: hard floor at zero
            if (projected.signum() < 0)
                throw new InsufficientFundsException("Insufficient funds");
        }

        account.setBalance(projected);
        accountRepository.save(account);
        return saveTransaction(accountId, TransactionType.WITHDRAWAL, amount, currency, "Withdrawal");
    }

    public void transfer(UUID sourceId, UUID targetId, BigDecimal amount, Currency currency) {
        Account source = findAccountOrThrow(sourceId);
        Account target = findAccountOrThrow(targetId);
        requireActive(source);
        requireActive(target);
        if (source.getType() == AccountType.TIME_DEPOSIT)
            throw new AccountNotOperableException("Time deposit accounts do not support transfers");
        if (source.getCurrency() != currency)
            throw new IllegalArgumentException("Currency mismatch with source account");
        if (target.getCurrency() != currency)
            throw new IllegalArgumentException("Currency mismatch with target account");

        boolean sameCustomer = source.getOwnerId().equals(target.getOwnerId());
        BigDecimal feePercent = getFeePercent();
        BigDecimal fee = transferService.calculateFee(amount, sameCustomer, feePercent);
        BigDecimal totalDebit = amount.add(fee);
        BigDecimal projected = source.getBalance().subtract(totalDebit);

        if (source.getType() == AccountType.CHECKING) {
            BigDecimal floor = source.getOverdraftLimit() == null
                    ? BigDecimal.ZERO
                    : source.getOverdraftLimit().negate();
            if (projected.compareTo(floor) < 0)
                throw new InsufficientFundsException("Insufficient funds for transfer including fee");
        } else {
            if (projected.signum() < 0)
                throw new InsufficientFundsException("Insufficient funds for transfer including fee");
        }

        source.setBalance(projected);
        target.setBalance(target.getBalance().add(amount));
        accountRepository.save(source);
        accountRepository.save(target);

        String outDesc = "Transfer out to " + targetId +
                (fee.compareTo(BigDecimal.ZERO) > 0 ? " (fee: " + fee + ")" : "");
        saveTransaction(sourceId, TransactionType.TRANSFER_OUT, amount, currency, outDesc);
        saveTransaction(targetId, TransactionType.TRANSFER_IN, amount, currency,
                "Transfer in from " + sourceId);
    }

    // ── Savings: monthly interest accrual ────────────────────────────────

    public Transaction accrueInterest(UUID accountId, YearMonth month) {
        Account account = findAccountOrThrow(accountId);
        if (account.getType() != AccountType.SAVINGS)
            throw new AccountNotOperableException("Account is not a savings account");
        // FROZEN accounts can still accrue: it's a system action, not a customer one.
        if (account.getStatus() == AccountStatus.CLOSED)
            throw new AccountNotOperableException("Cannot accrue interest on a closed account");
        LocalDate firstOfNextMonth = month.plusMonths(1).atDay(1);
        if (account.getLastAccrualDate() != null && !firstOfNextMonth.isAfter(account.getLastAccrualDate()))
            throw new AccountNotOperableException("Interest already accrued for or after " + month);

        BigDecimal monthlyRate = account.getInterestRate()
                .divide(MONTHS_PER_YEAR, 10, RoundingMode.HALF_UP);
        BigDecimal interest = account.getBalance()
                .multiply(monthlyRate)
                .setScale(2, RoundingMode.HALF_UP);

        account.setBalance(account.getBalance().add(interest));
        account.setLastAccrualDate(firstOfNextMonth);
        accountRepository.save(account);
        return saveTransaction(accountId, TransactionType.INTEREST, interest, account.getCurrency(),
                "Interest accrual for " + month);
    }

    // ── Time deposit: maturation ─────────────────────────────────────────

    public Transaction matureTimeDeposit(UUID accountId) {
        Account account = findAccountOrThrow(accountId);
        if (account.getType() != AccountType.TIME_DEPOSIT)
            throw new AccountNotOperableException("Account is not a time deposit");
        // FROZEN accounts can still mature: it's a date-driven system action.
        if (account.getStatus() == AccountStatus.CLOSED)
            throw new AccountNotOperableException("Cannot mature a closed account");
        if (Boolean.TRUE.equals(account.getMatured()))
            throw new AccountNotOperableException("Account is already matured");
        LocalDate today = LocalDate.now();
        if (today.isBefore(account.getMaturityDate()))
            throw new AccountNotOperableException("Maturity date not yet reached");

        long months = ChronoUnit.MONTHS.between(account.getOpenedOn(), account.getMaturityDate());
        BigDecimal years = BigDecimal.valueOf(months).divide(MONTHS_PER_YEAR, 10, RoundingMode.HALF_UP);
        BigDecimal interest = account.getPrincipal()
                .multiply(account.getInterestRate())
                .multiply(years)
                .setScale(2, RoundingMode.HALF_UP);

        account.setBalance(account.getBalance().add(interest));
        account.setMatured(true);
        accountRepository.save(account);
        return saveTransaction(accountId, TransactionType.INTEREST, interest, account.getCurrency(),
                "Maturity interest credit");
    }

    // ── Read-only queries ─────────────────────────────────────────────────

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

    // ── Status transitions ────────────────────────────────────────────────

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

    // ── Settings ──────────────────────────────────────────────────────────

    public void setTransferFeePercent(BigDecimal feePercent) {
        Settings settings = settingsRepository.findById("TRANSFER_FEE_PERCENT")
                .orElseGet(() -> { Settings s = new Settings(); s.setKey("TRANSFER_FEE_PERCENT"); return s; });
        settings.setValue(feePercent.toPlainString());
        settingsRepository.save(settings);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void requireActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE)
            throw new AccountNotOperableException("Account is not active: " + account.getStatus());
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
