package group3.la_panchina.repositories;

import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import group3.la_panchina.models.Notification;
import group3.la_panchina.models.NotificationStatus;
import group3.la_panchina.models.NotificationType;

// Test the notifications table with in-memory H2 database
@DataJpaTest
class NotificationRepositoryTest {
    
    @Autowired
    private NotificationRepository repo;

    // Builds an already-sent email notification for a booking
    private Notification sent(Long bookingId, NotificationType type) {
        return new Notification(bookingId, type, "EMAIL", "a@b.com", NotificationStatus.SENT);
    }
    
    // SUCCESS CASE: only the exact (booking, type) oaur that was saved is found
    @Test 
    void existsByBookingIdAndTypeIsTrueOnlyForTheSavedPair() {
        repo.saveAndFlush(sent(1L, NotificationType.BOOKING_CONFIRMATION));

        assertTrue(repo.existsByBookingIdAndType(1L, NotificationType.BOOKING_CONFIRMATION));
        assertFalse(repo.existsByBookingIdAndType(1L, NotificationType.REMINDER));
        assertFalse(repo.existsByBookingIdAndType(2L, NotificationType.BOOKING_CONFIRMATION));
    }

    @Test
    void sameTypeTwiceForTheSameBookingIsRejected() {
        repo.saveAndFlush(sent(1L,NotificationType.REMINDER));

        assertThrows(DataIntegrityViolationException.class, () -> repo.saveAndFlush(sent(1L, NotificationType.REMINDER)));
    }
}
