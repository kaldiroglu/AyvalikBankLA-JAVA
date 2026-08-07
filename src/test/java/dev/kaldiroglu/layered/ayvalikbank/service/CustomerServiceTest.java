package dev.kaldiroglu.layered.ayvalikbank.service;

import dev.kaldiroglu.layered.ayvalikbank.exception.*;
import dev.kaldiroglu.layered.ayvalikbank.model.Customer;
import dev.kaldiroglu.layered.ayvalikbank.model.CustomerTier;
import dev.kaldiroglu.layered.ayvalikbank.model.PasswordHistory;
import dev.kaldiroglu.layered.ayvalikbank.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private CustomerService service;

    @BeforeEach
    void setUp() {
        service = new CustomerService(customerRepository, passwordEncoder,
                new PasswordValidationService());
    }

    // ── createCustomer ────────────────────────────────────────────────────

    @Test
    void shouldCreateCustomerWithHashedPassword() {
        when(passwordEncoder.encode("Valid@123")).thenReturn("hashed");
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Customer result = service.createCustomer("Ali", "ali@test.com", "Valid@123");

        assertThat(result.getEmail()).isEqualTo("ali@test.com");
        assertThat(result.getCurrentPassword()).isEqualTo("hashed");
        assertThat(result.getRole()).isEqualTo("CUSTOMER");
        verify(customerRepository).save(any());
    }

    @Test
    void shouldThrowInvalidPasswordExceptionForWeakPassword() {
        assertThatThrownBy(() -> service.createCustomer("Ali", "ali@test.com", "weak"))
                .isInstanceOf(InvalidPasswordException.class);
        verifyNoInteractions(customerRepository);
    }

    // ── deleteCustomer ────────────────────────────────────────────────────

    @Test
    void shouldDeleteExistingCustomer() {
        UUID id = UUID.randomUUID();
        when(customerRepository.existsById(id)).thenReturn(true);

        service.deleteCustomer(id);

        verify(customerRepository).deleteById(id);
    }

    @Test
    void shouldThrowCustomerNotFoundOnDeleteOfMissingCustomer() {
        UUID id = UUID.randomUUID();
        when(customerRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteCustomer(id))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    // ── changePassword ────────────────────────────────────────────────────

    @Test
    void shouldChangePasswordSuccessfully() {
        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(id);
        customer.setCurrentPassword("old-hash");

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("Valid@123", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("Valid@123")).thenReturn("new-hash");
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.changePassword(id, id, "Valid@123");

        assertThat(customer.getCurrentPassword()).isEqualTo("new-hash");
        assertThat(customer.getPasswordHistory()).hasSize(1);
        assertThat(customer.getPasswordHistory().get(0).getHashedPassword()).isEqualTo("old-hash");
        verify(customerRepository).save(customer);
    }

    @Test
    void shouldThrowPasswordReusedExceptionWhenNewPasswordMatchesCurrent() {
        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(id);
        customer.setCurrentPassword("same-hash");

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("Valid@123", "same-hash")).thenReturn(true);

        assertThatThrownBy(() -> service.changePassword(id, id, "Valid@123"))
                .isInstanceOf(PasswordReusedException.class);
    }

    @Test
    void shouldDefaultNewCustomerToStandardTier() {
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Customer created = service.createCustomer("Ali", "ali@test.com", "Valid@123");

        assertThat(created.getTier()).isEqualTo(CustomerTier.STANDARD);
    }

    @Test
    void shouldChangeCustomerTier() {
        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(id);
        customer.setTier(CustomerTier.STANDARD);
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.changeCustomerTier(id, CustomerTier.PREMIUM);

        assertThat(customer.getTier()).isEqualTo(CustomerTier.PREMIUM);
        verify(customerRepository).save(customer);
    }

    @Test
    void shouldThrowCustomerNotFoundOnChangeTierForMissingCustomer() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeCustomerTier(id, CustomerTier.PREMIUM))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void shouldRejectChangingAnotherCustomersPassword() {
        assertThatThrownBy(() -> service.changePassword(
                UUID.randomUUID(), UUID.randomUUID(), "NewPass@123!"))
                .isInstanceOf(UnauthorizedAccessException.class);

        verifyNoInteractions(customerRepository);
    }
}
