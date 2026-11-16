package group3.la_panchina.services;

import java.time.Instant;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import group3.la_panchina.models.Booking;
import group3.la_panchina.models.Notification;
import group3.la_panchina.models.NotificationStatus;
import group3.la_panchina.models.NotificationType;
import group3.la_panchina.repositories.BookingNotificationRepository;
import group3.la_panchina.repositories.NotificationRepository;

@Service 
public class NotificationService 
{
    // #tries for an email before giving up
    public static final int MAX_ATTEMPTS =  3;

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final BookingNotificationRepository bookingRepository;
    private final NotificationRepository notificationRepository;
    private final EmailSender emailSender;
    private final NotificationMessages messages;
    private final String adminEmail;

    public NotificationService 
    (
        BookingNotificationRepository bookingRepository,
        NotificationRepository notificationRepository,
        EmailSender emailSender,
        NotificationMessages messages,
        @Value("${notifications.admin-email:${ADMIN_EMAIL:admin@lapanchina.com}}") String adminEmail) {

        this.bookingRepository = bookingRepository;
        this.notificationRepository = notificationRepository;
        this.emailSender = emailSender;
        this.messages = messages;
        this.adminEmail = adminEmail;
    }

    // US1 + US4: confirmation to the customer and alert to the administrator
    public void sendNewBookingNotifications()
    {
        process(NotificationType.BOOKING_CONFIRMATION, booking -> booking.getUser().getEmail(), messages::bookingConfirmation);
        process(NotificationType.ADMIN_NEW_BOOKING, booking -> adminEmail, messages::adminNewBooking);
    }

    // Sends one type of notification for every booking that still needs it
    private void process(NotificationType type, Function<Booking, String> recipient, BiFunction<String, Booking, EmailMessage> template)
    {
        List<Booking> pending = bookingRepository.findPending(type, NotificationStatus.FAILED, MAX_ATTEMPTS);

        for (Booking booking : pending)
        {
            String to = recipient.apply(booking);
            Notification row = notificationRepository.findByBookingIdAndType(booking.getId(), type)
            .orElseGet(() -> new Notification(booking.getId(), type, "EMAIL", to, NotificationStatus.FAILED));

            if (BookingStatuses.isCancelled(booking.getStatus()))
            {
                // Cancelled before we told anyone: nothing to send
                row.setStatus(NotificationStatus.SKIPPED);
                notificationRepository.save(row);
            }
            else
            {
                deliver(row, template.apply(to, booking));
            }
        }

    }

    private void deliver(Notification row, EmailMessage message)
    {
        row.setAttempts(row.getAttempts() + 1); // 1st
        try {
            emailSender.send(message);
            row.setStatus(NotificationStatus.SENT);
            row.setSentAt(Instant.now());
            row.setError(null);
        } catch (RuntimeException e) {
            // Any failure is recorded so one bad never blocks others
            row.setStatus(NotificationStatus.FAILED);
            row.setError(shorten(e.getMessage()));
            log.warn("Could not send {} for booking {} (attempt {}): {}", row.getType(), row.getBookingId(), row.getAttempts(), e.getMessage());
        }
        notificationRepository.save(row);
    }

    // The error column hols 500 characters
    private String shorten(String text)
    {
        if (text == null)
        {
            return null;
        }
        return text.length() > 500 ? text.substring(0, 500) : text;
    }
}
