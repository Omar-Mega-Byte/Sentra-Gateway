package com.omar.sentra.order.common.request;

import java.util.List;

/**
 * Validated identity, tenant, permission, and route context from the gateway.
 *
 * @param requestId approved request identifier
 * @param subject trusted user subject
 * @param tenantId trusted nullable tenant
 * @param actorType trusted actor type
 * @param scopes decoded scopes
 * @param roles decoded roles
 * @param routeId approved route identifier
 */
public record TrustedRequestContext(
        String requestId,
        String subject,
        String tenantId,
        String actorType,
        List<String> scopes,
        List<String> roles,
        String routeId) {

    public TrustedRequestContext {
        scopes = List.copyOf(scopes);
        roles = List.copyOf(roles);
    }
}
