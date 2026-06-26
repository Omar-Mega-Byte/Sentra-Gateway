package com.omar.sentra.user.common.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Stable JSON error contract returned by the user service.
 *
 * @param timestamp server time
 * @param requestId approved correlation identifier
 * @param status HTTP status
 * @param code stable service error code
 * @param message sanitized client-facing message
 * @param path internal request path without query parameters
 * @param routeId trusted gateway route identifier when available
 * @param details bounded field-level details
 */
@Schema(description = "Stable user-service error response")
public record ApiError(
        @Schema(format = "date-time", example = "2026-06-15T00:00:00Z") Instant timestamp,
        @Schema(example = "8e3a95b8-6674-423e-83e6-0df84c2d66d0") String requestId,
        @Schema(example = "409") int status,
        @Schema(example = "USR_VERSION_CONFLICT") String code,
        @Schema(example = "The profile was changed by another request.") String message,
        @Schema(example = "/internal/v1/users/me") String path,
        @Schema(nullable = true, example = "user-profile-update") String routeId,
        List<ErrorDetail> details) {

    public ApiError {
        details = List.copyOf(details);
    }
}
