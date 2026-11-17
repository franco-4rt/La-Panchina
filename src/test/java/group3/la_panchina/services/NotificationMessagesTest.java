package group3.la_panchina.services;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalTime;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import group3.la_panchina.models.Booking;
import group3.la_panchina.models.User;

class NotificationMessagesTest {

    private final NotificationMessages messages = new NotificationMessages("https://example.com");

    // Helper: A booking for Thursday, December 24, 2026 at 19:30 for 4 people, with a customer named "John Doe"
    private Booking booking() 
    {
        User customer = new User("John Doe", "john@example.com", "xxxx","CUSTOMER");
        customer.setPhone("123-456-7890");

        Booking booking = new Booking();
        booking.setId(5L);
        booking.setUser(customer);
        booking.setName("John Doe");
        booking.setDate(LocalDate.of(2026, 12, 24));
        booking.setTime(LocalTime.of(19, 30));
        booking.setPartySize(4);
        return booking;
    }

    // *SC = Success Case
    // SC: The confirmation goes to the customer and lists the booking details
    @Test
    void confirmationContainsTheBookingDetails()
    {
        EmailMessage email = messages.bookingConfirmation("john@example.com", booking());
        
        assertEquals("john@example.com", email.to());
        assertTrue(email.body().contains("Hola John Doe"));
        assertTrue(email.body().contains("Reserva #5"));
        assertTrue(email.body().contains("Fecha: 24/12/2026"));
        assertTrue(email.body().contains("Hora: 19:30"));
        assertTrue(email.body().contains("Número de personas: 4"));
    }

    // SC: The admin alert includes how to customer's contanct information
    @Test
    void adminAlertContainsCustomerContactInfo()
    {
        EmailMessage email = messages.adminNewBooking("admin@example.com", booking());

        assertEquals("admin@example.com", email.to());
        assertTrue(email.subject().contains("#5"));
        assertTrue(email.body().contains("john@example.com"));
        assertTrue(email.body().contains("123-456-7890"));
        assertTrue(email.body().contains("Estado: pending"));
        assertTrue(email.body().contains("https://example.com/admin/bookings"));
    }

    // SC: The reminder mentions the booking time
    @Test 
    void reminderMentionsTheBookingTime()
    {
        EmailMessage email = messages.bookingReminder("john@example.com", booking());
        
        assertTrue(email.subject().startsWith("Recordatorio")); // Spanish for "Reminder"
        assertTrue(email.body().contains("Hora: 19:30"));
    }

    // SC: The confirmed message says the booking is confirmed
    @Test
    void confirmedMessageSaysTheBookingIsConfirmed()
    {
        EmailMessage email = messages.statusConfirmed("john@example.com", booking());
        
        assertTrue(email.subject().contains("confirmada"));
        assertTrue(email.body().contains("Reserva #5"));
    }

    // SC: The cancelled message instructs the customer how to book again
    @Test
    void cancelledMessageIncludesRebookLink()
    {
        EmailMessage email = messages.statusCancelled("john@example.com", booking());
        
        assertTrue(email.subject().contains("cancelada"));
        assertTrue(email.body().contains("https://example.com/reservations"));
    }
}