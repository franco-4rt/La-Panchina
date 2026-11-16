package group3.la_panchina.services;

import static org.junit.jupiter.api.Assertions.*;

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

    // Helper: (took from NotificationBaselineTest.java)
    private Booking saveBooking(String status)
    {
        User customer = new User("John Doe", status + "@example.com", "xxxx","CUSTOMER");
        customer.setPhone("123-456-7890");
        customer = userRepository.save(customer);

        Booking booking = new Booking();
        booking.setUser(customer);
        booking.setName("John Doe");
        booking.setDate(LocalDate.of(2026, 12, 24));
        booking.setTime(LocalTime.of(19, 30));
        booking.setPartySize(4);
        booking.setStatus(status);
        return bookingRepository.save(booking);
    }

    // Helper: The status saved for a booking and notification type
    private NotificationStatus statusOf(Booking booking, NotificationType type)
    {
        return notificationRepository.findByBookingIdAndType(booking.getId(), type).orElseThrow().geStatus();
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
}
