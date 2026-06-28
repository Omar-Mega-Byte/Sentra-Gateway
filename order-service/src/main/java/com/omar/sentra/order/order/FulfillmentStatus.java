package com.omar.sentra.order.order;

/**
 * Basic fulfillment state tracked by the order service demo.
 */
public enum FulfillmentStatus {
    UNFULFILLED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
