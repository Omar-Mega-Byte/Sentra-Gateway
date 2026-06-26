package com.omar.sentra.gateway.common.error;

import org.springframework.http.HttpStatus;

/**
 * Stable client-facing gateway error codes.
 */
public enum ErrorCode {
    GW_REQUEST_INVALID(HttpStatus.BAD_REQUEST, "The request is invalid."),
    GW_AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "Authentication is required."),
    GW_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "The access token is invalid."),
    GW_API_KEY_INVALID(HttpStatus.UNAUTHORIZED, "The API key is invalid."),
    GW_SIGNATURE_INVALID(HttpStatus.UNAUTHORIZED, "The request signature is invalid."),
    GW_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "The request is not permitted."),
    GW_REPLAY_DETECTED(HttpStatus.FORBIDDEN, "The request was already used."),
    GW_IP_DENIED(HttpStatus.FORBIDDEN, "The source address is not permitted."),
    GW_RISK_DENIED(HttpStatus.FORBIDDEN, "The request was denied by security policy."),
    GW_ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "No route matched the request."),
    GW_RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource does not exist."),
    GW_POLICY_CONFLICT(HttpStatus.CONFLICT, "The resource was changed by another request."),
    GW_BODY_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "The request body is too large."),
    GW_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "The request rate limit was exceeded."),
    GW_DEPENDENCY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "A required dependency is unavailable."),
    GW_DOWNSTREAM_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "The downstream service is unavailable."),
    GW_DOWNSTREAM_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "The downstream service timed out."),
    GW_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "The gateway could not process the request.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
