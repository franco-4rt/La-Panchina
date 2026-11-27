package group3.la_panchina.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Admin-configurable booking rules. Single row table (id = 1).
 */
@Entity
@Table(name = "booking_settings")
public class BookingSettings {

    @Id
    private Long id = 1L;

    // How far ahead customers may book
    @Column(nullable = false)
    private int maxDaysAhead = 60;

    // Maximum number of guests (covers) that can be seated in one clock-hour slot.
    // Default 40 until an admin changes it.
    @Column(nullable = false)
    private int maxCoversPerSlot = 40;

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

    public int getMaxCoversPerSlot() {
        return maxCoversPerSlot;
    }

    public void setMaxCoversPerSlot(int maxCoversPerSlot) {
        this.maxCoversPerSlot = maxCoversPerSlot;
    }
}