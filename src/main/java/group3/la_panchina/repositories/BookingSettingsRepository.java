package group3.la_panchina.repositories;

import group3.la_panchina.models.BookingSettings;
import org.springframework.data.jpa.repository.JpaRepository;

// JpaRepository gives us findById() and save() for free.
// We only ever read/write the row with id = 1.
public interface BookingSettingsRepository extends JpaRepository<BookingSettings, Long> {
    
}