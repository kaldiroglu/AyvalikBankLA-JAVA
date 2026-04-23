package dev.kaldiroglu.layered.ayvalikbank.web.dto.response;

import dev.kaldiroglu.layered.ayvalikbank.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(UUID id, UUID accountId, String type,
                                   BigDecimal amount, String currency,
                                   LocalDateTime createdAt) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(t.getId(), t.getAccountId(),
                t.getType().name(), t.getAmount(),
                t.getCurrency().name(), t.getCreatedAt());
    }
}
