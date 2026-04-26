package dev.kaldiroglu.layered.ayvalikbank.model;

import java.math.BigDecimal;

/**
 * Customer tier carries a fee multiplier and per-transaction caps.
 * STANDARD pays full fee with modest caps; PREMIUM pays half fee with
 * higher caps; PRIVATE is free and unlimited.
 *
 * Limits are currency-agnostic numeric thresholds (a learning-project
 * simplification — real banks would convert via FX).
 */
public enum CustomerTier {

    STANDARD(new BigDecimal("1.00"), new BigDecimal("5000"),  new BigDecimal("5000")),
    PREMIUM (new BigDecimal("0.50"), new BigDecimal("50000"), new BigDecimal("25000")),
    PRIVATE (new BigDecimal("0.00"), null,                    null);

    private final BigDecimal feeMultiplier;
    private final BigDecimal maxPerTransfer;     // null = unlimited
    private final BigDecimal maxPerWithdrawal;   // null = unlimited

    CustomerTier(BigDecimal feeMultiplier, BigDecimal maxPerTransfer, BigDecimal maxPerWithdrawal) {
        this.feeMultiplier = feeMultiplier;
        this.maxPerTransfer = maxPerTransfer;
        this.maxPerWithdrawal = maxPerWithdrawal;
    }

    public BigDecimal feeMultiplier() { return feeMultiplier; }
    public BigDecimal maxPerTransfer() { return maxPerTransfer; }
    public BigDecimal maxPerWithdrawal() { return maxPerWithdrawal; }
}
