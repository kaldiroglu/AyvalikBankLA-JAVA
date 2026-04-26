package dev.kaldiroglu.layered.ayvalikbank.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.YearMonth;

public record AccrueInterestRequest(@NotNull YearMonth month) {}
