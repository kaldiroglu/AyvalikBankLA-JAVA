package dev.kaldiroglu.layered.ayvalikbank.web.controller;

import dev.kaldiroglu.layered.ayvalikbank.service.CustomerService;
import dev.kaldiroglu.layered.ayvalikbank.web.dto.request.ChangePasswordRequest;
import jakarta.validation.Valid;
import dev.kaldiroglu.layered.ayvalikbank.config.BankUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal BankUserPrincipal caller,
                                               @PathVariable UUID id,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        customerService.changePassword(caller.customerId(), id, request.newPassword());
        return ResponseEntity.ok().build();
    }
}
