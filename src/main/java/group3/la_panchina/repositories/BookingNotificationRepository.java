package group3.la_panchina.repositories;

import java.util.List;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import group3.la_panchina.models.Booking;
import group3.la_panchina.models.NotificationStatus;
import group3.la_panchina.models.NotificationType;

// Read-only queries on bookings used by the notification system.
// Kept separate from BookingRepository so that file stays untouched (Avoiding merge conflicts)
public interface BookingNotificationRepository extends Repository<Booking, Long> {
    
    @Query("""
            select b from Booking b join fetch b.user
            where not exists (
                select 1 from Notification n
                where n.bookingId = b.id and n.type = :type
                    and (n.status <> :failed or n.attempts >= :maxAttempts))
            order by b.id""")
    List<Booking> findPending(@Param("type") NotificationType type, @Param("failed") NotificationStatus failed, @Param("maxAttempts") int maxAttempts);

    // Same as findPending, but only bookings with a date inside the given range
    @Query("""
        select b from Booking b join fetch b.user
        where b.date between :fromDate and :toDate
            and not exists (
                select 1 from Notification n
                where n.bookingId = b.id and n.type = :type
                    and (n.status <> :failed or n.attempts >= :maxAttempts))
        order by b.id""")
    List<Booking> findPendingBetweenDates(@Param("type") NotificationType type, @Param("failed") NotificationStatus failed, @Param("maxAttempts") int maxAttempt, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

}
