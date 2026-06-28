package com.omar.sentra.order.order;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Order persistence contract centered on ownership-safe queries and atomic
 * idempotent creation.
 */
public interface OrderRepository {

    OrderPage<Order> findOwned(
            String tenantId,
            String subject,
            OrderStatus status,
            int page,
            int size);

    Optional<Order> findOwnedById(UUID id, String tenantId, String subject);

    OrderPage<Order> findAdmin(
            OrderStatus status,
            String tenantId,
            String subject,
            int page,
            int size);

    CreateOrderResult create(
            Order candidate,
            IdempotencyRequest idempotency,
            Instant now);

    Optional<Order> cancelOwned(
            UUID id,
            String tenantId,
            String subject,
            long expectedVersion,
            Instant now);

    Optional<Order> updateAdmin(
            UUID id,
            long expectedVersion,
            OrderStatus orderStatus,
            PaymentStatus paymentStatus,
            FulfillmentStatus fulfillmentStatus,
            Instant now);

    void cleanupExpired(Instant now);

    void reset();

    boolean ready();

    long count();

    default String mode() {
        return "memory";
    }
}
