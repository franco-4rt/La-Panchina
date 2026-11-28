package group3.la_panchina.services;

import java.text.Normalizer;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import group3.la_panchina.models.Booking;

// Builds the text of the SMS notifications sent to customers who chose SMS as their contact preference.
// Only builds the message; sending is done by an SmsSender.
// There is no administrator message: the admin alert is always sent via email.
// Text are written in Spanish without accents: they make the messages longer and expensive. They are kept short for the same reason.
// Using accents make the SMS use a mode where only 70 characters fit instead of the 160 using plain characters.

@Component 
public class SmsMessages {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final String siteUrl;

    public SmsMessages(@Value("${notifications.site-url:https://la-panchina.onrender.com})") String siteUrl)
    {
        this.siteUrl = siteUrl;
    }

    // US5: Sent to the customer right after the booking is made
    public SmsMessage bookingConfirmation(String to, Booking booking)
    {
        String body = "La Panchina: hemos recibido tu reserva #%s para el %s a las %s (%s). Te avisaremos cuando sea confirmada."
        .formatted(booking.getId(), date(booking), time(booking), people(booking));
        return new SmsMessage(to, plain(body));
    }

    // US6: Sent to the customer a few hours before the booking time
    public SmsMessage bookingReminder(String to, Booking booking)
    {
        String body = "La Panchina: recordatorio de tu reserva #%s para el %s a las %s (%s). Te esperamos!"
        .formatted(booking.getId(), date(booking), time(booking), people(booking));
        return new SmsMessage(to, plain(body));
    }

    // US7: Sent to the customer when the administrator confirms the booking
    public SmsMessage statusConfirmed(String to, Booking booking)
    {
        String body = "La Panchina: tu reserva #%s para el %s a las %s (%s) ha sido confirmada. Te esperamos!"
        .formatted(booking.getId(), date(booking), time(booking), people(booking));
        return new SmsMessage(to, plain(body));
    }

    // US7: Sent to the customer when the administrator cancels the booking (includes the link to book again)
    public SmsMessage statusCancelled (String to, Booking booking)
    {
        String body = "La Panchina: tu reserva #%s del %s a las %s fue cancelada. Reserva de nuevo: %s/reservations"
        .formatted(booking.getId(), date(booking), time(booking), siteUrl);
        return new SmsMessage(to, plain(body));
    }

    // Helper: Booking date as dd/MM/yyyy
    private String date(Booking booking)
    {
        return booking.getDate().format(DATE_FORMAT);
    }

    // Helper: Booking time as HH:mm
    private String time(Booking booking)
    {
        return booking.getTime().format(TIME_FORMAT);
    }

    // Helper: "1 persona" or "N personas"
    private String people(Booking booking)
    {
        int size = booking.getPartySize();
        return size == 1 ? "1 persona" : size + " personas";
    }

    // Helper: removes accents (e.g. "á" -> "a") and any other character outside plain ASCII
    private String plain(String text)
    {
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.replaceAll("[^\\x20-\\x7E]", "");
    }
}
