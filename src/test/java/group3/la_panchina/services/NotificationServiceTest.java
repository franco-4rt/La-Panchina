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
import org.springframework.cglib.core.Local;
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
@Import ({NotificationService.class, NotificationMessages.class, SmsMessages.class, NotificationServiceTest.FakeSenderConfig.class})
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

    // Fake SMS sender: records the text messages instead of sending them
    static class RecordingSmsSender implements SmsSender
    {
        final List<SmsMessage> sent = new ArrayList<>();
        boolean failing = false;

        @Override 
        public void send(SmsMessage message)
        {
            if (failing)
            {
                throw new SmsSendException("simulated failure");
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

        @Bean
        RecordingSmsSender smsSender()
        {
            return new RecordingSmsSender();
        }
    }

    @Autowired
    private NotificationService service;
    @Autowired 
    private RecordingEmailSender sender;
    @Autowired 
    private RecordingSmsSender smsSender;
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
        smsSender.sent.clear();
        smsSender.failing = false;
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

    // SC: a confirmed booking sends the "confirmed" email to the customer
    @Test 
    void confirmedBookingSendsTheConfirmedEmail()
    {
        Booking booking = saveBooking("confirmed");

        service.sendStatusNotifications(NOW);

        assertEquals(1, sender.sent.size());
        assertEquals("confirmed@example.com", sender.sent.get(0).to());
        assertTrue(sender.sent.get(0).subject().contains("confirmada")); // Spanish for confirmed
        assertEquals(NotificationStatus.SENT, statusOf(booking, NotificationType.STATUS_CONFIRMED));
        assertFalse(notificationRepository.existsByBookingIdAndType(booking.getId(), NotificationType.STATUS_CANCELLED));
    }

    // SC: a cancelled booking sends the "cancelled" email to the customer
    @Test
    void cancelledBookingSendsTheCancelledEmail()
    {
        Booking booking = saveBooking("cancelled");

        service.sendStatusNotifications(NOW);

        assertEquals(1, sender.sent.size());
        assertTrue(sender.sent.get(0).subject().contains("cancelada")); // Spanish for cancelled
        assertEquals(NotificationStatus.SENT, statusOf(booking, NotificationType.STATUS_CANCELLED));
    }

    // SC: a booking that is still pending gets no status email
    @Test 
    void pendingBookingGetsNoStatusEmail()
    {
        saveBooking("pending");

        service.sendStatusNotifications(NOW);

        assertEquals(0, sender.sent.size());
    }

    // SC: running the job again does not send the status email twice
    @Test
    void statusEmailIsNotSentTwice()
    {
        saveBooking("confirmed");

        service.sendStatusNotifications(NOW);
        service.sendStatusNotifications(NOW);

        assertEquals(1, sender.sent.size());
    }

    // SC: a booking that already started gets no status email (it's just recorded as SKIPPED)
    @Test 
    void noStatusEmailForBookingsThatAlreadyStarted()
    {
        Booking booking = saveBooking("confirmed", LocalDate.of(2026, 12, 24), LocalTime.of(10, 0));

        service.sendStatusNotifications(NOW);

        assertEquals(0, sender.sent.size());
        assertEquals(NotificationStatus.SKIPPED, statusOf(booking, NotificationType.STATUS_CONFIRMED));
    }

    // SC: (Real flow case) A booking is made, then an administrator confirms it later
    @Test 
    void changingTheStatusAfterBookingSendsTheConfirmedEmail()
    {
        Booking booking = saveBooking("pending");
        service.sendNewBookingNotifications();
        service.sendStatusNotifications(NOW);
        assertEquals(2, sender.sent.size()); // Only the confirmation and the admin alert

        booking.setStatus("Confirmed");
        bookingRepository.saveAndFlush(booking);
        service.sendStatusNotifications(NOW);

        assertEquals(3, sender.sent.size());
        assertTrue(sender.sent.get(2).subject().contains("confirmada"));
    }

    // =========== SMS Notifications (Iteration 3) =============

    // Helper: saves a customer who chose the given contact preference and phone, and a booking for December 24, 2026 at 19:30
    private Booking saveSmsBooking(String status, String preference, String phone)
    {
        return saveSmsBooking(status, preference, phone, LocalDate.of(2026, 12, 24), LocalTime.of(19, 30));
    }

    private Booking saveSmsBooking(String status, String preference, String phone, LocalDate date, LocalTime time)
    {
        User customer = new User("John Doe", "sms-" + status + "@example.com", "xxxx", "CUSTOMER");
        customer.setPhone(phone);
        customer.setContactPreference(preference);
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

    // Helper: The notification saved for a booking and notification type
    private Notification rowOf(Booking booking, NotificationType type)
    {
        return notificationRepository.findByBookingIdAndType(booking.getId(), type).orElseThrow();
    }

    // *SC = Success Case, FC = Failure Case

    // SC: a customer who chose SMS gets the confirmation by text; the administrator still gets an email
    @Test 
    void smsCustomerGetsTheConfirmationBySmsOnly()
    {
        Booking booking = saveSmsBooking("pending", "SMS", "(604) 321-4567");

        service.sendNewBookingNotifications();

        assertEquals(1, smsSender.sent.size());
        assertEquals("+16043214567", smsSender.sent.get(0).to());
        assertTrue(smsSender.sent.get(0).body().contains("reserva"));
        assertEquals(1, sender.sent.size());
        assertEquals("admin@test.com", sender.sent.get(0).to());

        Notification confirmation = rowOf(booking, NotificationType.BOOKING_CONFIRMATION);
        assertEquals("SMS", confirmation.getChannel());
        assertEquals("+16043214567", confirmation.getRecipient());
        assertEquals(NotificationStatus.SENT, confirmation.getStatus());
        assertEquals("EMAIL", rowOf(booking, NotificationType.ADMIN_NEW_BOOKING).getChannel());
    }

    // SC: the preference can be saved in any capitalization (e.g. "sms" & "SMS")
    @Test 
    void smsPreferenceIsNotCaseSensitive()
    {
        saveSmsBooking("pending", "sms", "604-321-4567");

        service.sendNewBookingNotifications();

        assertEquals(1, smsSender.sent.size());
    }

    // SC: a customer who chose email get no SMS
    @Test 
    void emailCustomerGetsNoSms()
    {
        Booking booking = saveBooking("pending");

        service.sendNewBookingNotifications();

        assertEquals(0, smsSender.sent.size());
        assertEquals(2, sender.sent.size());
        assertEquals("EMAIL", rowOf(booking, NotificationType.BOOKING_CONFIRMATION).getChannel());
    }

    // FC: customer who chose SMS with an invalid phone number gets nothing (saved as SKIPPED), and the admin still gets the alert (so it can still see the booking made)
    @Test 
    void invalidPhoneGetsNothingWhenSmsIsChosen()
    {
        Booking booking = saveSmsBooking("pending", "SMS", "098-765-4321");

        service.sendNewBookingNotifications();
        service.sendNewBookingNotifications();

        assertEquals(0, smsSender.sent.size());
        assertEquals(1, sender.sent.size());
        assertEquals("admin@test.com", sender.sent.get(0).to());

        Notification confirmation = rowOf(booking, NotificationType.BOOKING_CONFIRMATION);
        assertEquals(NotificationStatus.SKIPPED, confirmation.getStatus());
        assertEquals("SMS", confirmation.getChannel());
        assertNull(confirmation.getRecipient());
        assertEquals("Invalid phone number for SMS", confirmation.getError());
    }

    // SC: running the job again does not send the same text twice
    @Test 
    void smsIsNotSentTwice()
    {
        saveSmsBooking("pending", "SMS", "604-321-4567");

        service.sendNewBookingNotifications();
        service.sendNewBookingNotifications();

        assertEquals(1, smsSender.sent.size());
    }

    // FC: a failed text is recorded and retried on the next run
    @Test 
    void failedSmsIsRecordedAndRetried()
    {
        Booking booking = saveSmsBooking("pending", "SMS", "604-321-4567");

        smsSender.failing = true;
        service.sendNewBookingNotifications();
        assertEquals(NotificationStatus.FAILED, statusOf(booking, NotificationType.BOOKING_CONFIRMATION));
        assertEquals(0, smsSender.sent.size());

        smsSender.failing = false;
        service.sendNewBookingNotifications();
        assertEquals(NotificationStatus.SENT, statusOf(booking, NotificationType.BOOKING_CONFIRMATION));
        assertEquals(1, smsSender.sent.size());
        assertEquals(2, rowOf(booking, NotificationType.BOOKING_CONFIRMATION).getAttempts());
    }

    // FC: after the maximum numbers of attempts the job gives up on the text
    @Test 
    void smsStopsRetryingAfterMaximumAttempts()
    {
        Booking booking = saveSmsBooking("pending", "SMS", "604-321-4567");

        smsSender.failing = true;
        for (int run = 0; run < NotificationService.MAX_ATTEMPTS + 1; run++)
        {
            service.sendNewBookingNotifications();
        }

        assertEquals(NotificationService.MAX_ATTEMPTS, rowOf(booking, NotificationType.BOOKING_CONFIRMATION).getAttempts());
    }

    // SC: a booking starting in 3 hours (booked days ago) gets the reminder by SMS
    @Test 
    void smsReminderIsSentToSmsCustomers()
    {
        Booking booking = saveSmsBooking("pending", "SMS", "604-321-4567", LocalDate.of(2026, 12, 24), LocalTime.of(15, 0));
        service.sendNewBookingNotifications();
        setConfirmationCreatedAt(booking, NOW.minusDays(2).toInstant());
        sender.sent.clear();
        smsSender.sent.clear();

        service.sendReminders(NOW);

        assertEquals(1, smsSender.sent.size());
        assertEquals("+16043214567", smsSender.sent.get(0).to());
        assertTrue(smsSender.sent.get(0).body().contains("recordatorio"));
        assertEquals(0, sender.sent.size());
        assertEquals("SMS", rowOf(booking, NotificationType.REMINDER).getChannel());
        assertEquals(NotificationStatus.SENT, statusOf(booking, NotificationType.REMINDER));
    }

    // FC: a customer who chose SMS with an invalid phone gets no reminder
    @Test 
    void noReminderwhenTheSmsPhoneIsInvalid()
    {
        Booking booking = saveSmsBooking("pending", "SMS", "098-765-4321", LocalDate.of(2026, 12, 24), LocalTime.of(15, 0));
        service.sendNewBookingNotifications();
        setConfirmationCreatedAt(booking, NOW.minusDays(2).toInstant());
        sender.sent.clear();

        service.sendReminders(NOW);

        assertEquals(0, smsSender.sent.size());
        assertEquals(0, sender.sent.size());
        assertEquals(NotificationStatus.SKIPPED, statusOf(booking, NotificationType.REMINDER));
    }

    // SC: when the administrator confirms the booking, the SMS customer gets a text (no email)
    @Test 
    void confirmedBookingSendsTheConfirmedSms()
    {
        Booking booking = saveSmsBooking("confirmed", "SMS", "604-321-4567");

        service.sendStatusNotifications(NOW);

        assertEquals(1, smsSender.sent.size());
        assertTrue(smsSender.sent.get(0).body().contains("confirmada"));
        assertEquals(0, sender.sent.size());
        assertEquals(NotificationStatus.SENT, statusOf(booking, NotificationType.STATUS_CONFIRMED));
    }

    // SC: when the administrator cancels the booking, the SMS customer gets a text
    @Test 
    void cancelledBookingSendsTheCancelledSms()
    {
        Booking booking = saveSmsBooking("cancelled", "SMS", "604-321-4567");

        service.sendStatusNotifications(NOW);

         assertEquals(1, smsSender.sent.size());
        assertTrue(smsSender.sent.get(0).body().contains("cancelada"));
        assertEquals(0, sender.sent.size());
        assertEquals(NotificationStatus.SENT, statusOf(booking, NotificationType.STATUS_CANCELLED));
    }
}
