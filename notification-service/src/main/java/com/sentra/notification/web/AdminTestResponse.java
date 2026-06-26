package com.sentra.notification.web;

import com.sentra.notification.fault.TestScenario;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Public admin test response for successful deterministic scenarios.
 *
 * @param scenario scenario that was accepted
 * @param accepted whether the scenario was accepted by the service
 * @param result stable result label
 * @param createdAt UTC creation instant
 */
@Schema(description = "Admin test-notification response for successful scenarios.")
public record AdminTestResponse(
        @Schema(example = "SUCCESS") TestScenario scenario,
        @Schema(example = "true") boolean accepted,
        @Schema(example = "TEST_ACCEPTED") String result,
        @Schema(type = "string", format = "date-time", description = "UTC test response creation timestamp.") Instant createdAt) {
}
