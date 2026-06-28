package com.omar.sentra.order.order;

import static com.omar.sentra.order.common.error.ServiceErrors.idempotencyCapacityExceeded;
import static com.omar.sentra.order.common.error.ServiceErrors.idempotencyConflict;
import static com.omar.sentra.order.common.error.ServiceErrors.versionConflict;

import com.omar.sentra.order.config.OrderServiceProperties;
import com.omar.sentra.order.observability.OrderMetrics;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Synchronized deterministic in-memory repository.
 *
 * <p>Order insertion and idempotency-record insertion share one monitor so
 * concurrent duplicate creates commit exactly one order.</p>
 */
public class InMemoryOrderRepository implements OrderRepository {
    private static final Comparator<Order> ORDERING =
            Comparator.comparing(Order::createdAt)
                    .reversed()
                    .thenComparing(Order::id, Comparator.reverseOrder());

    private final Object monitor = new Object();
    private final boolean seedEnabled;
    private final boolean idempotencyEnabled;
    private final Duration retention;
    private final int maxRecords;
    private final Clock clock;
    private final OrderMetrics metrics;
    private final Map<UUID, Order> orders = new HashMap<>();
    private final Map<IdempotencyScope, IdempotencyRecord> idempotencyRecords = new HashMap<>();
    private volatile boolean ready;

    public InMemoryOrderRepository(
            OrderServiceProperties properties,
            Clock clock,
            OrderMetrics metrics) {
        seedEnabled = properties.repository().seedEnabled();
        idempotencyEnabled = properties.idempotency().enabled();
        retention = properties.idempotency().retention();
        maxRecords = properties.idempotency().maxRecords();
        this.clock = clock;
        this.metrics = metrics;
        reset();
    }

    @Override
    public OrderPage<Order> findOwned(
            String tenantId,
            String subject,
            OrderStatus status,
            int page,
            int size) {
        synchronized (monitor) {
            List<Order> matches = orders.values().stream()
                    .filter(order -> Objects.equals(order.tenantId(), tenantId))
                    .filter(order -> order.ownerSubject().equals(subject))
                    .filter(order -> status == null || order.status() == status)
                    .sorted(ORDERING)
                    .toList();
            metrics.repository("find-owned", "success");
            return page(matches, page, size);
        }
    }

    @Override
    public Optional<Order> findOwnedById(UUID id, String tenantId, String subject) {
        synchronized (monitor) {
            Order order = orders.get(id);
            boolean owned = order != null
                    && Objects.equals(order.tenantId(), tenantId)
                    && order.ownerSubject().equals(subject);
            metrics.repository("find-owned-by-id", owned ? "success" : "not_found");
            return owned ? Optional.of(order) : Optional.empty();
        }
    }

    @Override
    public OrderPage<Order> findAdmin(
            OrderStatus status,
            String tenantId,
            String subject,
            int page,
            int size) {
        synchronized (monitor) {
            List<Order> matches = orders.values().stream()
                    .filter(order -> status == null || order.status() == status)
                    .filter(order -> tenantId == null || Objects.equals(order.tenantId(), tenantId))
                    .filter(order -> subject == null || order.ownerSubject().equals(subject))
                    .sorted(ORDERING)
                    .toList();
            metrics.repository("find-admin", "success");
            return page(matches, page, size);
        }
    }

    @Override
    public CreateOrderResult create(
            Order candidate,
            IdempotencyRequest idempotency,
            Instant now) {
        synchronized (monitor) {
            cleanupExpiredLocked(now);
            if (idempotency != null) {
                if (!idempotencyEnabled) {
                    throw idempotencyCapacityExceeded();
                }
                IdempotencyScope scope = new IdempotencyScope(
                        idempotency.routeId(),
                        idempotency.tenantPartition(),
                        idempotency.subject(),
                        idempotency.key());
                IdempotencyRecord existing = idempotencyRecords.get(scope);
                if (existing != null) {
                    if (!existing.fingerprint().equals(idempotency.fingerprint())) {
                        metrics.idempotency("conflict");
                        throw idempotencyConflict();
                    }
                    metrics.idempotency("replayed");
                    metrics.repository("create", "replayed");
                    return new CreateOrderResult(orders.get(existing.orderId()), true, true);
                }
                if (idempotencyRecords.size() >= maxRecords) {
                    metrics.idempotency("capacity");
                    throw idempotencyCapacityExceeded();
                }
                insert(candidate);
                idempotencyRecords.put(
                        scope,
                        new IdempotencyRecord(
                                idempotency.fingerprint(),
                                candidate.id(),
                                now.plus(retention)));
                metrics.idempotency("created");
                metrics.repository("create", "created");
                return new CreateOrderResult(candidate, false, true);
            }
            insert(candidate);
            metrics.repository("create", "created");
            return new CreateOrderResult(candidate, false, false);
        }
    }

    @Override
    public Optional<Order> cancelOwned(
            UUID id,
            String tenantId,
            String subject,
            long expectedVersion,
            Instant now) {
        synchronized (monitor) {
            Order current = orders.get(id);
            if (current == null
                    || !Objects.equals(current.tenantId(), tenantId)
                    || !current.ownerSubject().equals(subject)) {
                return Optional.empty();
            }
            if (current.version() != expectedVersion) {
                throw versionConflict();
            }
            Order updated = new Order(
                    current.id(),
                    current.ownerSubject(),
                    current.tenantId(),
                    current.items(),
                    OrderStatus.CANCELLED,
                    current.paymentStatus(),
                    FulfillmentStatus.CANCELLED,
                    current.version() + 1,
                    current.createdAt(),
                    now);
            orders.put(id, updated);
            return Optional.of(updated);
        }
    }

    @Override
    public Optional<Order> updateAdmin(
            UUID id,
            long expectedVersion,
            OrderStatus orderStatus,
            PaymentStatus paymentStatus,
            FulfillmentStatus fulfillmentStatus,
            Instant now) {
        synchronized (monitor) {
            Order current = orders.get(id);
            if (current == null) {
                return Optional.empty();
            }
            if (current.version() != expectedVersion) {
                throw versionConflict();
            }
            Order updated = new Order(
                    current.id(),
                    current.ownerSubject(),
                    current.tenantId(),
                    current.items(),
                    orderStatus == null ? current.status() : orderStatus,
                    paymentStatus == null ? current.paymentStatus() : paymentStatus,
                    fulfillmentStatus == null ? current.fulfillmentStatus() : fulfillmentStatus,
                    current.version() + 1,
                    current.createdAt(),
                    now);
            orders.put(id, updated);
            return Optional.of(updated);
        }
    }

    @Override
    public void cleanupExpired(Instant ignored) {
        synchronized (monitor) {
            cleanupExpiredLocked(ignored);
        }
    }

    /**
     * Scheduled no-argument cleanup entry point.
     */
    @Scheduled(fixedDelayString = "${sentra.order.idempotency.cleanup-interval:5m}")
    public void cleanupExpired() {
        cleanupExpired(Instant.now(clock));
    }

    @Override
    public void reset() {
        synchronized (monitor) {
            ready = false;
            orders.clear();
            idempotencyRecords.clear();
            if (seedEnabled) {
                OrderSeedData.orders().forEach(this::insert);
            }
            ready = true;
        }
    }

    @Override
    public boolean ready() {
        return ready;
    }

    @Override
    public long count() {
        synchronized (monitor) {
            return orders.size();
        }
    }

    @Override
    public String mode() {
        return "memory";
    }

    private void cleanupExpiredLocked(Instant now) {
        idempotencyRecords.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private void insert(Order order) {
        if (orders.putIfAbsent(order.id(), order) != null) {
            throw new IllegalStateException("Duplicate order identity.");
        }
    }

    private static OrderPage<Order> page(List<Order> values, int page, int size) {
        long total = values.size();
        int totalPages = total == 0 ? 0 : (int) ((total + size - 1) / size);
        long offset = (long) page * size;
        if (offset >= total) {
            return new OrderPage<>(page, size, total, totalPages, List.of());
        }
        int from = Math.toIntExact(offset);
        int to = Math.min(from + size, values.size());
        return new OrderPage<>(page, size, total, totalPages, new ArrayList<>(values.subList(from, to)));
    }

    private record IdempotencyScope(
            String routeId,
            String tenantPartition,
            String subject,
            String key) {}

    private record IdempotencyRecord(
            String fingerprint,
            UUID orderId,
            Instant expiresAt) {}
}
