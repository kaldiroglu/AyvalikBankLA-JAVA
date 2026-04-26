package dev.kaldiroglu.layered.ayvalikbank.service;

import dev.kaldiroglu.layered.ayvalikbank.exception.LimitExceededException;
import dev.kaldiroglu.layered.ayvalikbank.model.CustomerTier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TransferService {

    /**
     * Cross-customer fee = amount × feePercent × tier.feeMultiplier() / 100.
     * Same-customer transfers are always free.
     */
    public BigDecimal calculateFee(BigDecimal amount, boolean sameCustomer,
                                   BigDecimal feePercent, CustomerTier sourceTier) {
        if (sameCustomer) return BigDecimal.ZERO;
        BigDecimal scaledPercent = feePercent.multiply(sourceTier.feeMultiplier());
        return amount.multiply(scaledPercent)
                     .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public void requireTransferWithinLimit(BigDecimal amount, CustomerTier tier) {
        BigDecimal cap = tier.maxPerTransfer();
        if (cap != null && amount.compareTo(cap) > 0)
            throw new LimitExceededException(
                    "Transfer amount " + amount + " exceeds " + tier + " tier limit of " + cap);
    }

    public void requireWithdrawalWithinLimit(BigDecimal amount, CustomerTier tier) {
        BigDecimal cap = tier.maxPerWithdrawal();
        if (cap != null && amount.compareTo(cap) > 0)
            throw new LimitExceededException(
                    "Withdrawal amount " + amount + " exceeds " + tier + " tier limit of " + cap);
    }
}
