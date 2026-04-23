package dev.kaldiroglu.layered.ayvalikbank.web.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SetTransferFeeRequest(
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal feePercent) {}
