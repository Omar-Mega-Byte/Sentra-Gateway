package com.omar.sentra.gateway.security.apikey;

import java.time.Instant;
import java.util.UUID;

/**
 * One-time API-key issuance result.
 */
public record IssuedApiKey(
        UUID keyId,
        String apiKey,
        String prefix,
        Instant createdAt,
        Instant expiresAt,
        String warning) {
}
