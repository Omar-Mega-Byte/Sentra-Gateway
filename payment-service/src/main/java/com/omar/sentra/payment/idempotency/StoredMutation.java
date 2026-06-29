package com.omar.sentra.payment.idempotency;

/**
 * Mutation result persisted behind an idempotency key.
 *
 * @param body response body
 * @param location response Location header value
 * @param <T> response body type
 */
public record StoredMutation<T>(T body, String location) {}
