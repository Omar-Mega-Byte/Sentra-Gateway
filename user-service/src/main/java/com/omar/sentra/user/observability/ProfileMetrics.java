package com.omar.sentra.user.observability;

import com.omar.sentra.user.config.UserServiceProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Low-cardinality profile operation metrics.
 */
@Component
public class ProfileMetrics {
    private final MeterRegistry registry;
    private final String environment;

    public ProfileMetrics(MeterRegistry registry, UserServiceProperties properties) {
        this.registry = registry;
        this.environment = properties.environment();
    }

    public void lookup(String operation, String result) {
        counter("sentra.user.profile.lookups", operation, result).increment();
    }

    public void update(String result) {
        counter("sentra.user.profile.updates", "profile-update", result).increment();
    }

    private Counter counter(String name, String operation, String result) {
        return Counter.builder(name)
                .tag("operation", operation)
                .tag("result", result)
                .tag("environment", environment)
                .register(registry);
    }
}
