package group3.la_panchina.repositories;

import group3.la_panchina.models.Booking;
import group3.la_panchina.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// @Repository redundant at the moment because of JpaRepo
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    // Find all bookings for a specific date
    List<Booking> findByDate(LocalDate date);
    
    // Find all bookings with a specific status
    List<Booking> findByStatus(String status);
    
    // Find all bookings for a specific user, ordered by date descending (newest first)
    List<Booking> findByUserOrderByDateDesc(User user);

    // Bookings on a given date whose start time falls inside [start, end).
    // Used to total the seats already taken in one clock-hour slot.
    List<Booking> findByDateAndTimeGreaterThanEqualAndTimeLessThan(
            LocalDate date, LocalTime start, LocalTime end);
}