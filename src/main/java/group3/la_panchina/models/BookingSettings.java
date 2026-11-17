package group3.la_panchina.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Holds admin-configurable booking rules.
 * This table only ever has ONE row (id = 1), which is a common pattern
 * for "application settings" that an admin can edit at runtime.
 */
@Entity
@Table(name = "booking_settings")
public class BookingSettings {

    // Fixed id of 1: there is only ever one settings row.
    // No @GeneratedValue because we assign the id ourselves.
    @Id
    private Long id = 1L;

    // How many days into the future a customer is allowed to book.
    // Default is 60 days until an admin changes it.
    @Column(nullable = false)
    private int maxDaysAhead = 60;

    // JPA requires a no-argument constructor
    public BookingSettings() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getMaxDaysAhead() {
        return maxDaysAhead;
    }

    public void setMaxDaysAhead(int maxDaysAhead) {
        this.maxDaysAhead = maxDaysAhead;
    }
}