package com.sentra.notification.fault;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Admin-triggered deterministic downstream test scenarios.
 */
@Schema(description = "Admin test-notification scenario.", allowableValues = {"SUCCESS", "DELAY", "FAILURE", "MALFORMED", "DISCONNECT"})
public enum TestScenario {
    /** Return the normal accepted test response. */
    SUCCESS,
    /** Apply a bounded artificial delay before returning success. */
    DELAY,
    /** Return a bounded configured failure status. */
    FAILURE,
    /** Return intentionally malformed JSON in local/test. */
    MALFORMED,
    /** Simulate a downstream disconnect as a committed partial response. */
    DISCONNECT
}
