package com.omar.sentra.order.order;

import java.util.List;

/**
 * Canonical validated create request.
 *
 * @param items ordered normalized items
 * @param fingerprint SHA-256 of canonical fields
 */
public record ValidatedCreateOrder(List<OrderItem> items, String fingerprint) {
    public ValidatedCreateOrder {
        items = List.copyOf(items);
    }
}
