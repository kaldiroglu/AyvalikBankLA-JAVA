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
        a.setType(AccountType.CHECKING);
        a.setOverdraftLimit(BigDecimal.ZERO);
        return a;
    }

    private Account stubSavingsAccount(UUID ownerId, Currency currency, BigDecimal rate) {
        Account a = stubAccount(ownerId, currency);
        a.setType(AccountType.SAVINGS);
        a.setOverdraftLimit(null);
        a.setInterestRate(rate);
        return a;
    }

    private Account stubTimeDepositAccount(UUID ownerId, Currency currency, BigDecimal principal,
                                            java.time.LocalDate maturity, BigDecimal rate) {
        Account a = stubAccount(ownerId, currency);
        a.setType(AccountType.TIME_DEPOSIT);
        a.setOverdraftLimit(null);
        a.setBalance(principal);
        a.setPrincipal(principal);
        a.setOpenedOn(java.time.LocalDate.now());
        a.setMaturityDate(maturity);
        a.setInterestRate(rate);
        a.setMatured(false);
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

    // ── POST /api/accounts/checking ──────────────────────────────────────

    @Test @WithMockUser(roles = "CUSTOMER")
    void createCheckingAccount_returnsCreated() throws Exception {
        UUID ownerId = UUID.randomUUID();
        when(accountService.createCheckingAccount(any(), any(), any()))
                .thenReturn(stubAccount(ownerId, Currency.USD));

        mockMvc.perform(post("/api/accounts/checking")
                        .param("ownerId", ownerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currency":"USD","overdraftLimit":0}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CHECKING"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test @WithMockUser(roles = "CUSTOMER")
    void createCheckingAccount_returnsBadRequestOnMissingCurrency() throws Exception {
        mockMvc.perform(post("/api/accounts/checking")
                        .param("ownerId", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(accountService);
    }

    @Test @WithMockUser(roles = "ADMIN")
    void createCheckingAccount_returnsForbiddenForAdminRole() throws Exception {
        mockMvc.perform(post("/api/accounts/checking")
                        .param("ownerId", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currency":"USD"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "CUSTOMER")
    void createSavingsAccount_returnsCreated() throws Exception {
        UUID ownerId = UUID.randomUUID();
        when(accountService.createSavingsAccount(any(), any(), any()))
                .thenReturn(stubSavingsAccount(ownerId, Currency.EUR, new BigDecimal("0.03")));

        mockMvc.perform(post("/api/accounts/savings")
                        .param("ownerId", ownerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currency":"EUR","annualInterestRate":0.03}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("SAVINGS"))
                .andExpect(jsonPath("$.interestRate").value(0.03));
    }

    @Test @WithMockUser(roles = "CUSTOMER")
    void createTimeDepositAccount_returnsCreated() throws Exception {
        UUID ownerId = UUID.randomUUID();
        java.time.LocalDate maturity = java.time.LocalDate.now().plusYears(1);
        when(accountService.createTimeDepositAccount(any(), any(), any(), any(), any()))
                .thenReturn(stubTimeDepositAccount(ownerId, Currency.USD,
                        new BigDecimal("1000"), maturity, new BigDecimal("0.05")));

        mockMvc.perform(post("/api/accounts/time-deposit")
                        .param("ownerId", ownerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currency":"USD","principal":1000,"maturityDate":"%s","annualInterestRate":0.05}
                                """.formatted(maturity)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("TIME_DEPOSIT"))
                .andExpect(jsonPath("$.principal").value(1000));
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
