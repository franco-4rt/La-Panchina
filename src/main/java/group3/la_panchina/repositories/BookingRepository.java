package group3.la_panchina.repositories;

import group3.la_panchina.models.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

// @Repository redundant at the moment because of JpaRepo
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    // Find all bookings for a specific date
    List<Booking> findByDate(LocalDate date);
    
    // Find all bookings with a specific status
    List<Booking> findByStatus(String status);
}