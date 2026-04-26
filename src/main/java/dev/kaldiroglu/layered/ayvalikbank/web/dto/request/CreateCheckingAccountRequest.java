package dev.kaldiroglu.layered.ayvalikbank.web.dto.request;

import dev.kaldiroglu.layered.ayvalikbank.model.Currency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateCheckingAccountRequest(
        @NotNull Currency currency,
        @PositiveOrZero BigDecimal overdraftLimit) {}
