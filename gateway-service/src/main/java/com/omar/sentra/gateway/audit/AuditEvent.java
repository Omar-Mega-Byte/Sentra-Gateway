package com.omar.sentra.gateway.audit;

import java.time.Instant;
import java.util.UUID;

/**
 * Redacted security and routing decision.
 */
public record AuditEvent(
        UUID id,
        Instant eventTime,
        String requestId,
        String traceId,
        String eventType,
        String decision,
        String reasonCode,
        String routeId,
        String method,
        String path,
        String actorType,
        String subjectRef,
        String sourceIp,
        int status,
        long latencyMs,
        String instanceId,
        String environment) {
}
