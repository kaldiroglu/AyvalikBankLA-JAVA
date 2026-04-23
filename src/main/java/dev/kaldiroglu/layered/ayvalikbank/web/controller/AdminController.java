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
