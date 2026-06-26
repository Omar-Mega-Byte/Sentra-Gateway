package com.sentra.notification.common.error;

import java.util.List;

/**
 * Redacted service exception carrying a stable error code, public status, and
 * bounded validation details.
 */
public class ServiceException extends RuntimeException {

    private final ErrorCode code;
    private final int status;
    private final List<String> details;

    private ServiceException(ErrorCode code, int status, String message, List<String> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = List.copyOf(details);
    }

    /** @param code stable error code @return service exception */
    public static ServiceException of(ErrorCode code) {
        return new ServiceException(code, code.status(), code.message(), List.of());
    }

    /** @param code stable error code @param status HTTP status code @return service exception */
    public static ServiceException of(ErrorCode code, int status) {
        return new ServiceException(code, status, code.message(), List.of());
    }

    /** @param code stable error code @param details public validation details @return service exception */
    public static ServiceException withDetails(ErrorCode code, List<String> details) {
        return new ServiceException(code, code.status(), code.message(), details);
    }

    /** @return stable error code */
    public ErrorCode code() {
        return code;
    }

    /** @return HTTP status */
    public int status() {
        return status;
    }

    /** @return bounded public details */
    public List<String> details() {
        return details;
    }
}
