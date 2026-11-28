package group3.la_panchina.services;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class TwilioSmsSenderTest {
    private HttpServer server;
    private final AtomicReference<String> body = new AtomicReference<>();
    private final AtomicReference<String> auth = new AtomicReference<>();
    private final AtomicReference<String> contentType = new AtomicReference<>();
    private volatile int status = 201;

    // Starts a fake Twilio server that records what it receives and responds with the given status code. The server is stopped after each test.
    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/2010-04-01/Accounts/AC123/Messages.json", exchange -> {
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
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
    private TwilioSmsSender sender()
    {
        String url = "http://localhost:" + server.getAddress().getPort() + "/2010-04-01";
        return new TwilioSmsSender("AC123", "test-token", "+16045559999", url);
    }

    // *SC = Success Case, FC = Failure Case

    // SC: Sends the login (account SID + token) and the destination, sender and text as a form to the fake Twilio server
    @Test
    void sendsAnAuthorizedFormRequest()
    {
        sender().send(new SmsMessage("+16043214567", "Hola mundo"));

        String expectedAuth = "Basic " + Base64.getEncoder().encodeToString("AC123:test-token".getBytes(StandardCharsets.UTF_8));
        assertEquals(expectedAuth, auth.get());
        assertTrue(contentType.get().startsWith("application/x-www-form-urlencoded"));

        String form = URLDecoder.decode(body.get(), StandardCharsets.UTF_8);
        assertTrue(form.contains("To=+16043214567"));
        assertTrue(form.contains("From=+16045559999"));
        assertTrue(form.contains("Body=Hola mundo"));
    }

    // FC: If Twilio returns a non-2xx status code, the sender throws an exception.
    @Test
    void aNon2xxResponseThrowsSmsSendException()
    {
        status = 401; // Unauthorized

        assertThrows(SmsSendException.class, () -> sender().send(new SmsMessage("+16043214567", "Hola")));
    }

    // FC: Missing account SID, token or from number = stops the application at startup.
    @Test 
    void missingCredentialsFailFast()
    {
        assertThrows(IllegalStateException.class, () -> new TwilioSmsSender(" ", "token", "+16045559999", "http://localhost:x"));
        assertThrows(IllegalStateException.class, () -> new TwilioSmsSender("AC123", " ", "+16045559999", "http://localhost:x"));
        assertThrows(IllegalStateException.class, () -> new TwilioSmsSender("AC123", "token", " ", "http://localhost:x"));
    }

}
