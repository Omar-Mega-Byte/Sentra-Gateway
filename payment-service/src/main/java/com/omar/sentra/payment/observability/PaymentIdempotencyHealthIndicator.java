package com.omar.sentra.payment.observability;

import com.omar.sentra.payment.idempotency.IdempotencyStore;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Readiness contributor for mutation idempotency safety.
 */
@Component("paymentIdempotencyHealthIndicator")
public class PaymentIdempotencyHealthIndicator implements HealthIndicator {
    private final IdempotencyStore idempotencyStore;

    public PaymentIdempotencyHealthIndicator(IdempotencyStore idempotencyStore) {
        this.idempotencyStore = idempotencyStore;
    }

    @Override
    public Health health() {
        return idempotencyStore.ready() ? Health.up().build() : Health.down().build();
    }
}
