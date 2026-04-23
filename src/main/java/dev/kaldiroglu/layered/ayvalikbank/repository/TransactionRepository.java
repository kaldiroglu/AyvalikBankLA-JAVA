package dev.kaldiroglu.layered.ayvalikbank.repository;

import dev.kaldiroglu.layered.ayvalikbank.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByAccountId(UUID accountId);
}
