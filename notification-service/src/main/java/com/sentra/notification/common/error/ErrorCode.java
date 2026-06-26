package com.sentra.notification.common.error;

/**
 * Stable notification-service error codes and their documented default HTTP
 * statuses.
 */
public enum ErrorCode {
    /** Generic request validation failure. */
    NTF_REQUEST_INVALID(400, "The request is invalid."),
    /** Required trusted gateway context is absent or malformed. */
    NTF_TRUSTED_CONTEXT_REQUIRED(401, "Trusted gateway context is required."),
    /** Trusted actor type is not allowed for this service. */
    NTF_ACTOR_NOT_ALLOWED(403, "Actor type is not allowed."),
    /** Trusted route identity is not allowed for this operation. */
    NTF_ROUTE_NOT_ALLOWED(403, "Route identity is not allowed."),
    /** Required user scope is absent. */
    NTF_SCOPE_REQUIRED(403, "Required notification scope is missing."),
    /** Required admin role is absent. */
    NTF_ROLE_REQUIRED(403, "Required notification admin role is missing."),
    /** Fault controls are disabled for this environment or scenario. */
    NTF_FAULT_CONTROL_DISABLED(403, "Fault controls are disabled for the current profile."),
    /** Preference optimistic version mismatch. */
    NTF_VERSION_CONFLICT(409, "Notification preferences were changed by another request."),
    /** Request body is larger than the configured limit. */
    NTF_BODY_TOO_LARGE(413, "Request body exceeds the configured limit."),
    /** Request media type is unsupported. */
    NTF_MEDIA_TYPE_UNSUPPORTED(415, "Content-Type must be application/json."),
    /** Controlled local/test failure scenario. */
    NTF_TEST_FAILURE(500, "Configured notification test failure."),
    /** Repository or supporting subsystem is unavailable. */
    NTF_DEPENDENCY_UNAVAILABLE(503, "Notification dependency is unavailable."),
    /** Unexpected service failure. */
    NTF_INTERNAL_ERROR(500, "Notification service encountered an internal error.");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    /** @return HTTP status code */
    public int status() {
        return status;
    }

    /** @return public redacted message */
    public String message() {
        return message;
    }
}
