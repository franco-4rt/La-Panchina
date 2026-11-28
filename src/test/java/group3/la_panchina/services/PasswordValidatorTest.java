package group3.la_panchina.services;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class PasswordValidatorTest {

    @Test
    void validPasswordPasses() {
        assertTrue(PasswordValidator.isValid("StrongPass123"));
    }

    @Test
    void tooShortPasswordFails() {
        assertFalse(PasswordValidator.isValid("Short1A"));
    }

    @Test
    void missingUppercaseFails() {
        assertFalse(PasswordValidator.isValid("lowercasepass123"));
    }

    @Test
    void missingLowercaseFails() {
        assertFalse(PasswordValidator.isValid("UPPERCASEPASS123"));
    }

    @Test
    void missingDigitFails() {
        assertFalse(PasswordValidator.isValid("NoNumbersHerePass"));
    }
}