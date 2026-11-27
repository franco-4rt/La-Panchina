package group3.la_panchina.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component 
@ConditionalOnProperty (name = "notifications.sms.provider", havingValue = "log", matchIfMissing = true)
public class LogSmsSender implements SmsSender
{
    private static final Logger log = LoggerFactory.getLogger(LogSmsSender.class);

    @Override
    public void send(SmsMessage message)
    {
        log.info("[SMS - log only] to={}\n{}", message.to(), message.body());        
    }
}