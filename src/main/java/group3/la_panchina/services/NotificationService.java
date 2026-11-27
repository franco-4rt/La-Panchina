package group3.la_panchina.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import group3.la_panchina.models.Booking;
import group3.la_panchina.models.Notification;
import group3.la_panchina.models.NotificationStatus;
import group3.la_panchina.models.NotificationType;
import group3.la_panchina.models.User;
import group3.la_panchina.repositories.BookingNotificationRepository;
import group3.la_panchina.repositories.NotificationRepository;

@Service 
public class NotificationService 
{
    // #tries for an email or SMS before giving up
    public static final int MAX_ATTEMPTS =  3;

    // How many hours before a booking the reminder is sent (US2)
    public static final int REMINDER_HOURS = 6;

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final BookingNotificationRepository bookingRepository;
    private final NotificationRepository notificationRepository;
    private final EmailSender emailSender;
    private final NotificationMessages messages;
    private final SmsSender smsSender;
    private final SmsMessages smsMessages;
    private final String adminEmail;
    private final ZoneId zone;

    public NotificationService 
    (
        BookingNotificationRepository bookingRepository,
        NotificationRepository notificationRepository,
        EmailSender emailSender,
        NotificationMessages messages,
        SmsSender smsSender,
        SmsMessages smsMessages,
        @Value("${notifications.admin-email:${ADMIN_EMAIL:admin@lapanchina.com}}") String adminEmail,
        @Value("${notifications.zone:America/Vancouver}") String zone) {

        this.bookingRepository = bookingRepository;
        this.notificationRepository = notificationRepository;
        this.emailSender = emailSender;
        this.messages = messages;
        this.smsSender = smsSender;
        this.smsMessages = smsMessages;
        this.adminEmail = adminEmail;
        this.zone = ZoneId.of(zone);
    }

    // US1 + US4: confirmation to the customer and alert to the administrator
    public void sendNewBookingNotifications()
    {
        processForCustomer(NotificationType.BOOKING_CONFIRMATION, bookingRepository.findPending(NotificationType.BOOKING_CONFIRMATION, NotificationStatus.FAILED, MAX_ATTEMPTS), messages::bookingConfirmation, smsMessages::bookingConfirmation, this::isCancelled);
        // The administrator alert is always via email
        processAdminAlert(NotificationType.ADMIN_NEW_BOOKING, bookingRepository.findPending(NotificationType.ADMIN_NEW_BOOKING, NotificationStatus.FAILED, MAX_ATTEMPTS), messages::adminNewBooking, this::isCancelled);
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

        processForCustomer(NotificationType.REMINDER, inWindow, messages::bookingReminder, smsMessages::bookingReminder, booking -> isCancelled(booking) || wasBookedInsideReminderWindow(booking));
    }

    // US3: Tells the customer when an administrator confirmed or cancelled the booking
    public void sendStatusNotifications()
    {
        sendStatusNotifications(ZonedDateTime.now(zone));
    }

    // Same as above but with a given "current time"
    public void sendStatusNotifications(ZonedDateTime now)
    {
        LocalDateTime nowLocal = now.toLocalDateTime();

        processForCustomer(NotificationType.STATUS_CONFIRMED,  bookingRepository.findPendingWithStatus(NotificationType.STATUS_CONFIRMED, NotificationStatus.FAILED, MAX_ATTEMPTS, List.of("confirmed")),
        messages::statusConfirmed, smsMessages::statusConfirmed, booking -> hasAlreadyStarted(booking, nowLocal));

        processForCustomer(NotificationType.STATUS_CANCELLED,  bookingRepository.findPendingWithStatus(NotificationType.STATUS_CANCELLED, NotificationStatus.FAILED, MAX_ATTEMPTS, List.of("cancelled", "canceled")),
        messages::statusCancelled, smsMessages::statusCancelled, booking -> hasAlreadyStarted(booking, nowLocal));
    }

    // True if the booking time has already passed (doesn't make sense to tell the customer about it)
    private boolean hasAlreadyStarted(Booking booking, LocalDateTime nowLocal)
    {
        return LocalDateTime.of(booking.getDate(), booking.getTime()).isBefore(nowLocal);
    }

    private void processForCustomer(NotificationType type, List<Booking> bookings, BiFunction<String, Booking, EmailMessage> emailTemplate, BiFunction<String, Booking, SmsMessage> smsTemplate, Predicate<Booking> shouldSkip)
    {
        for (Booking booking : bookings)
        {
            User customer = booking.getUser();
            boolean bySms = prefersSms(customer);
            Optional<String> phone = bySms ? PhoneNumbers.toE164(customer.getPhone()) : Optional.empty();
            String channel = bySms ? "SMS" : "EMAIL";
            String to = bySms ? phone.orElse(null) : customer.getEmail();

            Notification row = notificationRepository.findByBookingIdAndType(booking.getId(), type)
            .orElseGet(() -> new Notification(booking.getId(), type, channel, to, NotificationStatus.FAILED));
            // The customer may have changed the preference since a previous failed attempt
            row.setChannel(channel);
            row.setRecipient(to);

            if (shouldSkip.test(booking))
            {
                row.setStatus(NotificationStatus.SKIPPED);
                notificationRepository.save(row);
            }
            else if (bySms && phone.isEmpty())
            {
                // Preference choosed: SMS, but number is not valid: nothing is sent (phone number is not written in log)
                row.setStatus(NotificationStatus.SKIPPED);
                row.setError("Invalid phone number for SMS");
                notificationRepository.save(row);
                log.info("Skipped {} for booking {}: the customer chose SMS but the phone number is not valid", type, booking.getId());
            }
            else if (bySms)
            {
                deliver(row, () -> smsSender.send(smsTemplate.apply(phone.get(), booking)));
            }
            else
            {
                deliver(row, () -> emailSender.send(emailTemplate.apply(to, booking)));
            }
        }
    }

    // Sends the alert for the administrator for each booking in the list (always by email) 
    private void processAdminAlert(NotificationType type, List<Booking> bookings, BiFunction<String, Booking, EmailMessage> template, Predicate<Booking> shouldSkip)
    {
        for (Booking booking : bookings)
        {
            Notification row = notificationRepository.findByBookingIdAndType(booking.getId(), type)
            .orElseGet(() -> new Notification(booking.getId(), type, "EMAIL", adminEmail, NotificationStatus.FAILED));

            if (shouldSkip.test(booking))
            {
                row.setStatus(NotificationStatus.SKIPPED);
                notificationRepository.save(row);
            }
            else
            {
                deliver(row, () -> emailSender.send(template.apply(adminEmail, booking)));
            }
        }
    }

    private boolean prefersSms(User customer)
    {
        String preference = customer.getContactPreference();
        return preference != null && "SMS".equalsIgnoreCase(preference.trim());
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
    
    // Runs the given send action (email or SMS) and records the result in the notification row
    private void deliver(Notification row, Runnable send)
    {
        row.setAttempts(row.getAttempts() + 1); // 1st
        try {
            send.run();
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
