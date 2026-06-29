package com.omar.sentra.payment.idempotency;

import static com.omar.sentra.payment.common.error.ServiceErrors.idempotencyCapacityExceeded;
import static com.omar.sentra.payment.common.error.ServiceErrors.idempotencyConflict;

import com.omar.sentra.payment.config.PaymentServiceProperties;
import com.omar.sentra.payment.observability.PaymentMetrics;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Synchronized in-memory idempotency store scoped by route, client, and key.
 */
@Component
public class InMemoryIdempotencyStore implements IdempotencyStore {
    private final PaymentServiceProperties properties;
    private final Clock clock;
    private final PaymentMetrics metrics;
    private final Map<String, Record> records = new HashMap<>();
    private boolean ready = true;

    public InMemoryIdempotencyStore(PaymentServiceProperties properties, Clock clock, PaymentMetrics metrics) {
        this.properties = properties;
        this.clock = clock;
        this.metrics = metrics;
    }

    @Override
    public synchronized <T> IdempotentResult<T> execute(
            String operation,
            String routeId,
            String clientId,
            String key,
            String fingerprint,
            Supplier<StoredMutation<T>> mutation) {
        cleanupExpired();
        String scope = routeId + "|" + clientId + "|" + key;
        Record existing = records.get(scope);
        if (existing != null) {
            if (!existing.fingerprint().equals(fingerprint)) {
                metrics.idempotency(operation, "conflict");
                throw idempotencyConflict();
            }
            metrics.idempotency(operation, "replay");
            @SuppressWarnings("unchecked")
            T body = (T) existing.body();
            return new IdempotentResult<>(body, existing.location(), true);
        }
        if (records.size() >= properties.idempotency().maxRecords()) {
            metrics.idempotency(operation, "capacity-exceeded");
            throw idempotencyCapacityExceeded();
        }
        StoredMutation<T> created = mutation.get();
        Instant expiresAt = Instant.now(clock).plus(properties.idempotency().retention());
        records.put(scope, new Record(fingerprint, created.body(), created.location(), expiresAt));
        metrics.idempotency(operation, "created");
        return new IdempotentResult<>(created.body(), created.location(), false);
    }

    @Override
    @Scheduled(fixedDelayString = "${IDEMPOTENCY_CLEANUP_INTERVAL:5m}")
    public synchronized void cleanupExpired() {
        Instant now = Instant.now(clock);
        Iterator<Map.Entry<String, Record>> iterator = records.entrySet().iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().getValue().expiresAt().isAfter(now)) {
                iterator.remove();
            }
        }
    }

    @Override
    public synchronized void reset() {
        records.clear();
        ready = true;
    }

    @Override
    public synchronized boolean ready() {
        return ready;
    }

    private record Record(String fingerprint, Object body, String location, Instant expiresAt) {}
}
