package group3.la_panchina.services;

import java.net.http.HttpClient;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// REAL SMS SENDER: sends text messages using Twilio's REST API over HTTPS.
// Used only if the notifications.sms.provider is set to "twilio" (otherwise LogSmsSender is used)
@Component
@ConditionalOnProperty (name = "notifications.sms.provider", havingValue = "twilio")
public class TwilioSmsSender implements SmsSender 
{
    private static final Logger log = LoggerFactory.getLogger(TwilioSmsSender.class);

    private final RestClient client;
    private final String accountSid;
    private final String fromNumber;

    public TwilioSmsSender(
        @Value("${notifications.sms.twilio.account-sid}") String accountSid,
        @Value("${notifications.sms.twilio.auth-token}") String authToken,
        @Value("${notifications.sms.from-number}") String fromNumber,
        @Value("${notifications.sms.twilio.endpoint:https://api.twilio.com/2010-04-01}") String endpoint)
    {
        if (isBlank(accountSid) || isBlank(authToken) || isBlank(fromNumber))
        {
            throw new IllegalStateException("Twilio account SID, auth token and from-number must be provided");
        }
        this.accountSid = accountSid.trim();
        this.fromNumber = fromNumber.trim();

        // Timeouts so a slow Twilio response can't hang the application indefinitely
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(15));

        this.client = RestClient.builder()
        .requestFactory(factory)
        .baseUrl(endpoint)
        .defaultHeaders(headers -> headers.setBasicAuth(this.accountSid, authToken.trim()))
        .build();

        // Helps to confirm in Render logs which sender is active 
        log.info("SMS Sender: Twilio is active (from {})", this.fromNumber);
    }

    @Override
    public void send(SmsMessage message)
    {
        // Twilio expects a form (not JSON (compared to SendGrid)) with the destination, the sender, and the message
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", message.to());
        form.add("From", fromNumber);
        form.add("Body", message.body());

        try
        {
            // Twilio answers 201 Created when it accepts the message
            client.post()
            .uri("/Accounts/{sid}/Messages.json", accountSid)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .toBodilessEntity();
        }
        catch(RestClientException e)
        {
            // Covers error responses (4xx, 5xx) and network issues
            throw new SmsSendException("Failed to send SMS via Twilio: " + e.getMessage(), e);
        }
    }
    
    private static boolean isBlank(String text)
    {
        return text == null || text.isBlank();
    }
}
