package com.omar.sentra.gateway.audit;

import java.time.Instant;
import java.util.UUID;

/**
 * Administrative mutation evidence.
 */
public record AdminAction(
        UUID id,
        Instant eventTime,
        String actor,
        String action,
        String targetType,
        String targetId,
        String result,
        String changeSummary,
        String requestId) {
}
