package com.omar.sentra.gateway.security.apikey;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Durable API-key metadata and verifier.
 */
public record ApiKeyRecord(
        UUID id,
        UUID clientId,
        String prefix,
        String verifier,
        String pepperVersion,
        List<String> scopes,
        List<String> allowedRoutes,
        KeyStatus status,
        Instant validFrom,
        Instant expiresAt,
        UUID rotatedFrom,
        Instant lastUsedAt,
        Instant createdAt) {
}
