package com.omar.sentra.gateway.security.apikey;

import java.util.List;
import java.util.UUID;

/**
 * Validated partner identity.
 */
public record ApiKeyPrincipal(
        UUID clientId,
        UUID keyId,
        String clientName,
        String tenantId,
        List<String> scopes,
        List<String> allowedRoutes,
        String presentedKey) {
}
