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
        a.setType(AccountType.CHECKING);
        a.setOverdraftLimit(BigDecimal.ZERO);
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

        Account account = service.createCheckingAccount(ownerId, Currency.USD, BigDecimal.ZERO);

        assertThat(account.getCurrency()).isEqualTo(Currency.USD);
        assertThat(account.getOwnerId()).isEqualTo(ownerId);
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void shouldThrowCustomerNotFoundWhenOwnerMissing() {
        UUID ownerId = UUID.randomUUID();
        when(customerRepository.existsById(ownerId)).thenReturn(false);

        assertThatThrownBy(() -> service.createCheckingAccount(ownerId, Currency.EUR, BigDecimal.ZERO))
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

    // ── Type-specific behavior ────────────────────────────────────────────

    @Test
    void shouldOpenSavingsAccountWithRate() {
        UUID ownerId = UUID.randomUUID();
        when(customerRepository.existsById(ownerId)).thenReturn(true);
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Account account = service.createSavingsAccount(ownerId, Currency.EUR, new BigDecimal("0.03"));

        assertThat(account.getType()).isEqualTo(AccountType.SAVINGS);
        assertThat(account.getInterestRate()).isEqualByComparingTo("0.03");
        assertThat(account.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void shouldOpenTimeDepositAccountWithPrincipalAsBalance() {
        UUID ownerId = UUID.randomUUID();
        when(customerRepository.existsById(ownerId)).thenReturn(true);
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Account account = service.createTimeDepositAccount(ownerId, Currency.USD,
                new BigDecimal("1000"),
                java.time.LocalDate.now().plusYears(1),
                new BigDecimal("0.05"));

        assertThat(account.getType()).isEqualTo(AccountType.TIME_DEPOSIT);
        assertThat(account.getBalance()).isEqualByComparingTo("1000");
        assertThat(account.getMatured()).isFalse();
    }

    @Test
    void shouldRejectDepositOnTimeDeposit() {
        Account td = makeAccount(UUID.randomUUID(), Currency.USD);
        td.setType(AccountType.TIME_DEPOSIT);
        td.setOverdraftLimit(null);
        td.setBalance(new BigDecimal("1000"));
        when(accountRepository.findById(td.getId())).thenReturn(Optional.of(td));

        assertThatThrownBy(() -> service.deposit(td.getId(), new BigDecimal("100"), Currency.USD))
                .isInstanceOf(AccountNotOperableException.class)
                .hasMessageContaining("locked");
    }

    @Test
    void shouldAllowCheckingWithdrawIntoOverdraft() {
        Account checking = makeAccount(UUID.randomUUID(), Currency.USD);
        checking.setOverdraftLimit(new BigDecimal("100"));
        checking.setBalance(new BigDecimal("50"));
        when(accountRepository.findById(checking.getId())).thenReturn(Optional.of(checking));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.withdraw(checking.getId(), new BigDecimal("120"), Currency.USD);

        assertThat(checking.getBalance()).isEqualByComparingTo("-70.00");
    }

    @Test
    void shouldRejectWithdrawBeyondCheckingOverdraftLimit() {
        Account checking = makeAccount(UUID.randomUUID(), Currency.USD);
        checking.setOverdraftLimit(new BigDecimal("50"));
        when(accountRepository.findById(checking.getId())).thenReturn(Optional.of(checking));

        assertThatThrownBy(() -> service.withdraw(checking.getId(), new BigDecimal("60"), Currency.USD))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("overdraft");
    }

    @Test
    void shouldRejectWithdrawOnUnmaturedTimeDeposit() {
        Account td = makeAccount(UUID.randomUUID(), Currency.USD);
        td.setType(AccountType.TIME_DEPOSIT);
        td.setOverdraftLimit(null);
        td.setBalance(new BigDecimal("1000"));
        td.setMatured(false);
        when(accountRepository.findById(td.getId())).thenReturn(Optional.of(td));

        assertThatThrownBy(() -> service.withdraw(td.getId(), new BigDecimal("100"), Currency.USD))
                .isInstanceOf(AccountNotOperableException.class)
                .hasMessageContaining("matured");
    }

    @Test
    void shouldRejectTransferFromTimeDeposit() {
        Account td = makeAccount(UUID.randomUUID(), Currency.USD);
        td.setType(AccountType.TIME_DEPOSIT);
        td.setOverdraftLimit(null);
        td.setBalance(new BigDecimal("1000"));
        Account target = makeAccount(UUID.randomUUID(), Currency.USD);
        when(accountRepository.findById(td.getId())).thenReturn(Optional.of(td));
        when(accountRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.transfer(td.getId(), target.getId(),
                new BigDecimal("100"), Currency.USD))
                .isInstanceOf(AccountNotOperableException.class)
                .hasMessageContaining("transfers");
    }

    @Test
    void shouldAccrueMonthlyInterestOnSavings() {
        Account savings = makeAccount(UUID.randomUUID(), Currency.USD);
        savings.setType(AccountType.SAVINGS);
        savings.setOverdraftLimit(null);
        savings.setInterestRate(new BigDecimal("0.12")); // 1% monthly
        savings.setBalance(new BigDecimal("1000"));
        when(accountRepository.findById(savings.getId())).thenReturn(Optional.of(savings));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transaction tx = service.accrueInterest(savings.getId(), java.time.YearMonth.of(2026, 4));

        assertThat(tx.getType()).isEqualTo(TransactionType.INTEREST);
        assertThat(tx.getAmount()).isEqualByComparingTo("10.00");
        assertThat(savings.getBalance()).isEqualByComparingTo("1010.00");
        assertThat(savings.getLastAccrualDate()).isEqualTo(java.time.LocalDate.of(2026, 5, 1));
    }

    @Test
    void shouldRejectAccrueOnNonSavings() {
        Account checking = makeAccount(UUID.randomUUID(), Currency.USD);
        when(accountRepository.findById(checking.getId())).thenReturn(Optional.of(checking));

        assertThatThrownBy(() -> service.accrueInterest(checking.getId(),
                java.time.YearMonth.of(2026, 4)))
                .isInstanceOf(AccountNotOperableException.class)
                .hasMessageContaining("savings");
    }

    @Test
    void shouldMatureTimeDepositOnOrAfterMaturityAndCreditInterest() {
        Account td = makeAccount(UUID.randomUUID(), Currency.USD);
        td.setType(AccountType.TIME_DEPOSIT);
        td.setOverdraftLimit(null);
        td.setBalance(new BigDecimal("1000"));
        td.setPrincipal(new BigDecimal("1000"));
        td.setOpenedOn(java.time.LocalDate.now().minusYears(1).minusDays(1));
        td.setMaturityDate(java.time.LocalDate.now().minusDays(1));
        td.setInterestRate(new BigDecimal("0.05"));
        td.setMatured(false);
        when(accountRepository.findById(td.getId())).thenReturn(Optional.of(td));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transaction tx = service.matureTimeDeposit(td.getId());

        assertThat(tx.getType()).isEqualTo(TransactionType.INTEREST);
        assertThat(td.getMatured()).isTrue();
    }

    @Test
    void shouldRejectMatureOnNonTimeDeposit() {
        Account checking = makeAccount(UUID.randomUUID(), Currency.USD);
        when(accountRepository.findById(checking.getId())).thenReturn(Optional.of(checking));

        assertThatThrownBy(() -> service.matureTimeDeposit(checking.getId()))
                .isInstanceOf(AccountNotOperableException.class)
                .hasMessageContaining("time deposit");
    }
}
