package dev.kaldiroglu.layered.ayvalikbank.repository;

import dev.kaldiroglu.layered.ayvalikbank.model.Account;
import dev.kaldiroglu.layered.ayvalikbank.model.AccountStatus;
import dev.kaldiroglu.layered.ayvalikbank.model.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.properties")
class AccountRepositoryTest {

    @Autowired AccountRepository accountRepository;

    private Account makeAccount(UUID ownerId, Currency currency) {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        a.setOwnerId(ownerId);
        a.setCurrency(currency);
        a.setBalance(BigDecimal.ZERO);
        a.setStatus(AccountStatus.ACTIVE);
        return a;
    }

    @Test
    void shouldFindAccountsByOwnerId() {
        UUID ownerId = UUID.randomUUID();
        accountRepository.saveAll(List.of(
                makeAccount(ownerId, Currency.USD),
                makeAccount(ownerId, Currency.EUR),
                makeAccount(UUID.randomUUID(), Currency.TRY)));

        List<Account> accounts = accountRepository.findByOwnerId(ownerId);

        assertThat(accounts).hasSize(2);
        assertThat(accounts).extracting(Account::getOwnerId).containsOnly(ownerId);
    }

    @Test
    void shouldReturnEmptyListWhenNoAccountsForOwner() {
        List<Account> accounts = accountRepository.findByOwnerId(UUID.randomUUID());
        assertThat(accounts).isEmpty();
    }

    @Test
    void shouldPersistEnumFieldsCorrectly() {
        UUID ownerId = UUID.randomUUID();
        Account saved = accountRepository.save(makeAccount(ownerId, Currency.EUR));

        Account loaded = accountRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getCurrency()).isEqualTo(Currency.EUR);
        assertThat(loaded.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }
}
