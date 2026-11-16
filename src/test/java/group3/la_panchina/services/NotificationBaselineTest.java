package group3.la_panchina.services;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalTime;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import group3.la_panchina.models.Booking;
import group3.la_panchina.models.NotificationStatus;
import group3.la_panchina.models.NotificationType;
import group3.la_panchina.models.User;
import group3.la_panchina.repositories.BookingRepository;
import group3.la_panchina.repositories.NotificationRepository;
import group3.la_panchina.repositories.UserRepository;


// Test the baseline with the in-memory H2 database
@DataJpaTest(properties = "notifications.enabled=true")
@Import(NotificationBaseline.class)
class NotificationBaselineTest {
    
    @Autowired
    private NotificationBaseline baseline;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired 
    private UserRepository userRepository;
    @Autowired
    private NotificationRepository notificationRepository;


    // Helper: Saves a customer and a booking with the given status (copied helper from NotificationMessagesTest.java since shares most of the structure)
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

    // *SC = SUCCESS CASE , FC = FAILURE CASE

    // SC: Existing bookings get SKIPPED rows, including the status that they already have
    @Test
    void existingBookingsAreMarkedAsSkipped() 
    {
        Booking pending = saveBooking("pending");
        Booking confirmed = saveBooking("confirmed");
        Booking cancelled = saveBooking("cancelled");

        baseline.markExistingBookingAsHandled();

        assertTrue(notificationRepository.existsByBookingIdAndType(pending.getId(), NotificationType.BOOKING_CONFIRMATION));
        assertTrue(notificationRepository.existsByBookingIdAndType(pending.getId(), NotificationType.ADMIN_NEW_BOOKING));
        assertFalse(notificationRepository.existsByBookingIdAndType(pending.getId(), NotificationType.STATUS_CONFIRMED));

        assertTrue(notificationRepository.existsByBookingIdAndType(confirmed.getId(), NotificationType.STATUS_CONFIRMED));
        assertFalse(notificationRepository.existsByBookingIdAndType(confirmed.getId(), NotificationType.STATUS_CANCELLED));

        assertTrue(notificationRepository.existsByBookingIdAndType(cancelled.getId(), NotificationType.STATUS_CANCELLED));

        // 2 rows for the pending booking + 3 for the confirmed + 3 for the cancelled one
        assertEquals(8, notificationRepository.count());
        notificationRepository.findAll().forEach(n -> assertEquals(NotificationStatus.SKIPPED, n.getStatus()));
    }

    // SC: Once the baseline is done, running it again does not touch newer bookings
    @Test 
    void doesNothingWhenNotificationsAlreadyExists() 
    {
        saveBooking("pending");
        baseline.markExistingBookingAsHandled();
        long rowsAfterFirstRun = notificationRepository.count();

        saveBooking("confirmed");
        baseline.markExistingBookingAsHandled();

        assertEquals(rowsAfterFirstRun, notificationRepository.count());
    }
}