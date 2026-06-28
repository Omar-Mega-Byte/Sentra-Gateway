package com.omar.sentra.order.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.omar.sentra.order.TestProperties;
import com.omar.sentra.order.common.error.OrderServiceException;
import com.omar.sentra.order.observability.OrderMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class InMemoryOrderRepositoryTest {
    private static final Instant NOW = Instant.parse("2026-06-15T12:00:00Z");

    @Test
    void filtersByOwnerAndUsesFixedDescendingOrdering() {
        InMemoryOrderRepository repository = repository(true, 10000, Duration.ofHours(24));

        OrderPage<Order> page = repository.findOwned(
                OrderSeedData.DEMO_TENANT,
                OrderSeedData.DEMO_SUBJECT,
                null,
                0,
                20);

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.items()).extracting(Order::id)
                .containsExactly(OrderSeedData.OWNED_CREATED_ID, OrderSeedData.OWNED_COMPLETED_ID);
        assertThat(repository.findOwnedById(
                        OrderSeedData.FOREIGN_SUBJECT_ID,
                        OrderSeedData.DEMO_TENANT,
                        OrderSeedData.DEMO_SUBJECT))
                .isEmpty();
        assertThat(repository.findOwnedById(
                        OrderSeedData.FOREIGN_TENANT_ID,
                        OrderSeedData.DEMO_TENANT,
                        OrderSeedData.DEMO_SUBJECT))
                .isEmpty();
    }

    @Test
    void replayConflictExpiryAndCapacityAreEnforced() {
        InMemoryOrderRepository repository = repository(false, 1, Duration.ofMinutes(1));
        IdempotencyRequest key = key("key-000000000001", "fingerprint-a");
        Order original = order();

        assertThat(repository.create(original, key, NOW).replayed()).isFalse();
        assertThat(repository.create(order(), key, NOW.plusSeconds(1)).order().id())
                .isEqualTo(original.id());
        assertThatThrownBy(() -> repository.create(
                        order(),
                        key("key-000000000001", "fingerprint-b"),
                        NOW.plusSeconds(2)))
                .isInstanceOfSatisfying(OrderServiceException.class,
                        exception -> assertThat(exception.code()).isEqualTo("ORD_IDEMPOTENCY_CONFLICT"));
        assertThatThrownBy(() -> repository.create(
                        order(),
                        key("key-000000000002", "fingerprint-a"),
                        NOW.plusSeconds(2)))
                .isInstanceOfSatisfying(OrderServiceException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo("ORD_IDEMPOTENCY_CAPACITY_EXCEEDED"));

        CreateOrderResult afterExpiry = repository.create(
                order(),
                key,
                NOW.plusSeconds(61));
        assertThat(afterExpiry.replayed()).isFalse();
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void concurrentDuplicateCommitsExactlyOneOrder() throws Exception {
        InMemoryOrderRepository repository = repository(false, 100, Duration.ofHours(1));
        IdempotencyRequest key = key("concurrent-key-0001", "same-fingerprint");
        List<Callable<CreateOrderResult>> tasks = new ArrayList<>();
        for (int index = 0; index < 24; index++) {
            tasks.add(() -> repository.create(order(), key, NOW));
        }

        try (var executor = Executors.newFixedThreadPool(8)) {
            List<CreateOrderResult> results = executor.invokeAll(tasks).stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                    })
                    .toList();
            assertThat(results).extracting(result -> result.order().id()).containsOnly(
                    results.getFirst().order().id());
            assertThat(results.stream().filter(result -> !result.replayed())).hasSize(1);
        }
        assertThat(repository.count()).isEqualTo(1);
    }

    private static InMemoryOrderRepository repository(
            boolean seed,
            int maxRecords,
            Duration retention) {
        var properties = TestProperties.create(
                seed,
                true,
                List.of(
                        "orders-list",
                        "orders-get",
                        "orders-create",
                        "orders-cancel",
                        "admin-orders-list",
                        "admin-orders-update"),
                List.of(),
                maxRecords,
                retention);
        return new InMemoryOrderRepository(
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC),
                new OrderMetrics(new SimpleMeterRegistry(), properties));
    }

    private static IdempotencyRequest key(String key, String fingerprint) {
        return new IdempotencyRequest(
                "orders-create",
                "tenant-demo",
                "sentra-user-omar",
                key,
                fingerprint);
    }

    private static Order order() {
        return new Order(
                UUID.randomUUID(),
                "sentra-user-omar",
                "tenant-demo",
                List.of(new OrderItem("BOOK-JAVA-25", 1)),
                OrderStatus.CREATED,
                PaymentStatus.PENDING,
                FulfillmentStatus.UNFULFILLED,
                1,
                NOW,
                NOW);
    }
}
