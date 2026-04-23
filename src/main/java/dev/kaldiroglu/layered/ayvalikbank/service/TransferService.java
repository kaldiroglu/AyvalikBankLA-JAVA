package dev.kaldiroglu.layered.ayvalikbank.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TransferService {

    public BigDecimal calculateFee(BigDecimal amount, boolean sameCustomer, BigDecimal feePercent) {
        if (sameCustomer) return BigDecimal.ZERO;
        return amount.multiply(feePercent)
                     .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
