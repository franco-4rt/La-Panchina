package group3.la_panchina.services;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BookingStatusesTest {
    
    // *SC = SUCCESS CASE , FC = FAILURE CASE
    
    // SC: "confirmed" is recognized in any casing
    @Test
    void confirmedIsRecognizedIgnoringCase()
    {
        assertTrue(BookingStatuses.isConfirmed("confirmed"));
        assertTrue(BookingStatuses.isConfirmed("CONFIRMED"));
        assertFalse(BookingStatuses.isConfirmed("pending"));
    }

    // SC: both spellings of cancelled are recognized!
    void cancelledAcceptsBothSpellings()
    {
        assertTrue(BookingStatuses.isCancelled("cancelled"));
        assertTrue(BookingStatuses.isCancelled("Canceled"));
        assertFalse(BookingStatuses.isCancelled("confirmed"));
    }

    // FC: a missing status is neither confirmed nor cancelled
    void nullStatusIsNeither()
    {
        assertFalse(BookingStatuses.isConfirmed(null));
        assertFalse(BookingStatuses.isCancelled(null));
    }
}
