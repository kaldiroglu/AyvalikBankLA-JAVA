package dev.kaldiroglu.layered.ayvalikbank.service;

import dev.kaldiroglu.layered.ayvalikbank.exception.LimitExceededException;
import dev.kaldiroglu.layered.ayvalikbank.model.CustomerTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class TransferServiceTest {

    private TransferService service;

    @BeforeEach
    void setUp() { service = new TransferService(); }

    @Test
    void shouldReturnZeroFeeForSameCustomerTransfer() {
        BigDecimal fee = service.calculateFee(new BigDecimal("200"), true,
                new BigDecimal("1.0"), CustomerTier.STANDARD);
        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void standardTierPaysFullFeeForDifferentCustomers() {
        BigDecimal fee = service.calculateFee(new BigDecimal("200"), false,
                new BigDecimal("1.0"), CustomerTier.STANDARD);
        assertThat(fee).isEqualByComparingTo("2.00");
    }

    @Test
    void premiumTierPaysHalfFee() {
        BigDecimal fee = service.calculateFee(new BigDecimal("200"), false,
                new BigDecimal("1.0"), CustomerTier.PREMIUM);
        assertThat(fee).isEqualByComparingTo("1.00");
    }

    @Test
    void privateTierPaysNoFee() {
        BigDecimal fee = service.calculateFee(new BigDecimal("200"), false,
                new BigDecimal("1.0"), CustomerTier.PRIVATE);
        assertThat(fee).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldReturnZeroFeeWhenFeePercentIsZero() {
        BigDecimal fee = service.calculateFee(new BigDecimal("200"), false,
                BigDecimal.ZERO, CustomerTier.STANDARD);
        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void rejectsTransferAboveStandardCap() {
        assertThatThrownBy(() -> service.requireTransferWithinLimit(
                new BigDecimal("5001"), CustomerTier.STANDARD))
                .isInstanceOf(LimitExceededException.class)
                .hasMessageContaining("STANDARD");
    }

    @Test
    void allowsTransferAtExactlyTheCap() {
        assertThatCode(() -> service.requireTransferWithinLimit(
                new BigDecimal("5000"), CustomerTier.STANDARD))
                .doesNotThrowAnyException();
    }

    @Test
    void privateTierTransferIsUnlimited() {
        assertThatCode(() -> service.requireTransferWithinLimit(
                new BigDecimal("1000000"), CustomerTier.PRIVATE))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsWithdrawalAbovePremiumCap() {
        assertThatThrownBy(() -> service.requireWithdrawalWithinLimit(
                new BigDecimal("25001"), CustomerTier.PREMIUM))
                .isInstanceOf(LimitExceededException.class)
                .hasMessageContaining("PREMIUM");
    }

    @Test
    void privateTierWithdrawalIsUnlimited() {
        assertThatCode(() -> service.requireWithdrawalWithinLimit(
                new BigDecimal("1000000"), CustomerTier.PRIVATE))
                .doesNotThrowAnyException();
    }
}
