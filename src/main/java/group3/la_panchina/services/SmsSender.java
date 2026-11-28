package group3.la_panchina.services;

// Anything that can be send a text message (log-only for dev, Twilio for production)
public interface SmsSender {
    // Sends SMS, or throws SmsSendException if it could not be sent.
    void send(SmsMessage message);
}
