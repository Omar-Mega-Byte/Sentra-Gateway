package com.sentra.notification.common.error;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Stable redacted API error response shared by all documented endpoints.
 *
 * @param timestamp UTC error timestamp
 * @param requestId request correlation identifier returned as {@code X-Request-Id}
 * @param status HTTP status code
 * @param code stable {@code NTF_*} code
 * @param message public redacted error message
 * @param path request path without sensitive values
 * @param routeId trusted route ID when available
 * @param details bounded validation details without request bodies or trusted-header values
 */
@Schema(description = "Stable notification-service error response.")
public record ApiError(
        @Schema(type = "string", format = "date-time", description = "UTC error timestamp.") Instant timestamp,
        @Schema(example = "8e3a95b8-6674-423e-83e6-0df84c2d66d0") String requestId,
        @Schema(example = "409") int status,
        @Schema(example = "NTF_VERSION_CONFLICT") String code,
        @Schema(example = "Notification preferences were changed by another request.") String message,
        @Schema(example = "/internal/v1/preferences") String path,
        @Schema(example = "notification-preferences-update") String routeId,
        @ArraySchema(schema = @Schema(example = "version: must match the current preference version")) List<String> details) {
}
