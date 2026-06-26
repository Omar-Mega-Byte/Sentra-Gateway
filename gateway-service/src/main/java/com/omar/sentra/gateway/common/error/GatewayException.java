package com.omar.sentra.gateway.common.error;

import java.util.List;

/**
 * Safe exception carrying a stable external error code.
 */
public class GatewayException extends RuntimeException {
    private final ErrorCode errorCode;
    private final List<ErrorDetail> details;

    public GatewayException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage(), List.of());
    }

    public GatewayException(ErrorCode errorCode, String message) {
        this(errorCode, message, List.of());
    }

    public GatewayException(ErrorCode errorCode, String message, List<ErrorDetail> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = List.copyOf(details);
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public List<ErrorDetail> details() {
        return details;
    }
}
