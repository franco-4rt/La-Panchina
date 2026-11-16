package group3.la_panchina.services;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;


import group3.la_panchina.models.Booking;
import group3.la_panchina.models.Notification;
import group3.la_panchina.models.NotificationStatus;
import group3.la_panchina.models.NotificationType;
import group3.la_panchina.repositories.BookingRepository;
import group3.la_panchina.repositories.NotificationRepository;

// Runs once when the app starts. The first time notifications are enabled, it marks the bookings that already
// exist as handled (SKIPPED) so customers don't receive emails for old bookings. Only runs when notifications.enabled=true.
@Component
@ConditionalOnProperty(name = "notifications.enabled", havingValue = "true")
public class NotificationBaseline implements ApplicationRunner{
    
    private static final Logger log = LoggerFactory.getLogger(NotificationBaseline.class);

    private final BookingRepository bookingRepository;
    private final NotificationRepository notificationRepository;

    public NotificationBaseline(BookingRepository bookingRepository, NotificationRepository notificationRepository)
    {
        this.bookingRepository = bookingRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void run(ApplicationArguments args)
    {
        markExistingBookingAsHandled();
    }

    // Does nothing if the notifications table already has rows (baseline already done)
    public void markExistingBookingAsHandled()
    {
        if (notificationRepository.count() > 0)
            {
                return;
            }        
        
        List<Notification> rows = new ArrayList<>();
        for (Booking booking : bookingRepository.findAll())
        {
            rows.add(skipped(booking.getId(), NotificationType.BOOKING_CONFIRMATION));
            rows.add(skipped(booking.getId(), NotificationType.ADMIN_NEW_BOOKING));

            // A booking that already cancelled/confirmed must not trigger a status email
            if (BookingStatuses.isConfirmed(booking.getStatus()))
            {
                rows.add(skipped(booking.getId(), NotificationType.STATUS_CONFIRMED));
            } else if (BookingStatuses.isCancelled(booking.getStatus()))
            {
                rows.add(skipped(booking.getId(), NotificationType.STATUS_CANCELLED));
            }
        }
        notificationRepository.saveAll(rows);
        log.info("Notification baseline: {} existing notifications marked as skipped", rows.size());
    }

    // Helper: Row meaning "this notification will never be sent"
    private Notification skipped(Long bookingID, NotificationType type)
    {
        return new Notification(bookingID, type, "EMAIL", null, NotificationStatus.SKIPPED);
    }
}
