package group3.la_panchina.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


// Runs the notification job every X seconds
@Component
@ConditionalOnProperty(name = "notifications.enabled", havingValue = "true")
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationService service;
    private final NotificationBaseline baseline;
    private boolean baselineDone = false;

    public NotificationScheduler(NotificationService service, NotificationBaseline baseline)
    {
        this.service = service;
        this.baseline = baseline;
    }

    // Waits for the prev run to finish, then waits the interval again (15s by default)
    @Scheduled(fixedDelayString = "${notifications.poll-interval-ms:15000}")
    public void run()
    {
        try {
            // Old bookings must be marked before anything is send
            if (!baselineDone)
            {
                baseline.markExistingBookingAsHandled();
                baselineDone = true;
            }
            service.sendNewBookingNotifications();
        } catch (RuntimeException e) {
            // Never let an error stop the job from running again
            log.error("Notification job failed", e);
        }
    } 
}
