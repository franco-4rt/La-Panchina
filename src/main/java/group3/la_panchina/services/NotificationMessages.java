package group3.la_panchina.services;

import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import group3.la_panchina.models.Booking;
import group3.la_panchina.models.User;

// Builds the text of the notification emails sent to customers and administrators. Each method corresponds to a specific use case (US1, US2, US3, US4) 
// and returns an EmailMessage object containing the recipient, subject, and body of the email.

// Only builds the message; sending is done by an EmailSender.

// Since La Panchina is a Spanish-speaking restaurant, all messages are written in Spanish. All of them do what's described in the user stories.

@Component 
public class NotificationMessages {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final String siteUrl;

    public NotificationMessages(@Value("${notifications.site-url:https://la-panchina.onrender.com}") String siteUrl) {
        this.siteUrl = siteUrl;
    }

    // US1: Send notification to customer right after booking is made
    public EmailMessage bookingConfirmation(String to, Booking booking) {
        String body ="""
                Hola %s,
                
                Gracias por reservar en La Panchina! Hemos recibido tu reserva:
                
                %s

                Te haremos saber tan pronto como tu reserva sea confirmada.

                La Panchina""".formatted(booking.getName(), details(booking));
        return new EmailMessage(to, "Hemos recibido tu reserva en La Panchina", body);
    }

    // US4: Sent email to administrator when a customer submits a booking
    public EmailMessage adminNewBooking(String to, Booking booking) {
        User customer = booking.getUser();
        String body = """
                Hola Administrador,
                
                Un cliente ha hecho una reserva en La Panchina!
                
                %s
                Estado: %s

                Nombre del cliente: %s
                Email del cliente: %s
                Número de teléfono del cliente: %s
                
                Por favor, confirme la reserva lo antes posible a través del panel de administración: %s/admin/bookings""".formatted(details(booking), booking.getStatus(), booking.getName(), customer.getEmail(), customer.getPhone(), siteUrl);
        return new EmailMessage(to, "Nueva reserva #" + booking.getId() + " - " + booking.getDate().format(DATE_FORMAT), body);
    }

    // US2: Reminder email sent to customer few hours prior booking time
    public EmailMessage bookingReminder(String to, Booking booking) {
        String body = """
                Hola %s,
                
                Este es un recordatorio de su reserva en La Panchina:
                
                %s

                Esperamos verte pronto!

                La Panchina""".formatted(booking.getName(), details(booking));
        return new EmailMessage(to, "Recordatorio: Tu reserva en La Panchina", body);
    }

    // US3: Send notification to customer when booking is confirmed by administrator
    public EmailMessage statusConfirmed(String to, Booking booking) {
        String body = """
                Hola %s,
                
                Tu reserva en La Panchina ha sido confirmada!
                
                %s

                Esperamos verte pronto!

                La Panchina""".formatted(booking.getName(), details(booking));
        return new EmailMessage(to, "Tu reserva en La Panchina ha sido confirmada!", body);
    }

    // US3: Send notification to customer when booking is cancelled by administrator
    public EmailMessage statusCancelled(String to, Booking booking) {
        String body = """
                Hola %s,
                
                Lamentamos informarte que tu reserva en La Panchina ha sido cancelada.
                
                %s

                Si quieres reservar para otro momento, puedes hacerlo aquí: %s/reservations

                La Panchina""".formatted(booking.getName(), details(booking), siteUrl);
        return new EmailMessage(to, "Tu reserva en La Panchina ha sido cancelada", body);
    }

    // Helper: Booking details block shared by all messages
    private String details(Booking booking) 
    {
        return """
                Reserva #%s
                Fecha: %s
                Hora: %s
                Número de personas: %s""".formatted(booking.getId(), booking.getDate().format(DATE_FORMAT), booking.getTime().format(TIME_FORMAT), booking.getPartySize());
    }
}
