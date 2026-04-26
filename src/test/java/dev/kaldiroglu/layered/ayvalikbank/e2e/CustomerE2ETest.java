package dev.kaldiroglu.layered.ayvalikbank.e2e;

import dev.kaldiroglu.layered.ayvalikbank.model.Customer;
import dev.kaldiroglu.layered.ayvalikbank.model.CustomerTier;
import dev.kaldiroglu.layered.ayvalikbank.repository.AccountRepository;
import dev.kaldiroglu.layered.ayvalikbank.repository.CustomerRepository;
import dev.kaldiroglu.layered.ayvalikbank.repository.SettingsRepository;
import dev.kaldiroglu.layered.ayvalikbank.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class CustomerE2ETest {

    @Autowired MockMvc mockMvc;
    @Autowired CustomerRepository customerRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired SettingsRepository settingsRepository;
    @Autowired PasswordEncoder passwordEncoder;

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
        admin.setTier(CustomerTier.STANDARD);
        admin.setCurrentPassword(passwordEncoder.encode("Admin@123!"));
        customerRepository.save(admin);

        adminAuth = "Basic " + Base64.getEncoder()
                .encodeToString("admin@ayvalikbank.dev:Admin@123!".getBytes());
    }

    @Test
    void shouldCreateCustomerAndReturnCreated() throws Exception {
        mockMvc.perform(post("/api/admin/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", adminAuth)
                        .content("""
                                {"name":"Alice","email":"alice@test.com","password":"Valid@123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        assertThat(customerRepository.findByEmail("alice@test.com")).isPresent();
    }

    @Test
    void shouldDeleteCustomerAndReturnNoContent() throws Exception {
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setName("ToDelete");
        c.setEmail("delete@test.com");
        c.setRole("CUSTOMER");
        c.setTier(CustomerTier.STANDARD);
        c.setCurrentPassword(passwordEncoder.encode("Valid@123"));
        customerRepository.save(c);

        mockMvc.perform(delete("/api/admin/customers/{id}", c.getId())
                        .header("Authorization", adminAuth))
                .andExpect(status().isNoContent());

        assertThat(customerRepository.findById(c.getId())).isEmpty();
    }

    @Test
    void shouldListCustomers() throws Exception {
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setName("Listed");
        c.setEmail("listed@test.com");
        c.setRole("CUSTOMER");
        c.setTier(CustomerTier.STANDARD);
        c.setCurrentPassword(passwordEncoder.encode("Valid@123"));
        customerRepository.save(c);

        mockMvc.perform(get("/api/admin/customers")
                        .header("Authorization", adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)); // admin + listed
    }

    @Test
    void shouldReturn401WithoutCredentials() throws Exception {
        mockMvc.perform(get("/api/admin/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenCustomerAccessesAdminEndpoint() throws Exception {
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setName("Regular");
        c.setEmail("regular@test.com");
        c.setRole("CUSTOMER");
        c.setTier(CustomerTier.STANDARD);
        c.setCurrentPassword(passwordEncoder.encode("Valid@123"));
        customerRepository.save(c);

        String customerAuth = "Basic " + Base64.getEncoder()
                .encodeToString("regular@test.com:Valid@123".getBytes());

        mockMvc.perform(get("/api/admin/customers")
                        .header("Authorization", customerAuth))
                .andExpect(status().isForbidden());
    }
}
