package dev.kaldiroglu.layered.ayvalikbank.e2e;

import dev.kaldiroglu.layered.ayvalikbank.model.*;
import dev.kaldiroglu.layered.ayvalikbank.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class AccountE2ETest {

    @Autowired MockMvc mockMvc;
    @Autowired CustomerRepository customerRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired SettingsRepository settingsRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private Customer customer;
    private String customerAuth;
    private String adminAuth;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        customerRepository.deleteAll();

        Customer admin = new Customer();
        admin.setId(UUID.randomUUID());
        admin.setName("Admin");
        admin.setEmail("admin@ayvalikbank.dev");
        admin.setRole("ADMIN");
        admin.setCurrentPassword(passwordEncoder.encode("Admin@123!"));
        customerRepository.save(admin);

        customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setName("Alice");
        customer.setEmail("alice@test.com");
        customer.setRole("CUSTOMER");
        customer.setCurrentPassword(passwordEncoder.encode("Valid@123"));
        customerRepository.save(customer);

        customerAuth = "Basic " + Base64.getEncoder()
                .encodeToString("alice@test.com:Valid@123".getBytes());
        adminAuth = "Basic " + Base64.getEncoder()
                .encodeToString("admin@ayvalikbank.dev:Admin@123!".getBytes());

        // Seed transfer fee (data.sql doesn't run in tests)
        Settings settings = settingsRepository.findById("TRANSFER_FEE_PERCENT")
                .orElseGet(() -> { Settings s = new Settings(); s.setKey("TRANSFER_FEE_PERCENT"); return s; });
        settings.setValue("1.0");
        settingsRepository.save(settings);
    }

    @Test
    void shouldCreateAccountDepositAndCheckBalance() throws Exception {
        // Create account
        String createResponse = mockMvc.perform(post("/api/accounts/checking")
                        .param("ownerId", customer.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", customerAuth)
                        .content("""
                                {"currency":"USD","overdraftLimit":0}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CHECKING"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.balance").value(0))
                .andReturn().getResponse().getContentAsString();

        String accountId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(createResponse).get("id").asText();

        // Deposit
        mockMvc.perform(post("/api/accounts/{id}/deposit", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", customerAuth)
                        .content("""
                                {"amount":500,"currency":"USD"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DEPOSIT"));

        // Check balance
        mockMvc.perform(get("/api/accounts/{id}/balance", accountId)
                        .header("Authorization", customerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(500.0))
                .andExpect(jsonPath("$.currency").value("USD"));

        Account saved = accountRepository.findById(UUID.fromString(accountId)).orElseThrow();
        assertThat(saved.getBalance()).isEqualByComparingTo("500.00");
    }

    @Test
    void shouldFreezeAndUnfreezeAccount() throws Exception {
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setOwnerId(customer.getId());
        account.setCurrency(Currency.USD);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        account.setType(AccountType.CHECKING);
        account.setOverdraftLimit(BigDecimal.ZERO);
        accountRepository.save(account);

        mockMvc.perform(put("/api/admin/accounts/{id}/freeze", account.getId())
                        .header("Authorization", adminAuth))
                .andExpect(status().isOk());

        assertThat(accountRepository.findById(account.getId()).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.FROZEN);

        mockMvc.perform(put("/api/admin/accounts/{id}/unfreeze", account.getId())
                        .header("Authorization", adminAuth))
                .andExpect(status().isOk());

        assertThat(accountRepository.findById(account.getId()).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void shouldTransferWithFeeForDifferentCustomers() throws Exception {
        Customer other = new Customer();
        other.setId(UUID.randomUUID());
        other.setName("Bob");
        other.setEmail("bob@test.com");
        other.setRole("CUSTOMER");
        other.setCurrentPassword(passwordEncoder.encode("Valid@123"));
        customerRepository.save(other);

        Account source = new Account();
        source.setId(UUID.randomUUID());
        source.setOwnerId(customer.getId());
        source.setCurrency(Currency.USD);
        source.setBalance(new BigDecimal("1000"));
        source.setStatus(AccountStatus.ACTIVE);
        source.setType(AccountType.CHECKING);
        source.setOverdraftLimit(BigDecimal.ZERO);
        accountRepository.save(source);

        Account target = new Account();
        target.setId(UUID.randomUUID());
        target.setOwnerId(other.getId());
        target.setCurrency(Currency.USD);
        target.setBalance(BigDecimal.ZERO);
        target.setStatus(AccountStatus.ACTIVE);
        target.setType(AccountType.CHECKING);
        target.setOverdraftLimit(BigDecimal.ZERO);
        accountRepository.save(target);

        mockMvc.perform(post("/api/accounts/{id}/transfer", source.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", customerAuth)
                        .content("""
                                {"targetAccountId":"%s","amount":200,"currency":"USD"}
                                """.formatted(target.getId())))
                .andExpect(status().isOk());

        // 200 + 2 (1% fee) = 202 deducted
        assertThat(accountRepository.findById(source.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("798.00");
        assertThat(accountRepository.findById(target.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("200.00");
    }
}
