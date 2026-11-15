package group3.la_panchina.services;

// Thrown when an email couldn't be sent (caller marks it as FAILED)
public class EmailSendException extends RuntimeException {
    public EmailSendException(String message) {
        super(message);
    }

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
