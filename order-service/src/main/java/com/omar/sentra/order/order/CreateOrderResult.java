package com.omar.sentra.order.order;

/**
 * Result of an atomic create or idempotent replay.
 *
 * @param order committed order
 * @param replayed whether a live record supplied the original result
 * @param keyed whether the request used an idempotency key
 */
public record CreateOrderResult(Order order, boolean replayed, boolean keyed) {}
