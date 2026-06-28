package com.omar.sentra.order.order;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable order aggregate owned by one trusted subject and tenant partition.
 *
 * @param id service-generated order identifier
 * @param ownerSubject trusted owner subject
 * @param tenantId trusted nullable tenant
 * @param items ordered immutable items
 * @param status service-controlled status
 * @param paymentStatus basic payment state
 * @param fulfillmentStatus basic fulfillment state
 * @param version optimistic version
 * @param createdAt creation time
 * @param updatedAt last update time
 */
public record Order(
        UUID id,
        String ownerSubject,
        String tenantId,
        List<OrderItem> items,
        OrderStatus status,
        PaymentStatus paymentStatus,
        FulfillmentStatus fulfillmentStatus,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    public Order {
        items = List.copyOf(items);
    }
}
