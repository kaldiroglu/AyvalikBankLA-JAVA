package dev.kaldiroglu.layered.ayvalikbank.web.dto.request;

import dev.kaldiroglu.layered.ayvalikbank.model.Currency;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(@NotNull Currency currency) {}
