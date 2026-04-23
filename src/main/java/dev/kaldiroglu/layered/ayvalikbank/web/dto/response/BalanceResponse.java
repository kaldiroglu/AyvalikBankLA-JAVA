package dev.kaldiroglu.layered.ayvalikbank.web.dto.response;

import dev.kaldiroglu.layered.ayvalikbank.model.Account;

import java.math.BigDecimal;

public record BalanceResponse(BigDecimal amount, String currency) {
    public static BalanceResponse from(Account a) {
        return new BalanceResponse(a.getBalance(), a.getCurrency().name());
    }
}
