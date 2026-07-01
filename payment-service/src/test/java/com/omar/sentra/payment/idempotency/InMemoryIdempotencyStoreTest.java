package com.omar.sentra.payment.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.omar.sentra.payment.TestProperties;
import com.omar.sentra.payment.common.error.PaymentServiceException;
import com.omar.sentra.payment.observability.PaymentMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InMemoryIdempotencyStoreTest {
    private static final Instant NOW = Instant.parse("2026-06-16T12:00:00Z");

    @Test
    void replayConflictExpiryAndCapacityAreEnforced() {
        var properties = TestProperties.create(
                true,
                TestProperties.defaults().gateway().allowedRouteIds(),
                TestProperties.defaults().gateway().allowedPeers(),
                1,
                Duration.ofMinutes(1));
        InMemoryIdempotencyStore store = store(properties, NOW);

        IdempotentResult<String> first = store.execute(
                "payment-create",
                "partner-payment-create",
                "partner-acme",
                "key-000000000001",
                "fingerprint-a",
                () -> new StoredMutation<>("body-a", "/internal/v1/payments/1"));
        assertThat(first.replayed()).isFalse();
        assertThat(store.execute(
                        "payment-create",
                        "partner-payment-create",
                        "partner-acme",
                        "key-000000000001",
                        "fingerprint-a",
                        () -> new StoredMutation<>("body-b", "/internal/v1/payments/2")).body())
                .isEqualTo("body-a");
        assertThatThrownBy(() -> store.execute(
                        "payment-create",
                        "partner-payment-create",
                        "partner-acme",
                        "key-000000000001",
                        "fingerprint-b",
                        () -> new StoredMutation<>("body-c", "/internal/v1/payments/3")))
                .isInstanceOfSatisfying(PaymentServiceException.class,
                        exception -> assertThat(exception.code()).isEqualTo("PAY_IDEMPOTENCY_CONFLICT"));
        assertThatThrownBy(() -> store.execute(
                        "payment-create",
                        "partner-payment-create",
                        "partner-acme",
                        "key-000000000002",
                        "fingerprint-a",
                        () -> new StoredMutation<>("body-d", "/internal/v1/payments/4")))
                .isInstanceOfSatisfying(PaymentServiceException.class,
                        exception -> assertThat(exception.code()).isEqualTo("PAY_IDEMPOTENCY_CAPACITY_EXCEEDED"));

        InMemoryIdempotencyStore expired = store(properties, NOW.plusSeconds(61));
        expired.execute(
                "payment-create",
                "partner-payment-create",
                "partner-acme",
                "key-000000000001",
                "fingerprint-a",
                () -> new StoredMutation<>("new-body", "/internal/v1/payments/5"));
    }

    @Test
    void concurrentDuplicateCommitsExactlyOneMutation() throws Exception {
        InMemoryIdempotencyStore store = store(TestProperties.defaults(), NOW);
        AtomicInteger commits = new AtomicInteger();
        List<Callable<IdempotentResult<String>>> tasks = new ArrayList<>();
        for (int index = 0; index < 24; index++) {
            tasks.add(() -> store.execute(
                    "payment-create",
                    "partner-payment-create",
                    "partner-acme",
                    "concurrent-key-0001",
                    "same-fingerprint",
                    () -> new StoredMutation<>("body-" + commits.incrementAndGet(), "/internal/v1/payments/1")));
        }

        try (var executor = Executors.newFixedThreadPool(8)) {
            List<IdempotentResult<String>> results = executor.invokeAll(tasks).stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                    })
                    .toList();
            assertThat(results).extracting(IdempotentResult::body).containsOnly("body-1");
            assertThat(results.stream().filter(result -> !result.replayed())).hasSize(1);
        }
        assertThat(commits).hasValue(1);
    }

    private static InMemoryIdempotencyStore store(
            com.omar.sentra.payment.config.PaymentServiceProperties properties,
            Instant now) {
        return new InMemoryIdempotencyStore(
                properties,
                Clock.fixed(now, ZoneOffset.UTC),
                new PaymentMetrics(new SimpleMeterRegistry(), properties));
    }
}
