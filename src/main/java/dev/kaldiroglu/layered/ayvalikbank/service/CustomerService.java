package dev.kaldiroglu.layered.ayvalikbank.service;

import dev.kaldiroglu.layered.ayvalikbank.exception.CustomerNotFoundException;
import dev.kaldiroglu.layered.ayvalikbank.exception.PasswordReusedException;
import dev.kaldiroglu.layered.ayvalikbank.model.Customer;
import dev.kaldiroglu.layered.ayvalikbank.model.CustomerTier;
import dev.kaldiroglu.layered.ayvalikbank.model.PasswordHistory;
import dev.kaldiroglu.layered.ayvalikbank.repository.CustomerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CustomerService {

    private static final int PASSWORD_HISTORY_SIZE = 3;

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidationService passwordValidationService;

    public CustomerService(CustomerRepository customerRepository,
                           PasswordEncoder passwordEncoder,
                           PasswordValidationService passwordValidationService) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidationService = passwordValidationService;
    }

    public Customer createCustomer(String name, String email, String rawPassword) {
        passwordValidationService.validate(rawPassword);
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setName(name);
        customer.setEmail(email);
        customer.setRole("CUSTOMER");
        customer.setTier(CustomerTier.STANDARD);
        customer.setCurrentPassword(passwordEncoder.encode(rawPassword));
        return customerRepository.save(customer);
    }

    public void changeCustomerTier(UUID customerId, CustomerTier newTier) {
        if (newTier == null) throw new IllegalArgumentException("Tier must not be null");
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));
        customer.setTier(newTier);
        customerRepository.save(customer);
    }

    public void deleteCustomer(UUID id) {
        if (!customerRepository.existsById(id))
            throw new CustomerNotFoundException("Customer not found: " + id);
        customerRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Customer> listCustomers() {
        return customerRepository.findAll();
    }

    public void changePassword(UUID customerId, String rawNewPassword) {
        passwordValidationService.validate(rawNewPassword);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        List<String> allHashes = new ArrayList<>();
        allHashes.add(customer.getCurrentPassword());
        customer.getPasswordHistory().stream()
                .sorted((a, b) -> a.getPosition() - b.getPosition())
                .forEach(ph -> allHashes.add(ph.getHashedPassword()));

        for (String hash : allHashes) {
            if (passwordEncoder.matches(rawNewPassword, hash))
                throw new PasswordReusedException("Password has been used recently");
        }

        // Build new history: old current + up to 2 previous entries
        List<String> newHistoryHashes = new ArrayList<>();
        newHistoryHashes.add(customer.getCurrentPassword());
        customer.getPasswordHistory().stream()
                .sorted((a, b) -> a.getPosition() - b.getPosition())
                .limit(PASSWORD_HISTORY_SIZE - 1)
                .forEach(ph -> newHistoryHashes.add(ph.getHashedPassword()));

        // Replace history (orphanRemoval handles DB deletes)
        customer.getPasswordHistory().clear();
        for (int i = 0; i < newHistoryHashes.size(); i++) {
            PasswordHistory ph = new PasswordHistory();
            ph.setId(UUID.randomUUID());
            ph.setCustomer(customer);
            ph.setHashedPassword(newHistoryHashes.get(i));
            ph.setPosition(i);
            customer.getPasswordHistory().add(ph);
        }

        customer.setCurrentPassword(passwordEncoder.encode(rawNewPassword));
        customerRepository.save(customer);
    }
}
