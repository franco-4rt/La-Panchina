package group3.la_panchina.services;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class SendGridEmailSenderTest {
    
    private HttpServer server;
    private final AtomicReference<String> body = new AtomicReference<>();
    private final AtomicReference<String> auth = new AtomicReference<>();
    private volatile int status = 202;

    // Starts a fake SendGrid server that records what it receives and responds with the given status code. The server is stopped after each test.
    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v3/mail/send", exchange -> {
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }
    
    // Helper: a sender that points to the fake SendGrid server instead of the real one. Uses a dummy API key and from email.
    private SendGridEmailSender sender()
    {
        String url = "http://localhost:" + server.getAddress().getPort() + "/v3/mail/send";
        return new SendGridEmailSender("test-api-key", "from@example.com", "La Panchina", url);
    }

    // SUCCESS CASE: Sends the API key and the email message in JSON format to the fake SendGrid server. The message text is escaped properly.
    @Test
    void sendsAnAuthorizedJsonRequestWithEscapedText() {
        sender().send(new EmailMessage("a@b.com", "Hello \"World\"", "Line1\nLine2"));

        assertEquals("Bearer test-api-key", auth.get());
        assertTrue(body.get().contains("\"email\":\"a@b.com\""));
        assertTrue(body.get().contains("Hello \\\"World\\\""));
        assertTrue(body.get().contains("Line1\\nLine2"));
    }

    // FAILURE CASE 1: If SendGrid returns a non-2xx status code, the sender throws an exception. 
    @Test
    void aNon2xxResponseThrowsEmailSendException() 
    {
        status = 401; // Unauthorized

        assertThrows(EmailSendException.class, () -> sender().send(new EmailMessage("a@b.com", "Subject", "Body")));
    }

    // FAILURE CASE 2: Missing API key = stops the application at startup.
    @Test
    void aBlankApiKeyFailsFast() 
    {
        assertThrows(IllegalStateException.class, () -> new SendGridEmailSender(" ", "from@example.com", "La Panchina", "http://localhost:x"));
    }
}