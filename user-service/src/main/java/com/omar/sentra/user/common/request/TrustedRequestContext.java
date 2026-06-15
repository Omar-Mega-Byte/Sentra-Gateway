package com.omar.sentra.user.common.request;

import java.util.List;

/**
 * Validated identity and routing context supplied by the gateway.
 *
 * @param requestId approved request identifier
 * @param subject trusted user subject
 * @param actorType trusted actor type
 * @param scopes decoded scopes
 * @param routeId approved route identifier
 */
public record TrustedRequestContext(
        String requestId,
        String subject,
        String actorType,
        List<String> scopes,
        String routeId) {

    public TrustedRequestContext {
        scopes = List.copyOf(scopes);
    }
}
