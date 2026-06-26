package com.omar.sentra.gateway.security.ip;

import java.time.Instant;

/**
 * Exact-address or CIDR network rule.
 */
public record IpRule(
        String id,
        String network,
        String action,
        String routeId,
        int priority,
        String reason,
        Instant validFrom,
        Instant expiresAt,
        boolean enabled,
        long version,
        Instant createdAt,
        Instant updatedAt) {
}
