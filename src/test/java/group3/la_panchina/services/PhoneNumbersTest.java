package group3.la_panchina.services;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class PhoneNumbersTest {
    // *SC = Success Case, FC = Failure Case

    // SC: the different ways of writing the same number all become +1XXXXXXXXXX
    @Test
    void differentFormatsBecomeTheSameInternationalNumber()
    {
        Optional<String> expected = Optional.of("+16043214567");

        assertEquals(expected, PhoneNumbers.toE164("604-321-4567"));
        assertEquals(expected, PhoneNumbers.toE164("(604) 321 4567"));
        assertEquals(expected, PhoneNumbers.toE164("604.321.4567"));
        assertEquals(expected, PhoneNumbers.toE164("+1 604 321 4567"));
        assertEquals(expected, PhoneNumbers.toE164("16043214567"));
        assertEquals(expected, PhoneNumbers.toE164(" 604-321-4567 "));
    }

    // FC: numbers with imposible are codes (like seed users) are invalid
    @Test 
    void impossibleAreaCodesAndExchangesAreRejected()
    {
        assertTrue(PhoneNumbers.toE164("123-456-7890").isEmpty());
        assertTrue(PhoneNumbers.toE164("098-765-4321").isEmpty());
        assertTrue(PhoneNumbers.toE164("604-021-4567").isEmpty());
    }

    // FC: fictional 555-0100 to 555-0199 range is invalid, but othe 555 numbers are
    @Test
    void fictionalNumbersAreRejected()
    {
        assertTrue(PhoneNumbers.toE164("604-555-0100").isEmpty());
        assertTrue(PhoneNumbers.toE164("604-555-0199").isEmpty());
        assertEquals(Optional.of("+16045550200"), PhoneNumbers.toE164("604-555-0200"));
    }

    // FC: numbers from other countries, with extensions or with wrong lenght are not valid
    @Test 
    void otherCountriesAndWrongLengthsAreRejected()
    {
        assertTrue(PhoneNumbers.toE164("+52 951 123 4567").isEmpty());
        assertTrue(PhoneNumbers.toE164("6043214567 ext 2").isEmpty());
        assertTrue(PhoneNumbers.toE164("321-4567").isEmpty());
        assertTrue(PhoneNumbers.toE164("26043214567").isEmpty());
    }

    // FC: empty, blank or null values are not valid (and do not crash)
    @Test 
    void emptyValuesAreRejected()
    {
        assertTrue(PhoneNumbers.toE164(null).isEmpty());
        assertTrue(PhoneNumbers.toE164("").isEmpty());
        assertTrue(PhoneNumbers.toE164("   ").isEmpty());
        assertTrue(PhoneNumbers.toE164("abc").isEmpty());
    }
}


