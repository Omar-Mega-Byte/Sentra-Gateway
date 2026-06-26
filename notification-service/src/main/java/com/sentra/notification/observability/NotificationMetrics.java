package com.sentra.notification.observability;

import com.sentra.notification.config.NotificationServiceProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Emits low-cardinality notification service metrics with the documented label
 * allowlist.
 */
@Component
public class NotificationMetrics {
    private final MeterRegistry registry;
    private final String environment;

    /**
     * Creates metrics helper bound to the process meter registry.
     *
     * @param registry Micrometer registry
     * @param properties service configuration
     */
    public NotificationMetrics(MeterRegistry registry, NotificationServiceProperties properties) {
        this.registry = registry;
        this.environment = properties.environment();
    }

    /** Records an HTTP request completion. */
    public void recordRequest(String operation, String statusClass, Duration duration) {
        Counter.builder("sentra_notification_requests_total")
                .tag("operation", operation).tag("status_class", statusClass).tag("environment", environment)
                .register(registry).increment();
        Timer.builder("sentra_notification_request_duration_seconds")
                .tag("operation", operation).tag("status_class", statusClass).tag("environment", environment)
                .register(registry).record(duration);
    }

    /** Records a local/test fault-control action. */
    public void recordFault(String operation, String fault, String result) {
        Counter.builder("sentra_notification_faults_total")
                .tag("operation", operation).tag("fault", fault).tag("result", result).tag("environment", environment)
                .register(registry).increment();
    }

    /** Records preference update outcomes. */
    public void recordPreferenceUpdate(String result) {
        Counter.builder("sentra_notification_preferences_updates_total")
                .tag("result", result).tag("environment", environment)
                .register(registry).increment();
    }

    /** Records repository operation outcomes. */
    public void recordRepositoryOperation(String operation, String result) {
        Counter.builder("sentra_notification_repository_operations_total")
                .tag("operation", operation).tag("result", result).tag("environment", environment)
                .register(registry).increment();
    }
}
