package com.omar.sentra.payment.common.request;

import java.util.List;

/**
 * Validated partner client, key, permission, and route context from the gateway.
 *
 * @param requestId approved request identifier
 * @param actorType trusted actor type
 * @param clientId trusted partner client identity
 * @param keyId trusted API-key identifier
 * @param scopes decoded scopes
 * @param routeId approved route identifier
 */
public record TrustedRequestContext(
        String requestId,
        String actorType,
        String clientId,
        String keyId,
        List<String> scopes,
        String routeId) {

    public TrustedRequestContext {
        scopes = List.copyOf(scopes);
    }
}
