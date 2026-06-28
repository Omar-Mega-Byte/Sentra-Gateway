package com.omar.sentra.order.order;

import static com.omar.sentra.order.common.error.ServiceErrors.idempotencyCapacityExceeded;
import static com.omar.sentra.order.common.error.ServiceErrors.idempotencyConflict;
import static com.omar.sentra.order.common.error.ServiceErrors.versionConflict;

import com.omar.sentra.order.config.OrderServiceProperties;
import com.omar.sentra.order.observability.OrderMetrics;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * PostgreSQL order repository with transactional durable idempotency.
 */
public class JdbcOrderRepository implements OrderRepository {
    private final boolean seedEnabled;
    private final boolean idempotencyEnabled;
    private final Duration retention;
    private final int maxRecords;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final OrderMetrics metrics;

    public JdbcOrderRepository(
            OrderServiceProperties properties,
            JdbcTemplate jdbc,
            TransactionTemplate transactions,
            OrderMetrics metrics) {
        seedEnabled = properties.repository().seedEnabled();
        idempotencyEnabled = properties.idempotency().enabled();
        retention = properties.idempotency().retention();
        maxRecords = properties.idempotency().maxRecords();
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.metrics = metrics;
        if (seedEnabled) {
            seedMissing();
        }
    }

    @Override
    public OrderPage<Order> findOwned(
            String tenantId,
            String subject,
            OrderStatus status,
            int page,
            int size) {
        String where = " where owner_subject = ? and tenant_partition = ?"
                + " and (? is null or status = ?)";
        String partition = partition(tenantId);
        Object statusValue = status == null ? null : status.name();
        long total = jdbc.queryForObject(
                "select count(*) from orders" + where,
                Long.class,
                subject,
                partition,
                statusValue,
                statusValue);
        List<Order> orders = jdbc.query(
                "select * from orders" + where
                        + " order by created_at desc, id desc limit ? offset ?",
                this::mapOrder,
                subject,
                partition,
                statusValue,
                statusValue,
                size,
                (long) page * size);
        metrics.repository("find-owned", "success");
        return page(page, size, total, orders);
    }

    @Override
    public Optional<Order> findOwnedById(UUID id, String tenantId, String subject) {
        Optional<Order> result = jdbc.query(
                "select * from orders where id = ? and owner_subject = ? and tenant_partition = ?",
                this::mapOrder,
                id,
                subject,
                partition(tenantId)).stream().findFirst();
        metrics.repository("find-owned-by-id", result.isPresent() ? "success" : "not_found");
        return result;
    }

    @Override
    public OrderPage<Order> findAdmin(
            OrderStatus status,
            String tenantId,
            String subject,
            int page,
            int size) {
        Object statusValue = status == null ? null : status.name();
        String tenantPartition = tenantId == null ? null : partition(tenantId);
        String where = " where (? is null or status = ?)"
                + " and (? is null or tenant_partition = ?)"
                + " and (? is null or owner_subject = ?)";
        Object[] parameters = {
            statusValue, statusValue,
            tenantPartition, tenantPartition,
            subject, subject
        };
        long total = jdbc.queryForObject(
                "select count(*) from orders" + where,
                Long.class,
                parameters);
        List<Order> orders = jdbc.query(
                "select * from orders" + where
                        + " order by created_at desc, id desc limit ? offset ?",
                this::mapOrder,
                statusValue,
                statusValue,
                tenantPartition,
                tenantPartition,
                subject,
                subject,
                size,
                (long) page * size);
        metrics.repository("find-admin", "success");
        return page(page, size, total, orders);
    }

    @Override
    public CreateOrderResult create(
            Order candidate,
            IdempotencyRequest idempotency,
            Instant now) {
        return transactions.execute(status -> {
            cleanupExpired(now);
            if (idempotency == null) {
                insertOrder(candidate);
                metrics.repository("create", "created");
                return new CreateOrderResult(candidate, false, false);
            }
            if (!idempotencyEnabled) {
                throw idempotencyCapacityExceeded();
            }
            long liveRecords = jdbc.queryForObject(
                    "select count(*) from order_idempotency where expires_at > ?",
                    Long.class,
                    Timestamp.from(now));
            if (liveRecords >= maxRecords) {
                throw idempotencyCapacityExceeded();
            }

            insertOrder(candidate);
            int inserted = jdbc.update("""
                    insert into order_idempotency (
                        route_id, tenant_partition, owner_subject, idempotency_key,
                        request_fingerprint, order_id, expires_at, created_at
                    ) values (?, ?, ?, ?, ?, ?, ?, ?)
                    on conflict (route_id, tenant_partition, owner_subject, idempotency_key)
                    do nothing
                    """,
                    idempotency.routeId(),
                    idempotency.tenantPartition(),
                    idempotency.subject(),
                    idempotency.key(),
                    idempotency.fingerprint(),
                    candidate.id(),
                    Timestamp.from(now.plus(retention)),
                    Timestamp.from(now));
            if (inserted == 1) {
                metrics.idempotency("created");
                metrics.repository("create", "created");
                return new CreateOrderResult(candidate, false, true);
            }

            deleteOrder(candidate.id());
            IdempotencyRow existing = jdbc.queryForObject("""
                    select request_fingerprint, order_id
                      from order_idempotency
                     where route_id = ? and tenant_partition = ?
                       and owner_subject = ? and idempotency_key = ?
                    """,
                    (result, row) -> new IdempotencyRow(
                            result.getString("request_fingerprint"),
                            result.getObject("order_id", UUID.class)),
                    idempotency.routeId(),
                    idempotency.tenantPartition(),
                    idempotency.subject(),
                    idempotency.key());
            if (existing == null || !existing.fingerprint().equals(idempotency.fingerprint())) {
                metrics.idempotency("conflict");
                throw idempotencyConflict();
            }
            Order original = findById(existing.orderId())
                    .orElseThrow(() -> new IllegalStateException("Idempotency order is missing."));
            metrics.idempotency("replayed");
            metrics.repository("create", "replayed");
            return new CreateOrderResult(original, true, true);
        });
    }

    @Override
    public void cleanupExpired(Instant now) {
        jdbc.update("delete from order_idempotency where expires_at <= ?", Timestamp.from(now));
    }

    @Override
    public Optional<Order> cancelOwned(
            UUID id,
            String tenantId,
            String subject,
            long expectedVersion,
            Instant now) {
        return transactions.execute(status -> {
            Optional<Order> current = jdbc.query(
                    "select * from orders where id = ? and owner_subject = ? and tenant_partition = ? for update",
                    this::mapOrder,
                    id,
                    subject,
                    partition(tenantId)).stream().findFirst();
            if (current.isEmpty()) {
                return Optional.empty();
            }
            if (current.get().version() != expectedVersion) {
                throw versionConflict();
            }
            updateLifecycle(
                    id,
                    expectedVersion,
                    OrderStatus.CANCELLED,
                    current.get().paymentStatus(),
                    FulfillmentStatus.CANCELLED,
                    current.get().version() + 1,
                    now);
            return findById(id);
        });
    }

    @Override
    public Optional<Order> updateAdmin(
            UUID id,
            long expectedVersion,
            OrderStatus orderStatus,
            PaymentStatus paymentStatus,
            FulfillmentStatus fulfillmentStatus,
            Instant now) {
        return transactions.execute(status -> {
            Optional<Order> current = jdbc.query(
                    "select * from orders where id = ? for update",
                    this::mapOrder,
                    id).stream().findFirst();
            if (current.isEmpty()) {
                return Optional.empty();
            }
            if (current.get().version() != expectedVersion) {
                throw versionConflict();
            }
            updateLifecycle(
                    id,
                    expectedVersion,
                    orderStatus == null ? current.get().status() : orderStatus,
                    paymentStatus == null ? current.get().paymentStatus() : paymentStatus,
                    fulfillmentStatus == null ? current.get().fulfillmentStatus() : fulfillmentStatus,
                    current.get().version() + 1,
                    now);
            return findById(id);
        });
    }

    @Override
    public void reset() {
        if (!seedEnabled) {
            throw new UnsupportedOperationException("Reset is available only when seed data is enabled.");
        }
        transactions.executeWithoutResult(status -> {
            jdbc.update("delete from order_idempotency");
            jdbc.update("delete from order_items");
            jdbc.update("delete from orders");
            OrderSeedData.orders().forEach(this::insertOrder);
        });
    }

    @Override
    public boolean ready() {
        try {
            return Boolean.TRUE.equals(jdbc.queryForObject("select true", Boolean.class));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public long count() {
        return jdbc.queryForObject("select count(*) from orders", Long.class);
    }

    @Override
    public String mode() {
        return "postgres";
    }

    private Optional<Order> findById(UUID id) {
        return jdbc.query(
                "select * from orders where id = ?",
                this::mapOrder,
                id).stream().findFirst();
    }

    private Order mapOrder(ResultSet result, int row) throws SQLException {
        UUID id = result.getObject("id", UUID.class);
        List<OrderItem> items = jdbc.query(
                "select sku, quantity from order_items where order_id = ? order by item_index",
                (itemResult, itemRow) -> new OrderItem(
                        itemResult.getString("sku"),
                        itemResult.getInt("quantity")),
                id);
        String tenant = result.getString("tenant_id");
        return new Order(
                id,
                result.getString("owner_subject"),
                tenant,
                items,
                OrderStatus.valueOf(result.getString("status")),
                PaymentStatus.valueOf(result.getString("payment_status")),
                FulfillmentStatus.valueOf(result.getString("fulfillment_status")),
                result.getLong("version"),
                result.getTimestamp("created_at").toInstant(),
                result.getTimestamp("updated_at").toInstant());
    }

    private void insertOrder(Order order) {
        jdbc.update("""
                insert into orders (
                    id, owner_subject, tenant_id, tenant_partition,
                    status, payment_status, fulfillment_status, version, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                order.id(),
                order.ownerSubject(),
                order.tenantId(),
                partition(order.tenantId()),
                order.status().name(),
                order.paymentStatus().name(),
                order.fulfillmentStatus().name(),
                order.version(),
                Timestamp.from(order.createdAt()),
                Timestamp.from(order.updatedAt()));
        for (int index = 0; index < order.items().size(); index++) {
            OrderItem item = order.items().get(index);
            jdbc.update("""
                    insert into order_items (order_id, item_index, sku, quantity)
                    values (?, ?, ?, ?)
                    """,
                    order.id(),
                    index,
                    item.sku(),
                    item.quantity());
        }
    }

    private void deleteOrder(UUID id) {
        jdbc.update("delete from orders where id = ?", id);
    }

    private void updateLifecycle(
            UUID id,
            long expectedVersion,
            OrderStatus orderStatus,
            PaymentStatus paymentStatus,
            FulfillmentStatus fulfillmentStatus,
            long version,
            Instant updatedAt) {
        int changed = jdbc.update("""
                update orders
                   set status = ?, payment_status = ?, fulfillment_status = ?,
                       version = ?, updated_at = ?
                 where id = ? and version = ?
                """,
                orderStatus.name(),
                paymentStatus.name(),
                fulfillmentStatus.name(),
                version,
                Timestamp.from(updatedAt),
                id,
                expectedVersion);
        if (changed != 1) {
            throw versionConflict();
        }
    }

    private void seedMissing() {
        OrderSeedData.orders().forEach(order -> {
            if (findById(order.id()).isEmpty()) {
                insertOrder(order);
            }
        });
    }

    private static String partition(String tenantId) {
        return tenantId == null ? "<no-tenant>" : tenantId;
    }

    private static OrderPage<Order> page(
            int page,
            int size,
            long total,
            List<Order> orders) {
        int totalPages = total == 0 ? 0 : (int) ((total + size - 1) / size);
        return new OrderPage<>(page, size, total, totalPages, new ArrayList<>(orders));
    }

    private record IdempotencyRow(String fingerprint, UUID orderId) {}
}
