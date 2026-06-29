package com.omar.sentra.payment.observability;

import com.omar.sentra.payment.config.PaymentServiceProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Low-cardinality payment, idempotency, repository, and request metrics.
 */
@Component
public class PaymentMetrics {
    private final MeterRegistry registry;
    private final String environment;

    public PaymentMetrics(MeterRegistry registry, PaymentServiceProperties properties) {
        this.registry = registry;
        environment = properties.environment();
    }

    public void request(String operation, String statusClass, Duration duration) {
        Counter.builder("sentra.payment.requests")
                .tag("operation", operation)
                .tag("status_class", statusClass)
                .tag("environment", environment)
                .register(registry)
                .increment();
        Timer.builder("sentra.payment.request.duration")
                .tag("operation", operation)
                .tag("status_class", statusClass)
                .tag("environment", environment)
                .register(registry)
                .record(duration);
    }

    public void mutation(String operation, String result) {
        Counter.builder("sentra.payment.mutations")
                .tag("operation", operation)
                .tag("result", result)
                .tag("environment", environment)
                .register(registry)
                .increment();
    }

    public void idempotency(String operation, String result) {
        Counter.builder("sentra.payment.idempotency")
                .tag("operation", operation)
                .tag("result", result)
                .tag("environment", environment)
                .register(registry)
                .increment();
    }

    public void repository(String operation, String result) {
        Counter.builder("sentra.payment.repository.operations")
                .tag("operation", operation)
                .tag("result", result)
                .tag("environment", environment)
                .register(registry)
                .increment();
    }
}
