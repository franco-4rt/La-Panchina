package group3.la_panchina.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty (name = "notifications.provider", havingValue = "log", matchIfMissing = true)
public class LogEmailSender implements EmailSender 
{
    
    private static final Logger log = LoggerFactory.getLogger(LogEmailSender.class);

    @Override
    public void send(EmailMessage message) 
    {
        log.info("[EMAIL - log only] to={} | subject={}\n{}", message.to(), message.subject(), message.body());
    }
}
