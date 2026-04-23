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
