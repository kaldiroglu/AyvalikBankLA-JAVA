package dev.kaldiroglu.layered.ayvalikbank.web.dto.request;

import dev.kaldiroglu.layered.ayvalikbank.model.Currency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTimeDepositAccountRequest(
        @NotNull Currency currency,
        @NotNull @Positive BigDecimal principal,
        @NotNull LocalDate maturityDate,
        @NotNull @PositiveOrZero BigDecimal annualInterestRate) {}
