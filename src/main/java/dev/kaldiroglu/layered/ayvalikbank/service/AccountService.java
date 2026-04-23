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
