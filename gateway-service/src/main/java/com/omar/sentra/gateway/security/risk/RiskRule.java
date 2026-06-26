package com.omar.sentra.gateway.security.risk;

import java.time.Instant;

/**
 * Explainable bounded risk rule.
 */
public record RiskRule(
        String id,
        String signal,
        int thresholdValue,
        int weight,
        String action,
        String routeId,
        boolean enabled,
        long version,
        Instant createdAt,
        Instant updatedAt) {
}
