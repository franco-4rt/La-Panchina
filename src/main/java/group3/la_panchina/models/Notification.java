package group3.la_panchina.models;

import java.time.Instant;
import jakarta.persistence.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// Log of every nofitification the system sends
@Entity
@Table(name = "notifications",
    uniqueConstraints = @UniqueConstraint (
        name = "uk_notification_booking_type",
        columnNames = {"booking_id", "type"}))


public class Notification {
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    // Only email for now (to be considered)
    @Column (nullable = false, length = 10)
    private String channel;

    // Email address the notification was sent to
    @Column (length = 120)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column (nullable = false, length = 10)
    private NotificationStatus status;

    @Column (nullable = false)
    private int attempts = 0;

    @Column (nullable = false)
    private Instant createdAt = Instant.now();

    // When it was actually sent (null if not sent yet)
    private Instant sentAt;

    // Error msg
    @Column(length = 500)
    private String error;

    // JPA requires a default no-argument constructor
    protected Notification() {}

    public Notification(Long bookingId, NotificationType type, String channel, String recipient, NotificationStatus status) {
        this.bookingId = bookingId;
        this.type = type;
        this.channel = channel;
        this.recipient = recipient;
        this.status = status;
    }


// Getters and setters
   public Long getId() {return id;}
   public Long getBookingId() {return bookingId;}
   public NotificationType getType() {return type;}
   public String getChannel() {return channel;}
   public String getRecipient() {return recipient;}
   public NotificationStatus getStatus() {return status;}
   public void setStatus(NotificationStatus status) {this.status = status;}
   public int getAttempts() {return attempts;}
   public void setAttempts(int attempts) {this.attempts = attempts;}
   public Instant getCreatedAt() {return createdAt;}
   public Instant getSentAt() {return sentAt;}
   public void setSentAt(Instant sentAt) { this.sentAt = sentAt;}
   public String getError() {return error;}
   public void setError(String error) {this.error = error;}
}
