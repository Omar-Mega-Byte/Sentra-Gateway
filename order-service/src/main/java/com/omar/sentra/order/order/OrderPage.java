package com.omar.sentra.order.order;

import java.util.List;

/**
 * Immutable repository page.
 *
 * @param page zero-based page
 * @param size requested page size
 * @param totalElements exact matching count
 * @param totalPages exact page count
 * @param items ordered page items
 * @param <T> item type
 */
public record OrderPage<T>(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<T> items) {

    public OrderPage {
        items = List.copyOf(items);
    }
}
