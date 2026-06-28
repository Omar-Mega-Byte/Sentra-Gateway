package com.omar.sentra.order.observability;

import com.omar.sentra.order.order.OrderRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Readiness contributor for the required order repository.
 */
@Component("orderRepositoryHealthIndicator")
public class OrderRepositoryHealthIndicator implements HealthIndicator {
    private final OrderRepository repository;

    public OrderRepositoryHealthIndicator(OrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public Health health() {
        return repository.ready()
                ? Health.up().withDetail("mode", repository.mode()).build()
                : Health.down().withDetail("mode", repository.mode()).build();
    }
}
