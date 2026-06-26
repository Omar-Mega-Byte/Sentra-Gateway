package com.omar.sentra.user.common.error;

import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Base exception for expected, client-safe service failures.
 */
public class UserServiceException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final List<ErrorDetail> details;

    public UserServiceException(
            HttpStatus status,
            String code,
            String message,
            List<ErrorDetail> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = List.copyOf(details);
    }

    public UserServiceException(HttpStatus status, String code, String message) {
        this(status, code, message, List.of());
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public List<ErrorDetail> details() {
        return details;
    }
}
