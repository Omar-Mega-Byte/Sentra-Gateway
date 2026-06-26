package com.omar.sentra.user.common.error;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Sanitized field-level error information.
 *
 * @param field affected field
 * @param code stable detail code
 * @param message client-safe guidance
 */
@Schema(description = "Sanitized field-level validation or conflict detail")
public record ErrorDetail(
        @Schema(example = "version") String field,
        @Schema(example = "conflict") String code,
        @Schema(example = "Refresh the profile and retry the update.") String message) {}
