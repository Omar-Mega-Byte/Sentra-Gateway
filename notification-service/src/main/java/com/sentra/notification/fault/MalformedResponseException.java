package com.sentra.notification.fault;

/**
 * Internal signal for an intentionally malformed local/test response.
 */
public class MalformedResponseException extends RuntimeException {
    private final int status;

    /** @param status response status to commit */
    public MalformedResponseException(int status) {
        super("Malformed response simulation");
        this.status = status;
    }

    /** @return response status */
    public int status() {
        return status;
    }
}
