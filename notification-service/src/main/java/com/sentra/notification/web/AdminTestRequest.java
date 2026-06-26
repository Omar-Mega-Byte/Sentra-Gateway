package com.sentra.notification.web;

import com.sentra.notification.fault.TestScenario;
import com.sentra.notification.notification.Channel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Strict admin test request used to exercise gateway resilience behavior.
 *
 * @param scenario deterministic test scenario
 * @param channel notification channel to include in the test request
 * @param recipientReference bounded non-sensitive recipient fixture reference
 * @param message bounded smoke-test message
 */
@Schema(description = "Admin-only deterministic notification test request.")
public record AdminTestRequest(
        @NotNull @Schema(example = "SUCCESS", requiredMode = Schema.RequiredMode.REQUIRED) TestScenario scenario,
        @NotNull @Schema(example = "EMAIL", requiredMode = Schema.RequiredMode.REQUIRED) Channel channel,
        @NotBlank @Size(min = 1, max = 128) @Pattern(regexp = "[\\p{Graph} ]+")
        @Schema(example = "test-recipient", minLength = 1, maxLength = 128, requiredMode = Schema.RequiredMode.REQUIRED)
        String recipientReference,
        @NotBlank @Size(min = 1, max = 1000)
        @Schema(example = "Gateway resilience smoke test", minLength = 1, maxLength = 1000, requiredMode = Schema.RequiredMode.REQUIRED)
        String message) {
}
