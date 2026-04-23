package dev.kaldiroglu.layered.ayvalikbank.service;

import dev.kaldiroglu.layered.ayvalikbank.exception.InvalidPasswordException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class PasswordValidationServiceTest {

    private PasswordValidationService service;

    @BeforeEach
    void setUp() { service = new PasswordValidationService(); }

    @Test
    void shouldAcceptValidPassword() {
        assertThatNoException().isThrownBy(() -> service.validate("Valid@123"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Short1!", "ThisIsWayTooLong1!"})
    void shouldRejectPasswordOutOfLengthRange(String password) {
        assertThatThrownBy(() -> service.validate(password))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("between 8 and 16");
    }

    @Test
    void shouldRejectPasswordWithoutUppercase() {
        assertThatThrownBy(() -> service.validate("nouppercase1!"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("uppercase");
    }

    @Test
    void shouldRejectPasswordWithoutLowercase() {
        assertThatThrownBy(() -> service.validate("NOLOWERCASE1!"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("lowercase");
    }

    @Test
    void shouldRejectPasswordWithoutDigit() {
        assertThatThrownBy(() -> service.validate("NoDigitHere!"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("digit");
    }

    @Test
    void shouldRejectPasswordWithoutSpecialCharacter() {
        assertThatThrownBy(() -> service.validate("NoSpecial123"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("special");
    }

    @Test
    void shouldRejectNullPassword() {
        assertThatThrownBy(() -> service.validate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
