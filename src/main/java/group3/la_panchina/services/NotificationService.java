package group3.la_panchina.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

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

    // How many hours before a booking the reminder is sent (US2)
    public static final int REMINDER_HOURS = 6;

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final BookingNotificationRepository bookingRepository;
    private final NotificationRepository notificationRepository;
    private final EmailSender emailSender;
    private final NotificationMessages messages;
    private final String adminEmail;
    private final ZoneId zone;

    public NotificationService 
    (
        BookingNotificationRepository bookingRepository,
        NotificationRepository notificationRepository,
        EmailSender emailSender,
        NotificationMessages messages,
        @Value("${notifications.admin-email:${ADMIN_EMAIL:admin@lapanchina.com}}") String adminEmail,
        @Value("${notifications.zone:America/Mexico_City}") String zone) {

        this.bookingRepository = bookingRepository;
        this.notificationRepository = notificationRepository;
        this.emailSender = emailSender;
        this.messages = messages;
        this.adminEmail = adminEmail;
        this.zone = ZoneId.of(zone);
    }

    // US1 + US4: confirmation to the customer and alert to the administrator
    public void sendNewBookingNotifications()
    {
        process(NotificationType.BOOKING_CONFIRMATION, bookingRepository.findPending(NotificationType.BOOKING_CONFIRMATION, NotificationStatus.FAILED, MAX_ATTEMPTS), booking -> booking.getUser().getEmail(), messages::bookingConfirmation, this::isCancelled);
        process(NotificationType.ADMIN_NEW_BOOKING, bookingRepository.findPending(NotificationType.ADMIN_NEW_BOOKING, NotificationStatus.FAILED, MAX_ATTEMPTS), booking -> adminEmail, messages::adminNewBooking, this::isCancelled);
    }

    // US2: Reminder for the bookings that start within the next REMINDER_HOURS
    public void sendReminders()
    {
        sendReminders(ZonedDateTime.now(zone));
    }

    // Same as above but with a given "current time"
    public void sendReminders(ZonedDateTime now)
    {
        LocalDateTime nowLocal = now.toLocalDateTime();
        LocalDateTime limit = nowLocal.plusHours(REMINDER_HOURS);

        List<Booking> inWindow = bookingRepository.findPendingBetweenDates(NotificationType.REMINDER, NotificationStatus.FAILED, MAX_ATTEMPTS, nowLocal.toLocalDate(), limit.toLocalDate())
        .stream()
        .filter (booking -> {
            LocalDateTime start = LocalDateTime.of(booking.getDate(), booking.getTime());
            return start.isAfter(nowLocal) && !start.isAfter(limit);
        })
        .toList();

        process(NotificationType.REMINDER, inWindow, booking -> booking.getUser().getEmail(), messages::bookingReminder, booking -> isCancelled(booking) || wasBookedInsideReminderWindow(booking));
    }

    // Sends one type of notification for each booking in the list
    private void process(NotificationType type, List<Booking> bookings, Function<Booking, String> recipient, BiFunction<String, Booking, EmailMessage> template, Predicate<Booking> shouldSkip)
    {
        for (Booking booking : bookings)
        {
            String to = recipient.apply(booking);
            Notification row = notificationRepository.findByBookingIdAndType(booking.getId(), type)
            .orElseGet(() -> new Notification(booking.getId(), type, "EMAIL", to, NotificationStatus.FAILED));

            if (shouldSkip.test(booking))
            {
                row.setStatus(NotificationStatus.SKIPPED);
                notificationRepository.save(row);
            }
            else
            {
                deliver(row, template.apply(to, booking));
            }
        }
    }

    private boolean isCancelled(Booking booking)
    {
        return BookingStatuses.isCancelled(booking.getStatus());
    }

    // True is booking was made after the reminder time had already passed (else, confirmation email is just enough)
    private boolean wasBookedInsideReminderWindow(Booking booking)
    {
        Instant confirmedAt = notificationRepository
            .findByBookingIdAndType(booking.getId(), NotificationType.BOOKING_CONFIRMATION)
            .map(Notification::getCreatedAt)
            .orElse(Instant.now());
        Instant reminderTime = LocalDateTime.of(booking.getDate(), booking.getTime())
            .atZone(zone)
            .minusHours(REMINDER_HOURS)
            .toInstant();
        return confirmedAt.isAfter(reminderTime);
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
