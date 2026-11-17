package group3.la_panchina.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import group3.la_panchina.models.Notification;
import group3.la_panchina.models.NotificationType;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // True is this type of notification was already recorded for the booking (used to avoid sending the same email twice)
    boolean existsByBookingIdAndType(Long bookingId, NotificationType type);

    // The notification row of a booking and type, if any (used to retry failed emails)
    Optional<Notification> findByBookingIdAndType(Long bookingId, NotificationType type);
}
