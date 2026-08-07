package dev.kaldiroglu.layered.ayvalikbank.repository;

import dev.kaldiroglu.layered.ayvalikbank.model.Account;
import dev.kaldiroglu.layered.ayvalikbank.model.AccountStatus;
import dev.kaldiroglu.layered.ayvalikbank.model.AccountType;
import dev.kaldiroglu.layered.ayvalikbank.model.Currency;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * No threads are needed. A lost update is a <b>stale-read</b> problem, not a timing problem, so
 * two persistence contexts committing in a fixed order reproduce it deterministically.
 *
 * <p>Mirrors AyvalikBankHA-JAVA Refactorings.md entry 5.
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Optimistic locking on accounts")
class AccountOptimisticLockingTest {

    @Autowired
    private EntityManagerFactory emf;

    private UUID insertAccount(String balance) {
        UUID id = UUID.randomUUID();
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Account a = new Account();
        a.setId(id);
        a.setOwnerId(UUID.randomUUID());
        a.setCurrency(Currency.USD);
        a.setBalance(new BigDecimal(balance));
        a.setStatus(AccountStatus.ACTIVE);
        a.setType(AccountType.CHECKING);
        a.setOverdraftLimit(BigDecimal.ZERO);
        em.persist(a);
        em.getTransaction().commit();
        em.close();
        return id;
    }

    @Test
    void shouldPersistANewAccountAtVersionZero() {
        UUID id = insertAccount("100.00");

        EntityManager em = emf.createEntityManager();
        assertThat(em.find(Account.class, id).getVersion()).isZero();
        em.close();
    }

    @Test
    void shouldIncrementTheVersionOnEachUpdate() {
        UUID id = insertAccount("100.00");

        for (int expected = 1; expected <= 2; expected++) {
            EntityManager em = emf.createEntityManager();
            em.getTransaction().begin();
            em.find(Account.class, id).setBalance(new BigDecimal("10.0" + expected));
            em.getTransaction().commit();
            em.close();

            EntityManager check = emf.createEntityManager();
            assertThat(check.find(Account.class, id).getVersion()).isEqualTo(expected);
            check.close();
        }
    }

    @Test
    @DisplayName("the second writer is rejected when both loaded the same version")
    void shouldRejectTheSecondWriterWhenBothLoadedTheSameVersion() {
        UUID id = insertAccount("100.00");

        EntityManager em1 = emf.createEntityManager();
        EntityManager em2 = emf.createEntityManager();
        em1.getTransaction().begin();
        em2.getTransaction().begin();

        // Both read balance 100 at version 0 — this is the stale read.
        Account first = em1.find(Account.class, id);
        Account second = em2.find(Account.class, id);

        first.setBalance(new BigDecimal("50.00"));
        em1.getTransaction().commit();

        second.setBalance(new BigDecimal("50.00"));
        assertThatThrownBy(() -> em2.getTransaction().commit())
                .isInstanceOf(RollbackException.class)
                .hasCauseInstanceOf(OptimisticLockException.class);

        em1.close();
        em2.close();

        // Without the version both writers would have stored 50.00 and one withdrawal lost.
        EntityManager check = emf.createEntityManager();
        assertThat(check.find(Account.class, id).getVersion()).isEqualTo(1);
        check.close();
    }
}
