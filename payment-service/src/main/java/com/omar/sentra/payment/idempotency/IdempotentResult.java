package com.omar.sentra.payment.idempotency;

/**
 * Result of executing or replaying an idempotent mutation.
 *
 * @param body response body
 * @param location response Location header value
 * @param replayed whether this response came from an existing record
 * @param <T> response body type
 */
public record IdempotentResult<T>(T body, String location, boolean replayed) {}
