package group3.la_panchina.services;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import group3.la_panchina.models.Notification;
import group3.la_panchina.models.Booking;
import group3.la_panchina.models.NotificationStatus;
import group3.la_panchina.models.NotificationType;
import group3.la_panchina.models.User;
import group3.la_panchina.repositories.BookingRepository;
import group3.la_panchina.repositories.NotificationRepository;
import group3.la_panchina.repositories.UserRepository;

// Test on notiication service with in-memory H2 database and a fake email sender
@DataJpaTest (properties = "notifications.admin-email=admin@test.com")
@Import ({NotificationService.class, NotificationMessages.class, NotificationServiceTest.FakeSenderConfig.class})
class NotificationServiceTest {
    
    // Fake sender: records the emails instead of sending them
    static class RecordingEmailSender implements EmailSender
    {
        final List<EmailMessage> sent = new ArrayList<>();
        boolean failing = false;

        @Override
        public void send(EmailMessage message)
        {
            if (failing)
            {
                throw new EmailSendException("simulated failure");
            }
            sent.add(message);
        }
    }

    @TestConfiguration
    static class FakeSenderConfig
    {
        @Bean 
        RecordingEmailSender emailSender()
        {
            return new RecordingEmailSender();
        }
    }

    @Autowired
    private NotificationService service;
    @Autowired 
    private RecordingEmailSender sender;
    @Autowired 
    private BookingRepository bookingRepository;
    @Autowired 
    private UserRepository userRepository;
    @Autowired 
    private NotificationRepository notificationRepository;

    // Fake sender is shared between tests, so needs to be resseted before entering another
    @BeforeEach 
    void reset()
    {
        sender.sent.clear();
        sender.failing = false;
    }

    // Fixed "current time" for the reminder tests: Dec 24, 2026 at 12:00
    private static final ZonedDateTime NOW = ZonedDateTime.of(2026, 12, 24, 12, 0, 0, 0, ZoneId.of("America/Mexico_City"));

     // Helper: pretends the confirmation of a booking was created at the given time
    private void setConfirmationCreatedAt(Booking booking, Instant when)
    {
        Notification row = notificationRepository
                .findByBookingIdAndType(booking.getId(), NotificationType.BOOKING_CONFIRMATION).orElseThrow();
        row.setCreatedAt(when);
        notificationRepository.saveAndFlush(row);
    }

    // Helper: saves a customer and a booking for the default date (Dec 24, 2026 at 19:30)
    private Booking saveBooking(String status)
    {
        return saveBooking(status, LocalDate.of(2026, 12, 24), LocalTime.of(19, 30));
    }

    // Helper: (took from NotificationBaselineTest.java) modified so it also saves date and time
    private Booking saveBooking(String status, LocalDate date, LocalTime time)
    {
        User customer = new User("John Doe", status + "@example.com", "xxxx","CUSTOMER");
        customer.setPhone("123-456-7890");
        customer = userRepository.save(customer);

        Booking booking = new Booking();
        booking.setUser(customer);
        booking.setName("John Doe");
        booking.setDate(date);
        booking.setTime(time);
        booking.setPartySize(4);
        booking.setStatus(status);
        return bookingRepository.save(booking);
    }

    // Helper: The status saved for a booking and notification type
    private NotificationStatus statusOf(Booking booking, NotificationType type)
    {
        return notificationRepository.findByBookingIdAndType(booking.getId(), type).orElseThrow().getStatus();
    }

    // *SC = SUCCESS CASE , FC = FAILURE CASE

    // SC: A new booking just sends one email to the customer and one to the administrator
    @Test 
    void newBookingSendsConfirmationAndAdminAlert()
    {
        Booking booking = saveBooking("pending");

        service.sendNewBookingNotifications();

        assertEquals(2, sender.sent.size());
        List<String> recipients = sender.sent.stream().map(EmailMessage::to).toList();
        assertTrue(recipients.contains("pending@example.com"));
        assertTrue(recipients.contains("admin@test.com"));
        assertEquals(NotificationStatus.SENT, statusOf(booking, NotificationType.BOOKING_CONFIRMATION));
        assertEquals(NotificationStatus.SENT, statusOf(booking, NotificationType.ADMIN_NEW_BOOKING));
    }

    // SC: Running the job again does not send the same email twice
    @Test
    void emailsAreNotSentTwice()
    {
        saveBooking("pending");

        service.sendNewBookingNotifications();
        service.sendNewBookingNotifications();

        assertEquals(2, sender.sent.size());
    }

    // FC: a failed email is recorded and retried on the next run
    @Test 
    void failedEmailsAreRecordedAndRetried()
    {
        Booking booking = saveBooking("pending");

        sender.failing = true;
        service.sendNewBookingNotifications();
        assertEquals(NotificationStatus.FAILED, statusOf(booking, NotificationType.BOOKING_CONFIRMATION));
        assertEquals(0, sender.sent.size());

        sender.failing = false;
        service.sendNewBookingNotifications();
        assertEquals(NotificationStatus.SENT, statusOf(booking, NotificationType.BOOKING_CONFIRMATION));
        assertEquals(2, sender.sent.size());
        assertEquals(2, notificationRepository.findByBookingIdAndType(booking.getId(), NotificationType.BOOKING_CONFIRMATION).orElseThrow().getAttempts());
    }

    // FC: after the maximum number of attempts the job just gives up (stops, intended)
    @Test
    void stopsRetryingAfterMaximumAttempts()
    {
        Booking booking = saveBooking("pending");

        sender.failing = true;
        for (int run = 0; run < NotificationService.MAX_ATTEMPTS + 1; run++)
        {
            service.sendNewBookingNotifications();
        }

        assertEquals(NotificationService.MAX_ATTEMPTS, notificationRepository.findByBookingIdAndType(
                booking.getId(), NotificationType.BOOKING_CONFIRMATION).orElseThrow().getAttempts());
    }

    // SC: A booking is cancelled before job runs gets no emails
    @Test 
    void cancelledBookingIsSkipped()
    {
        Booking booking = saveBooking("cancelled");

        service.sendNewBookingNotifications();

        assertEquals(0, sender.sent.size());
        assertEquals(NotificationStatus.SKIPPED, statusOf(booking, NotificationType.BOOKING_CONFIRMATION));
        assertEquals(NotificationStatus.SKIPPED, statusOf(booking, NotificationType.ADMIN_NEW_BOOKING));
    }

    // SC: a booking starting in 3 hours (booked days ago)
    @Test 
    void reminderIsSentWhenTheBookingIsWithinSixHours()
    {
        Booking booking = saveBooking("pending", LocalDate.of(2026, 12, 24), LocalTime.of(15, 0));
        service.sendNewBookingNotifications();
        setConfirmationCreatedAt(booking, NOW.minusDays(2).toInstant());
        sender.sent.clear();

        service.sendReminders(NOW);

        assertEquals(1, sender.sent.size());
        assertEquals("pending@example.com", sender.sent.get(0).to());
        assertTrue(sender.sent.get(0).subject().startsWith("Recordatorio"));
        assertEquals(NotificationStatus.SENT, statusOf(booking, NotificationType.REMINDER));
    }

    // SC: a booking more than 6 hours away gets no reminder yet
    @Test 
    void noReminderWhenTheBookingIsMoreThanSixHoursAway()
    {
        Booking booking = saveBooking("pending", LocalDate.of(2026, 12, 24), LocalTime.of(19, 30));
        service.sendNewBookingNotifications();
        setConfirmationCreatedAt(booking, NOW.minusDays(2).toInstant());
        sender.sent.clear();

        service.sendReminders(NOW);

        assertEquals(0, sender.sent.size());
        assertFalse(notificationRepository.existsByBookingIdAndType(booking.getId(), NotificationType.REMINDER));
    }

    // FC: a booking that already start gets no reminder
    @Test 
    void noReminderForBookingsThatAlreadyStarted()
    {
        Booking booking = saveBooking("pending", LocalDate.of(2026, 12, 24), LocalTime.of(10, 0));
        service.sendNewBookingNotifications();
        setConfirmationCreatedAt(booking, NOW.minusDays(2).toInstant());
        sender.sent.clear();

        service.sendReminders(NOW);

        assertEquals(0, sender.sent.size());
    }

    // SC: booking made inside the 6-hour window only gets confirmation email, no reminder
    @Test 
    void noReminderWhenTheBookingWasMadeInsideTheWindow()
    {
        Booking booking = saveBooking("pending", LocalDate.of(2026, 12, 24), LocalTime.of(15, 0));
        service.sendNewBookingNotifications();
        setConfirmationCreatedAt(booking, NOW.minusHours(1).toInstant());
        sender.sent.clear();

        service.sendReminders(NOW);

        assertEquals(0, sender.sent.size());
        assertEquals(NotificationStatus.SKIPPED, statusOf(booking, NotificationType.REMINDER));
    }

    // SC: a booking with status cancelled gets no reminder
    @Test 
    void noReminderForCancelledBookings()
    {
        Booking booking = saveBooking("cancelled", LocalDate.of(2026, 12, 24), LocalTime.of(15, 0));
        service.sendNewBookingNotifications();
        setConfirmationCreatedAt(booking, NOW.minusDays(2).toInstant());
        sender.sent.clear();

        service.sendReminders(NOW);

        assertEquals(0, sender.sent.size());
        assertEquals(NotificationStatus.SKIPPED, statusOf(booking, NotificationType.REMINDER));
    }

    // SC: Running the reminder job again does not send the reminder twice
    @Test 
    void reminderIsNotSentTwice()
    {
        Booking booking = saveBooking("pending", LocalDate.of(2026, 12, 24), LocalTime.of(15, 0));
        service.sendNewBookingNotifications();
        setConfirmationCreatedAt(booking, NOW.minusDays(2).toInstant());
        sender.sent.clear();

        service.sendReminders(NOW);
        service.sendReminders(NOW);

        assertEquals(1, sender.sent.size());
    }
}
