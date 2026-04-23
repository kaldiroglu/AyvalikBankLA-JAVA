package dev.kaldiroglu.layered.ayvalikbank.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kaldiroglu.layered.ayvalikbank.config.BankUserDetailsService;
import dev.kaldiroglu.layered.ayvalikbank.config.SecurityConfig;
import dev.kaldiroglu.layered.ayvalikbank.exception.AccountNotOperableException;
import dev.kaldiroglu.layered.ayvalikbank.exception.CustomerNotFoundException;
import dev.kaldiroglu.layered.ayvalikbank.model.Customer;
import dev.kaldiroglu.layered.ayvalikbank.service.AccountService;
import dev.kaldiroglu.layered.ayvalikbank.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean BankUserDetailsService userDetailsService;
    @MockitoBean CustomerService customerService;
    @MockitoBean AccountService accountService;

    private Customer stubCustomer(String name, String email) {
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setName(name);
        c.setEmail(email);
        c.setRole("CUSTOMER");
        c.setCurrentPassword("hash");
        return c;
    }

    @Test @WithMockUser(roles = "ADMIN")
    void createCustomer_returnsCreated() throws Exception {
        when(customerService.createCustomer(any(), any(), any()))
                .thenReturn(stubCustomer("Alice", "alice@test.com"));

        mockMvc.perform(post("/api/admin/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice","email":"alice@test.com","password":"Valid@123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test @WithMockUser(roles = "ADMIN")
    void createCustomer_returnsBadRequestOnMissingName() throws Exception {
        mockMvc.perform(post("/api/admin/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@test.com","password":"Valid@123"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(customerService);
    }

    @Test @WithMockUser(roles = "ADMIN")
    void createCustomer_returnsBadRequestOnInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/admin/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice","email":"not-an-email","password":"Valid@123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "CUSTOMER")
    void createCustomer_returnsForbiddenForCustomerRole() throws Exception {
        mockMvc.perform(post("/api/admin/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice","email":"alice@test.com","password":"Valid@123"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCustomer_returnsUnauthorizedWithoutCredentials() throws Exception {
        mockMvc.perform(post("/api/admin/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice","email":"alice@test.com","password":"Valid@123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void deleteCustomer_returnsNoContent() throws Exception {
        doNothing().when(customerService).deleteCustomer(any());
        mockMvc.perform(delete("/api/admin/customers/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void deleteCustomer_returnsNotFoundWhenMissing() throws Exception {
        doThrow(new CustomerNotFoundException("Customer not found"))
                .when(customerService).deleteCustomer(any());
        mockMvc.perform(delete("/api/admin/customers/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void listCustomers_returnsOkWithList() throws Exception {
        when(customerService.listCustomers()).thenReturn(List.of(
                stubCustomer("Alice", "alice@test.com"),
                stubCustomer("Bob", "bob@test.com")));
        mockMvc.perform(get("/api/admin/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value("alice@test.com"));
    }

    @Test @WithMockUser(roles = "ADMIN")
    void listCustomers_returnsEmptyList() throws Exception {
        when(customerService.listCustomers()).thenReturn(List.of());
        mockMvc.perform(get("/api/admin/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test @WithMockUser(roles = "ADMIN")
    void setTransferFee_returnsOk() throws Exception {
        doNothing().when(accountService).setTransferFeePercent(any());
        mockMvc.perform(put("/api/admin/settings/transfer-fee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feePercent":1.5}
                                """))
                .andExpect(status().isOk());
        verify(accountService).setTransferFeePercent(any());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void setTransferFee_returnsBadRequestForNegativeValue() throws Exception {
        mockMvc.perform(put("/api/admin/settings/transfer-fee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feePercent":-1.0}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(accountService);
    }

    @Test @WithMockUser(roles = "ADMIN")
    void setTransferFee_returnsBadRequestForValueAbove100() throws Exception {
        mockMvc.perform(put("/api/admin/settings/transfer-fee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feePercent":101.0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void freezeAccount_returnsOk() throws Exception {
        doNothing().when(accountService).freezeAccount(any());
        mockMvc.perform(put("/api/admin/accounts/{id}/freeze", UUID.randomUUID()))
                .andExpect(status().isOk());
        verify(accountService).freezeAccount(any());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void freezeAccount_returnsUnprocessableEntityForInvalidTransition() throws Exception {
        doThrow(new AccountNotOperableException("Already frozen"))
                .when(accountService).freezeAccount(any());
        mockMvc.perform(put("/api/admin/accounts/{id}/freeze", UUID.randomUUID()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void unfreezeAccount_returnsOk() throws Exception {
        doNothing().when(accountService).unfreezeAccount(any());
        mockMvc.perform(put("/api/admin/accounts/{id}/unfreeze", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void unfreezeAccount_returnsUnprocessableEntityForInvalidTransition() throws Exception {
        doThrow(new AccountNotOperableException("Not frozen"))
                .when(accountService).unfreezeAccount(any());
        mockMvc.perform(put("/api/admin/accounts/{id}/unfreeze", UUID.randomUUID()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void closeAccount_returnsOk() throws Exception {
        doNothing().when(accountService).closeAccount(any());
        mockMvc.perform(put("/api/admin/accounts/{id}/close", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void closeAccount_returnsUnprocessableEntityForAlreadyClosed() throws Exception {
        doThrow(new AccountNotOperableException("Already closed"))
                .when(accountService).closeAccount(any());
        mockMvc.perform(put("/api/admin/accounts/{id}/close", UUID.randomUUID()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test @WithMockUser(roles = "CUSTOMER")
    void closeAccount_returnsForbiddenForCustomerRole() throws Exception {
        mockMvc.perform(put("/api/admin/accounts/{id}/close", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
