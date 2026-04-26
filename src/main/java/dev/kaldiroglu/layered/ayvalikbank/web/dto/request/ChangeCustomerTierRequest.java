package dev.kaldiroglu.layered.ayvalikbank.web.dto.request;

import dev.kaldiroglu.layered.ayvalikbank.model.CustomerTier;
import jakarta.validation.constraints.NotNull;

public record ChangeCustomerTierRequest(@NotNull CustomerTier tier) {}
