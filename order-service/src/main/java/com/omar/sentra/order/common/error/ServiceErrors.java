package com.omar.sentra.order.common.error;

import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Creates documented order-service failures without exposing submitted data.
 */
public final class ServiceErrors {
    private ServiceErrors() {}

    public static OrderServiceException requestInvalid(List<ErrorDetail> details) {
        return new OrderServiceException(
                HttpStatus.BAD_REQUEST,
                "ORD_REQUEST_INVALID",
                "The request is invalid.",
                details);
    }

    public static OrderServiceException idempotencyKeyInvalid() {
        return new OrderServiceException(
                HttpStatus.BAD_REQUEST,
                "ORD_IDEMPOTENCY_KEY_INVALID",
                "The idempotency key is invalid.",
                List.of(new ErrorDetail(
                        "Idempotency-Key",
                        "format",
                        "Use one visible ASCII value within the configured length.")));
    }

    public static OrderServiceException trustedContextRequired() {
        return new OrderServiceException(
                HttpStatus.UNAUTHORIZED,
                "ORD_TRUSTED_CONTEXT_REQUIRED",
                "Trusted gateway context is required.");
    }

    public static OrderServiceException actorNotAllowed() {
        return new OrderServiceException(
                HttpStatus.FORBIDDEN,
                "ORD_ACTOR_NOT_ALLOWED",
                "The trusted actor is not allowed for this operation.");
    }

    public static OrderServiceException routeNotAllowed() {
        return new OrderServiceException(
                HttpStatus.FORBIDDEN,
                "ORD_ROUTE_NOT_ALLOWED",
                "The trusted route is not allowed for this operation.");
    }

    public static OrderServiceException scopeRequired() {
        return new OrderServiceException(
                HttpStatus.FORBIDDEN,
                "ORD_SCOPE_REQUIRED",
                "The required order permission is missing.");
    }

    public static OrderServiceException roleRequired() {
        return new OrderServiceException(
                HttpStatus.FORBIDDEN,
                "ORD_ROLE_REQUIRED",
                "The required order administrator role is missing.");
    }

    public static OrderServiceException orderNotFound() {
        return new OrderServiceException(
                HttpStatus.NOT_FOUND,
                "ORD_ORDER_NOT_FOUND",
                "The requested order was not found.");
    }

    public static OrderServiceException idempotencyConflict() {
        return new OrderServiceException(
                HttpStatus.CONFLICT,
                "ORD_IDEMPOTENCY_CONFLICT",
                "The idempotency key was already used for a different request.",
                List.of(new ErrorDetail(
                        "Idempotency-Key",
                        "conflict",
                        "Use a new key for a different order request.")));
    }

    public static OrderServiceException versionConflict() {
        return new OrderServiceException(
                HttpStatus.CONFLICT,
                "ORD_VERSION_CONFLICT",
                "The order was changed by another request.",
                List.of(new ErrorDetail(
                        "version",
                        "conflict",
                        "Refresh the order and retry the update.")));
    }

    public static OrderServiceException stateConflict(String message) {
        return new OrderServiceException(
                HttpStatus.CONFLICT,
                "ORD_STATE_CONFLICT",
                message);
    }

    public static OrderServiceException bodyTooLarge() {
        return new OrderServiceException(
                HttpStatus.CONTENT_TOO_LARGE,
                "ORD_BODY_TOO_LARGE",
                "The request body exceeds the configured limit.");
    }

    public static OrderServiceException mediaTypeUnsupported() {
        return new OrderServiceException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "ORD_MEDIA_TYPE_UNSUPPORTED",
                "Content-Type application/json is required.");
    }

    public static OrderServiceException idempotencyCapacityExceeded() {
        return new OrderServiceException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "ORD_IDEMPOTENCY_CAPACITY_EXCEEDED",
                "The idempotency record capacity is temporarily exhausted.");
    }

    public static OrderServiceException dependencyUnavailable() {
        return new OrderServiceException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "ORD_DEPENDENCY_UNAVAILABLE",
                "The order repository is temporarily unavailable.");
    }
}
