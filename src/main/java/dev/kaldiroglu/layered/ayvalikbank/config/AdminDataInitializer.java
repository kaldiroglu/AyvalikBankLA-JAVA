package dev.kaldiroglu.layered.ayvalikbank.config;

import dev.kaldiroglu.layered.ayvalikbank.model.Customer;
import dev.kaldiroglu.layered.ayvalikbank.model.CustomerTier;
import dev.kaldiroglu.layered.ayvalikbank.repository.CustomerRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AdminDataInitializer implements ApplicationRunner {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminDataInitializer(CustomerRepository customerRepository,
                                PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (customerRepository.findByEmail("admin@ayvalikbank.dev").isEmpty()) {
            Customer admin = new Customer();
            admin.setId(UUID.randomUUID());
            admin.setName("Admin");
            admin.setEmail("admin@ayvalikbank.dev");
            admin.setRole("ADMIN");
            admin.setTier(CustomerTier.STANDARD);
            admin.setCurrentPassword(passwordEncoder.encode("Admin@123!"));
            customerRepository.save(admin);
        }
    }
}
