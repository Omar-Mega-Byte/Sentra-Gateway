package com.omar.sentra.order.order;

/**
 * Sensitive internal idempotency identity and request fingerprint.
 *
 * @param routeId create route identity
 * @param tenantPartition normalized tenant partition
 * @param subject trusted subject
 * @param key caller key
 * @param fingerprint canonical request SHA-256
 */
public record IdempotencyRequest(
        String routeId,
        String tenantPartition,
        String subject,
        String key,
        String fingerprint) {}
