package dev.kaldiroglu.layered.ayvalikbank.web.dto.response;

import dev.kaldiroglu.layered.ayvalikbank.model.Account;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(UUID id, UUID ownerId, String currency, BigDecimal balance, String status) {
    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getId(), a.getOwnerId(),
                a.getCurrency().name(), a.getBalance(), a.getStatus().name());
    }
}
