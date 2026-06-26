package com.omar.sentra.gateway.routing;

import java.time.Instant;
import java.util.List;

/**
 * Durable dynamic route and policy metadata.
 */
public record GatewayRoute(
        String id,
        RouteCategory category,
        List<String> pathPatterns,
        List<String> methods,
        String targetUri,
        int stripPrefix,
        String rewriteRegex,
        String rewriteReplacement,
        int order,
        boolean enabled,
        List<String> authentication,
        List<String> requiredRoles,
        List<String> requiredScopes,
        boolean signingRequired,
        String rateLimitPolicyId,
        String ipPolicyId,
        String riskPolicyId,
        int connectTimeoutMs,
        int responseTimeoutMs,
        boolean retryEnabled,
        int retryMaxAttempts,
        List<String> retryMethods,
        boolean circuitBreakerEnabled,
        String circuitBreakerName,
        String auditMode,
        long version,
        Instant createdAt,
        Instant updatedAt) {
}
