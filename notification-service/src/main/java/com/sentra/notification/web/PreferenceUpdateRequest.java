package com.sentra.notification.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Strict request body for optimistic notification preference updates.
 *
 * @param emailEnabled requested email preference
 * @param smsEnabled requested SMS preference
 * @param pushEnabled requested push preference
 * @param webhookEnabled requested webhook preference
 * @param version expected current version
 */
@Schema(description = "Preference update request. All fields are required and unknown JSON fields are rejected.")
public record PreferenceUpdateRequest(
        @NotNull @Schema(example = "true", requiredMode = Schema.RequiredMode.REQUIRED) Boolean emailEnabled,
        @NotNull @Schema(example = "false", requiredMode = Schema.RequiredMode.REQUIRED) Boolean smsEnabled,
        @NotNull @Schema(example = "true", requiredMode = Schema.RequiredMode.REQUIRED) Boolean pushEnabled,
        @NotNull @Schema(example = "false", requiredMode = Schema.RequiredMode.REQUIRED) Boolean webhookEnabled,
        @NotNull @PositiveOrZero @Schema(example = "2", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") Integer version) {
}
