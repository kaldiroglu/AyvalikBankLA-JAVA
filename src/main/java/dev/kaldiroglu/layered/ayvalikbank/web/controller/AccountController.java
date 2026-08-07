package dev.kaldiroglu.layered.ayvalikbank.web.controller;

import dev.kaldiroglu.layered.ayvalikbank.service.AccountService;
import dev.kaldiroglu.layered.ayvalikbank.web.dto.request.*;
import dev.kaldiroglu.layered.ayvalikbank.web.dto.response.AccountResponse;
import dev.kaldiroglu.layered.ayvalikbank.web.dto.response.BalanceResponse;
import dev.kaldiroglu.layered.ayvalikbank.web.dto.response.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import dev.kaldiroglu.layered.ayvalikbank.config.BankUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/accounts/checking")
    public ResponseEntity<AccountResponse> createCheckingAccount(
            @AuthenticationPrincipal BankUserPrincipal caller,
            @Valid @RequestBody CreateCheckingAccountRequest request) {
        BigDecimal overdraft = request.overdraftLimit() == null ? BigDecimal.ZERO : request.overdraftLimit();
        var account = accountService.createCheckingAccount(caller.customerId(), request.currency(), overdraft);
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @PostMapping("/accounts/savings")
    public ResponseEntity<AccountResponse> createSavingsAccount(
            @AuthenticationPrincipal BankUserPrincipal caller,
            @Valid @RequestBody CreateSavingsAccountRequest request) {
        var account = accountService.createSavingsAccount(caller.customerId(), request.currency(), request.annualInterestRate());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @PostMapping("/accounts/time-deposit")
    public ResponseEntity<AccountResponse> createTimeDepositAccount(
            @AuthenticationPrincipal BankUserPrincipal caller,
            @Valid @RequestBody CreateTimeDepositAccountRequest request) {
        var account = accountService.createTimeDepositAccount(
                caller.customerId(), request.currency(), request.principal(),
                request.maturityDate(), request.annualInterestRate());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @GetMapping("/customers/{customerId}/accounts")
    public ResponseEntity<List<AccountResponse>> listAccounts(@AuthenticationPrincipal BankUserPrincipal caller, @PathVariable UUID customerId) {
        var accounts = accountService.listAccounts(caller.customerId(), customerId).stream()
                .map(AccountResponse::from).toList();
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/accounts/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@AuthenticationPrincipal BankUserPrincipal caller, @PathVariable UUID accountId) {
        return ResponseEntity.ok(BalanceResponse.from(accountService.getAccount(caller.customerId(), accountId)));
    }

    @PostMapping("/accounts/{accountId}/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @AuthenticationPrincipal BankUserPrincipal caller,
            @PathVariable UUID accountId,
            @Valid @RequestBody MoneyOperationRequest request) {
        var tx = accountService.deposit(caller.customerId(), accountId, request.amount(), request.currency());
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(tx));
    }

    @PostMapping("/accounts/{accountId}/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @AuthenticationPrincipal BankUserPrincipal caller,
            @PathVariable UUID accountId,
            @Valid @RequestBody MoneyOperationRequest request) {
        var tx = accountService.withdraw(caller.customerId(), accountId, request.amount(), request.currency());
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(tx));
    }

    @PostMapping("/accounts/{accountId}/transfer")
    public ResponseEntity<Void> transfer(
            @AuthenticationPrincipal BankUserPrincipal caller,
            @PathVariable UUID accountId,
            @Valid @RequestBody TransferRequest request) {
        accountService.transfer(caller.customerId(), accountId, UUID.fromString(request.targetAccountId()),
                request.amount(), request.currency());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(@AuthenticationPrincipal BankUserPrincipal caller, @PathVariable UUID accountId) {
        var txs = accountService.getTransactions(caller.customerId(), accountId).stream()
                .map(TransactionResponse::from).toList();
        return ResponseEntity.ok(txs);
    }
}
