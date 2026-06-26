package com.omar.sentra.gateway.common.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Stable JSON error response.
 *
 * @param timestamp response time
 * @param requestId correlation ID
 * @param status HTTP status
 * @param code stable gateway code
 * @param message safe message
 * @param path request path
 * @param routeId selected route, if known
 * @param details validation details
 */
@Schema(name = "GatewayError")
public record ApiError(
        Instant timestamp,
        String requestId,
        int status,
        String code,
        String message,
        String path,
        String routeId,
        List<ErrorDetail> details) {
}
