package com.omar.sentra.gateway.security.apikey;

import java.time.Instant;
import java.util.UUID;

/**
 * Registered machine-to-machine client.
 */
public record ApiClient(
        UUID id,
        String name,
        String owner,
        String tenantId,
        ClientStatus status,
        long version,
        Instant createdAt,
        Instant updatedAt) {
}
