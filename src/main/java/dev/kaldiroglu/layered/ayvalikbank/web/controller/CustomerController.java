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
