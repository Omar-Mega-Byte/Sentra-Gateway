package com.sentra.notification.common.request;

import java.util.Set;

/**
 * Validated trusted context forwarded by the gateway for an internal request.
 *
 * @param requestId bounded request correlation identifier
 * @param subject trusted JWT subject
 * @param actorType trusted actor type
 * @param tenantId trusted tenant identifier, when supplied
 * @param scopes trusted user scopes
 * @param roles trusted user roles
 * @param routeId exact internal route identifier
 */
public record TrustedContext(String requestId, String subject, String actorType, String tenantId, Set<String> scopes, Set<String> roles, String routeId) {
}
