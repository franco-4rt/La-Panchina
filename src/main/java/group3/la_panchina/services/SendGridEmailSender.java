package group3.la_panchina.services;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// REAL SENDER: sends emails using SendGrid's API (SMTP blocked on Render). 
// This is the implementation that will be used in production if the notification.provider property is set to "sendgrid".
@Component 
@ConditionalOnProperty (name = "notification.provider", havingValue = "sendgrid")
public class SendGridEmailSender implements EmailSender 
{
    private final RestClient client;
    private final String fromEmail;
    private final String fromName;

    // Values come from .env locally or from Render's environment variables in production
    public SendGridEmailSender(
        @Value("${notifications.sendgrid.api.key}") String apiKey,
        @Value("${notifications.from-email}") String fromEmail,
        @Value("${notifications.from-name: La Panchina}") String fromName,
        @Value("${notifications.sendgrid.endpoint:https://api.sendgrid.com/v3/mail/send}") String endpoint) 
    {
        // Fail at startup if the API key is missing or blank
        if (apiKey == null || apiKey.isBlank()) 
        {
            throw new IllegalStateException("SendGrid API key must be provided");
        }
        this.fromEmail = fromEmail;
        this.fromName = fromName;

        // Timeouts so a slow SendGrid response can't hang the application indefinitely.
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(15));

        // Every request goes to the SendGrid endpoint with the API key
        this.client = RestClient.builder()
            .requestFactory(factory)
            .baseUrl(endpoint)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .build();
    }

    @Override 
    public void send(EmailMessage message) 
    {
        // Request body in the format SendGrid expects
        Map<String, Object> payload = Map.of(
            "personalizations", List.of(Map.of("to", List.of(Map.of("email", message.to())))),
            "from", Map.of("email", fromEmail, "name", fromName),
            "subject", message.subject(),
            "content", List.of(Map.of("type", "text/plain", "value", message.body()))
        );

        try 
        {
            // SendGrid answers 202 accepted for successful requests
            client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
        } 
        catch (RestClientException e) 
        {
            // Covers error responses (4xx, 5xx) and network issues
            throw new EmailSendException("Failed to send email via SendGrid: " + e.getMessage(), e);
        }
    }
}