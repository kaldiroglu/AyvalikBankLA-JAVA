package dev.kaldiroglu.layered.ayvalikbank.repository;

import dev.kaldiroglu.layered.ayvalikbank.model.Customer;
import dev.kaldiroglu.layered.ayvalikbank.model.CustomerTier;
import dev.kaldiroglu.layered.ayvalikbank.model.PasswordHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.properties")
class CustomerRepositoryTest {

    @Autowired CustomerRepository customerRepository;

    private Customer makeCustomer(String name, String email) {
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setName(name);
        c.setEmail(email);
        c.setRole("CUSTOMER");
        c.setTier(CustomerTier.STANDARD);
        c.setCurrentPassword("hash");
        return c;
    }

    @Test
    void shouldFindCustomerByEmail() {
        customerRepository.save(makeCustomer("Alice", "alice@test.com"));

        var found = customerRepository.findByEmail("alice@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Alice");
    }

    @Test
    void shouldReturnEmptyForUnknownEmail() {
        var found = customerRepository.findByEmail("nobody@test.com");
        assertThat(found).isEmpty();
    }

    @Test
    void shouldCascadeDeletePasswordHistoryOnCustomerDelete() {
        Customer c = makeCustomer("Bob", "bob@test.com");
        PasswordHistory ph = new PasswordHistory();
        ph.setId(UUID.randomUUID());
        ph.setCustomer(c);
        ph.setHashedPassword("old-hash");
        ph.setPosition(0);
        c.getPasswordHistory().add(ph);
        customerRepository.save(c);

        UUID customerId = c.getId();
        customerRepository.deleteById(customerId);

        assertThat(customerRepository.findById(customerId)).isEmpty();
    }

    @Test
    void shouldEnforceUniqueEmail() {
        customerRepository.save(makeCustomer("A", "dup@test.com"));
        Customer dup = makeCustomer("B", "dup@test.com");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            customerRepository.saveAndFlush(dup);
        }).isInstanceOf(Exception.class);
    }
}
