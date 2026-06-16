package com.omar.sentra.gateway.ratelimit;

/**
 * Token-bucket result.
 */
public record RateLimitDecision(boolean allowed, long remaining, long retryAfterSeconds) {
}
