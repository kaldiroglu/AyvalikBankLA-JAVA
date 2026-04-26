package dev.kaldiroglu.layered.ayvalikbank.web.dto.response;

import dev.kaldiroglu.layered.ayvalikbank.model.Account;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID ownerId,
        String currency,
        BigDecimal balance,
        String status,
        String type,
        BigDecimal overdraftLimit,
        BigDecimal interestRate,
        LocalDate lastAccrualDate,
        BigDecimal principal,
        LocalDate openedOn,
        LocalDate maturityDate,
        Boolean matured) {

    public static AccountResponse from(Account a) {
        return new AccountResponse(
                a.getId(),
                a.getOwnerId(),
                a.getCurrency().name(),
                a.getBalance(),
                a.getStatus().name(),
                a.getType() == null ? null : a.getType().name(),
                a.getOverdraftLimit(),
                a.getInterestRate(),
                a.getLastAccrualDate(),
                a.getPrincipal(),
                a.getOpenedOn(),
                a.getMaturityDate(),
                a.getMatured());
    }
}
