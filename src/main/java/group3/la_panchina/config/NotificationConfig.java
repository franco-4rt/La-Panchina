package group3.la_panchina.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Turns on scheduled jobs for the notification system.
// Only active when notifications.enabled=true (off by default, so tests and local runs without notification settings are not affected).
@Configuration 
@EnableScheduling 
@ConditionalOnProperty(name = "notifications.enabled", havingValue = "true")
public class NotificationConfig {
}
