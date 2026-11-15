package group3.la_panchina.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import group3.la_panchina.models.Notification;
import group3.la_panchina.models.NotificationType;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    boolean existsByBookingIdAndType(Long bookingId, NotificationType type);
}
