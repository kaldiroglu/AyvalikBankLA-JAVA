package dev.kaldiroglu.layered.ayvalikbank.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TransferServiceTest {

    private TransferService service;

    @BeforeEach
    void setUp() { service = new TransferService(); }

    @Test
    void shouldReturnZeroFeeForSameCustomerTransfer() {
        BigDecimal fee = service.calculateFee(new BigDecimal("200"), true, new BigDecimal("1.0"));
        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldCalculateFeeForDifferentCustomers() {
        BigDecimal fee = service.calculateFee(new BigDecimal("200"), false, new BigDecimal("1.0"));
        assertThat(fee).isEqualByComparingTo("2.00");
    }

    @Test
    void shouldReturnZeroFeeWhenFeePercentIsZero() {
        BigDecimal fee = service.calculateFee(new BigDecimal("200"), false, BigDecimal.ZERO);
        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
