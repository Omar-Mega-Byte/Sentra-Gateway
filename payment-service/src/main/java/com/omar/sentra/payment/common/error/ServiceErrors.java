package com.omar.sentra.payment.common.error;

import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Creates documented payment-service failures without exposing submitted data.
 */
public final class ServiceErrors {
    private ServiceErrors() {}

    public static PaymentServiceException requestInvalid(List<ErrorDetail> details) {
        return new PaymentServiceException(
                HttpStatus.BAD_REQUEST,
                "PAY_REQUEST_INVALID",
                "The request is invalid.",
                details);
    }

    public static PaymentServiceException idempotencyKeyRequired() {
        return new PaymentServiceException(
                HttpStatus.BAD_REQUEST,
                "PAY_IDEMPOTENCY_KEY_REQUIRED",
                "A valid Idempotency-Key header is required for this payment route.",
                List.of(new ErrorDetail(
                        "Idempotency-Key",
                        "required",
                        "Use one visible ASCII value within the configured length.")));
    }

    public static PaymentServiceException trustedContextRequired() {
        return new PaymentServiceException(
                HttpStatus.UNAUTHORIZED,
                "PAY_TRUSTED_CONTEXT_REQUIRED",
                "Trusted gateway context is required.");
    }

    public static PaymentServiceException actorNotAllowed() {
        return new PaymentServiceException(
                HttpStatus.FORBIDDEN,
                "PAY_ACTOR_NOT_ALLOWED",
                "The trusted actor is not allowed for this payment operation.");
    }

    public static PaymentServiceException routeNotAllowed() {
        return new PaymentServiceException(
                HttpStatus.FORBIDDEN,
                "PAY_ROUTE_NOT_ALLOWED",
                "The trusted route is not allowed for this payment operation.");
    }

    public static PaymentServiceException scopeRequired() {
        return new PaymentServiceException(
                HttpStatus.FORBIDDEN,
                "PAY_SCOPE_REQUIRED",
                "The required payment permission is missing.");
    }

    public static PaymentServiceException signatureContextRequired() {
        return new PaymentServiceException(
                HttpStatus.FORBIDDEN,
                "PAY_SIGNATURE_CONTEXT_REQUIRED",
                "A verified request signature is required for this payment route.");
    }

    public static PaymentServiceException paymentNotFound() {
        return new PaymentServiceException(
                HttpStatus.NOT_FOUND,
                "PAY_PAYMENT_NOT_FOUND",
                "The requested payment was not found.");
    }

    public static PaymentServiceException refundNotFound() {
        return new PaymentServiceException(
                HttpStatus.NOT_FOUND,
                "PAY_REFUND_NOT_FOUND",
                "The requested refund was not found.");
    }

    public static PaymentServiceException idempotencyConflict() {
        return new PaymentServiceException(
                HttpStatus.CONFLICT,
                "PAY_IDEMPOTENCY_CONFLICT",
                "The idempotency key was already used for a different request.",
                List.of(new ErrorDetail(
                        "Idempotency-Key",
                        "conflict",
                        "Use a new key for a different payment request.")));
    }

    public static PaymentServiceException referenceConflict() {
        return new PaymentServiceException(
                HttpStatus.CONFLICT,
                "PAY_REFERENCE_CONFLICT",
                "The merchant reference already exists for this client.");
    }

    public static PaymentServiceException refundNotAllowed() {
        return new PaymentServiceException(
                HttpStatus.CONFLICT,
                "PAY_REFUND_NOT_ALLOWED",
                "The payment cannot be refunded for the requested amount.");
    }

    public static PaymentServiceException bodyTooLarge() {
        return new PaymentServiceException(
                HttpStatus.CONTENT_TOO_LARGE,
                "PAY_BODY_TOO_LARGE",
                "The request body exceeds the configured limit.");
    }

    public static PaymentServiceException mediaTypeUnsupported() {
        return new PaymentServiceException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "PAY_MEDIA_TYPE_UNSUPPORTED",
                "Content-Type application/json is required.");
    }

    public static PaymentServiceException idempotencyCapacityExceeded() {
        return new PaymentServiceException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "PAY_IDEMPOTENCY_CAPACITY_EXCEEDED",
                "The idempotency record capacity is temporarily exhausted.");
    }

    public static PaymentServiceException dependencyUnavailable() {
        return new PaymentServiceException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "PAY_DEPENDENCY_UNAVAILABLE",
                "The payment repository is temporarily unavailable.");
    }
}
