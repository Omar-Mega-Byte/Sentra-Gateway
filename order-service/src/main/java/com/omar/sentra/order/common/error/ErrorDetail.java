package com.omar.sentra.order.common.error;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Sanitized field-level error information.
 *
 * @param field affected field or header
 * @param code stable detail code
 * @param message client-safe guidance
 */
@Schema(
        description = "Sanitized field-level validation or conflict detail",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record ErrorDetail(
        @Schema(minLength = 1, maxLength = 64, requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY, example = "items[0].quantity") String field,
        @Schema(minLength = 1, maxLength = 64, pattern = "^[a-z][a-z0-9_]*$",
                requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY,
                example = "range") String code,
        @Schema(minLength = 1, maxLength = 256, requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY,
                example = "Quantity must be between 1 and 100.") String message) {}
