package com.omar.sentra.gateway.ratelimit;

import java.time.Instant;

/**
 * Distributed token-bucket policy.
 */
public record RateLimitPolicy(
        String id,
        String subjectType,
        String routeId,
        String method,
        int capacity,
        int refillTokens,
        int refillPeriodSeconds,
        int priority,
        String redisOutageMode,
        boolean responseHeadersEnabled,
        boolean enabled,
        long version,
        Instant createdAt,
        Instant updatedAt) {
}
