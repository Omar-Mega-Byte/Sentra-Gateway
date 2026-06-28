package com.omar.sentra.order.common.error;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Stable JSON error contract returned by the order service.
 *
 * @param timestamp server UTC time
 * @param requestId approved correlation identifier
 * @param status HTTP status
 * @param code stable service error code
 * @param message sanitized client-facing message
 * @param path internal request path without query parameters
 * @param routeId trusted gateway route identifier when available
 * @param details bounded field-level details
 */
@Schema(
        description = "Stable order-service error response",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record ApiError(
        @Schema(format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY, example = "2026-06-15T12:00:00Z") Instant timestamp,
        @Schema(minLength = 1, maxLength = 128, requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY,
                example = "8e3a95b8-6674-423e-83e6-0df84c2d66d0") String requestId,
        @Schema(minimum = "400", maximum = "599", requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY, example = "409") int status,
        @Schema(
                allowableValues = {
                    "ORD_REQUEST_INVALID",
                    "ORD_IDEMPOTENCY_KEY_INVALID",
                    "ORD_TRUSTED_CONTEXT_REQUIRED",
                    "ORD_ACTOR_NOT_ALLOWED",
                    "ORD_ROUTE_NOT_ALLOWED",
                    "ORD_SCOPE_REQUIRED",
                    "ORD_ROLE_REQUIRED",
                    "ORD_ORDER_NOT_FOUND",
                    "ORD_IDEMPOTENCY_CONFLICT",
                    "ORD_VERSION_CONFLICT",
                    "ORD_STATE_CONFLICT",
                    "ORD_BODY_TOO_LARGE",
                    "ORD_MEDIA_TYPE_UNSUPPORTED",
                    "ORD_IDEMPOTENCY_CAPACITY_EXCEEDED",
                    "ORD_DEPENDENCY_UNAVAILABLE",
                    "ORD_INTERNAL_ERROR"
                },
                requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY,
                example = "ORD_IDEMPOTENCY_CONFLICT")
                String code,
        @Schema(minLength = 1, requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY,
                example = "The idempotency key was already used for a different request.") String message,
        @Schema(pattern = "^/[^?#]*$", requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY, example = "/internal/v1/orders") String path,
        @Schema(nullable = true, pattern = "^[a-z0-9][a-z0-9._-]{0,127}$",
                requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY,
                example = "orders-create") String routeId,
        @ArraySchema(minItems = 0, maxItems = 20,
                schema = @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY))
                List<ErrorDetail> details) {

    public ApiError {
        details = List.copyOf(details);
    }
}
