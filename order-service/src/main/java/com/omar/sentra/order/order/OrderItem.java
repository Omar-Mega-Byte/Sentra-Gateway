package com.omar.sentra.order.order;

/**
 * Immutable validated order item.
 *
 * @param sku opaque product identifier
 * @param quantity requested quantity
 */
public record OrderItem(String sku, int quantity) {}
