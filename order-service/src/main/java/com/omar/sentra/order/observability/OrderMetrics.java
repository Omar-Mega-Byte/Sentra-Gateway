package com.omar.sentra.order.observability;

import com.omar.sentra.order.config.OrderServiceProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Low-cardinality order, idempotency, repository, and request metrics.
 */
@Component
public class OrderMetrics {
    private final MeterRegistry registry;
    private final String environment;

    public OrderMetrics(MeterRegistry registry, OrderServiceProperties properties) {
        this.registry = registry;
        environment = properties.environment();
    }

    public void request(String operation, String statusClass, Duration duration) {
        Counter.builder("sentra.order.requests")
                .tag("operation", operation)
                .tag("status_class", statusClass)
                .tag("environment", environment)
                .register(registry)
                .increment();
        Timer.builder("sentra.order.request.duration")
                .tag("operation", operation)
                .tag("status_class", statusClass)
                .tag("environment", environment)
                .register(registry)
                .record(duration);
    }

    public void creation(String result) {
        counter("sentra.order.creations", "result", result).increment();
    }

    public void idempotency(String result) {
        counter("sentra.order.idempotency", "result", result).increment();
    }

    public void repository(String operation, String result) {
        Counter.builder("sentra.order.repository.operations")
                .tag("operation", operation)
                .tag("result", result)
                .tag("environment", environment)
                .register(registry)
                .increment();
    }

    private Counter counter(String name, String tag, String value) {
        return Counter.builder(name)
                .tag(tag, value)
                .tag("environment", environment)
                .register(registry);
    }
}
