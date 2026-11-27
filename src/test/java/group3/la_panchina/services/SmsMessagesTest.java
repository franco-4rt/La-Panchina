package group3.la_panchina.services;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import group3.la_panchina.models.Booking;
import group3.la_panchina.models.User;

class SmsMessagesTest {
    private final SmsMessages messages = new SmsMessages("https://example.com");

    // Helper: A booking for Thursday, December 24, 2026 at 19:30 for the given number of people, with a customer named "José Núñez"
    private Booking booking(int partySize)
    {
        User customer = new User("José Nuñéz", "jose@example.com", "xxxx", "CUSTOMER");
        customer.setPhone("604-321-4567");

        Booking booking = new Booking();
        booking.setId(5L);
        booking.setUser(customer);
        booking.setName("José Núñez");
        booking.setDate(LocalDate.of(2026, 12, 24));
        booking.setTime(LocalTime.of(19, 30));
        booking.setPartySize(partySize);
        return booking;
    }

    // Helper: the four texts for the same booking
    private List<SmsMessage> allMessages(Booking booking)
    {
        return List.of(
            messages.bookingConfirmation("+16043214567", booking),
            messages.bookingReminder("+16043214567", booking),
            messages.statusConfirmed("+16043214567", booking),
            messages.statusCancelled("+16043214567", booking)
        );
    }

    // *SC = Success Case, FC = Failure Case

    // SC: The confirmation goes to the given number and lists the booking details
    @Test
    void confirmationContainsTheBookingDetails()
    {
        SmsMessage sms = messages.bookingConfirmation("+16043214567", booking(4));

        assertEquals("+16043214567", sms.to());
        assertTrue(sms.body().contains("#5"));
        assertTrue(sms.body().contains("24/12/2026"));
        assertTrue(sms.body().contains("19:30"));
        assertTrue(sms.body().contains("4 personas"));
    }

    // SC: The reminder mentions the date and time of the booking
    @Test
    void reminderMentionsTheBookingTime()
    {
        SmsMessage sms = messages.bookingReminder("+16043214567", booking(4));

        assertTrue(sms.body().contains("recordatorio"));
        assertTrue(sms.body().contains("24/12/2026"));
        assertTrue(sms.body().contains("19:30"));
    }

    // SC: The status texts say if the booking was confirmed or cancelled
    @Test
    void statusTextsSayConfirmedOrCancelled()
    {
        assertTrue(messages.statusConfirmed("+16043214567", booking(4)).body().contains("confirmada"));
        assertTrue(messages.statusCancelled("+16043214567", booking(4)).body().contains("cancelada"));
    }

    // SC: The cancellation text includes the link to book again
    @Test
    void cancellationIncludesTheLinkToBookAgain()
    {
        SmsMessage sms = messages.statusCancelled("+16043214567", booking(4));

        assertTrue(sms.body().contains("https://example.com/reservations"));
    }

    // SC: One person is written in singular
    @Test 
    void OnePersonIsWrittenInSingular()
    {
        String body = messages.bookingConfirmation("+16043214567", booking(1)).body();

        assertTrue(body.contains("1 persona")); // Singular for person
        assertFalse(body.contains("1 personas")); // Plural
    }

    // SC: Texts use only plain characters (no accents), so they can use the 160-character SMS mode
    @Test
    void textsUseOnlyPlainCharacters()
    {
        for (SmsMessage sms : allMessages(booking(4)))
        {
            assertTrue(sms.body().chars().allMatch(c -> c >= 32 && c < 127), sms.body());
        }
    }

    // SC: Accents that come from outside (e.g. in the site URL) are removed too
    @Test
    void accentsFromOutisdeAreRemoved()
    {
        SmsMessages accented = new SmsMessages("https://exámple.com");

        String body = accented.statusCancelled("+16043214567", booking(4)).body();

        assertTrue(body.contains("https://example.com/reservations"));
    }

    // SC: Every text fits in a single 160-character SMS (with the site URL used in the tests)
    @Test
    void textsFitInOneSms()
    {
        for (SmsMessage sms : allMessages(booking(10)))
        {
            assertTrue(sms.body().length() <= 160, sms.body().length() + " characters: " + sms.body());
        }
    }
}