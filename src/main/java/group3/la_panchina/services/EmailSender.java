package group3.la_panchina.services;

// Anything that can send an email (log-only for dev, SendGrid for production)
public interface EmailSender {
    // Sends the email, or throws EmailSendException if it could not be sent.
    void send(EmailMessage message);
}
