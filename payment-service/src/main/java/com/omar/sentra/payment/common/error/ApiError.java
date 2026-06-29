package com.omar.sentra.payment.common.error;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Stable JSON error contract returned by the payment service.
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
        description = "Stable payment-service error response",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record ApiError(
        @Schema(format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY, example = "2026-06-16T12:00:00Z") Instant timestamp,
        @Schema(minLength = 1, maxLength = 128, requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY,
                example = "8e3a95b8-6674-423e-83e6-0df84c2d66d0") String requestId,
        @Schema(minimum = "400", maximum = "599", requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY, example = "403") int status,
        @Schema(
                allowableValues = {
                    "PAY_REQUEST_INVALID",
                    "PAY_IDEMPOTENCY_KEY_REQUIRED",
                    "PAY_TRUSTED_CONTEXT_REQUIRED",
                    "PAY_ACTOR_NOT_ALLOWED",
                    "PAY_ROUTE_NOT_ALLOWED",
                    "PAY_SCOPE_REQUIRED",
                    "PAY_SIGNATURE_CONTEXT_REQUIRED",
                    "PAY_PAYMENT_NOT_FOUND",
                    "PAY_REFUND_NOT_FOUND",
                    "PAY_IDEMPOTENCY_CONFLICT",
                    "PAY_REFERENCE_CONFLICT",
                    "PAY_REFUND_NOT_ALLOWED",
                    "PAY_BODY_TOO_LARGE",
                    "PAY_MEDIA_TYPE_UNSUPPORTED",
                    "PAY_IDEMPOTENCY_CAPACITY_EXCEEDED",
                    "PAY_DEPENDENCY_UNAVAILABLE",
                    "PAY_INTERNAL_ERROR"
                },
                requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY,
                example = "PAY_SIGNATURE_CONTEXT_REQUIRED")
                String code,
        @Schema(minLength = 1, requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY,
                example = "A verified request signature is required for this payment route.") String message,
        @Schema(pattern = "^/[^?#]*$", requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY, example = "/internal/v1/payments") String path,
        @Schema(nullable = true, pattern = "^[a-z0-9][a-z0-9._-]{0,127}$",
                requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY,
                example = "partner-payment-create") String routeId,
        @ArraySchema(minItems = 0, maxItems = 20,
                schema = @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY))
                List<ErrorDetail> details) {

    public ApiError {
        details = List.copyOf(details);
    }
}
