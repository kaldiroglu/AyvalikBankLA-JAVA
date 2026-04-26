package dev.kaldiroglu.layered.ayvalikbank.web.controller;

import dev.kaldiroglu.layered.ayvalikbank.service.AccountService;
import dev.kaldiroglu.layered.ayvalikbank.web.dto.request.*;
import dev.kaldiroglu.layered.ayvalikbank.web.dto.response.AccountResponse;
import dev.kaldiroglu.layered.ayvalikbank.web.dto.response.BalanceResponse;
import dev.kaldiroglu.layered.ayvalikbank.web.dto.response.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            @RequestParam UUID ownerId,
            @Valid @RequestBody CreateCheckingAccountRequest request) {
        BigDecimal overdraft = request.overdraftLimit() == null ? BigDecimal.ZERO : request.overdraftLimit();
        var account = accountService.createCheckingAccount(ownerId, request.currency(), overdraft);
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @PostMapping("/accounts/savings")
    public ResponseEntity<AccountResponse> createSavingsAccount(
            @RequestParam UUID ownerId,
            @Valid @RequestBody CreateSavingsAccountRequest request) {
        var account = accountService.createSavingsAccount(ownerId, request.currency(), request.annualInterestRate());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @PostMapping("/accounts/time-deposit")
    public ResponseEntity<AccountResponse> createTimeDepositAccount(
            @RequestParam UUID ownerId,
            @Valid @RequestBody CreateTimeDepositAccountRequest request) {
        var account = accountService.createTimeDepositAccount(
                ownerId, request.currency(), request.principal(),
                request.maturityDate(), request.annualInterestRate());
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
