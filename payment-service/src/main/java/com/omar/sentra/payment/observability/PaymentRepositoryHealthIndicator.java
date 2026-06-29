package com.omar.sentra.payment.observability;

import com.omar.sentra.payment.payment.PaymentRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Readiness contributor for the required payment repository.
 */
@Component("paymentRepositoryHealthIndicator")
public class PaymentRepositoryHealthIndicator implements HealthIndicator {
    private final PaymentRepository repository;

    public PaymentRepositoryHealthIndicator(PaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Health health() {
        return repository.ready()
                ? Health.up().withDetail("mode", repository.mode()).build()
                : Health.down().withDetail("mode", repository.mode()).build();
    }
}
