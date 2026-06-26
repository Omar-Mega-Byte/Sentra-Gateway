package com.sentra.notification.observability;

import com.sentra.notification.notification.NotificationRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Readiness health indicator for the deterministic notification repository.
 */
@Component("notificationRepository")
public class NotificationRepositoryHealthIndicator implements HealthIndicator {
    private final NotificationRepository repository;

    /** @param repository notification repository */
    public NotificationRepositoryHealthIndicator(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Health health() {
        return repository.available()
                ? Health.up().withDetail("mode", "memory").build()
                : Health.down().withDetail("mode", "memory").build();
    }
}
