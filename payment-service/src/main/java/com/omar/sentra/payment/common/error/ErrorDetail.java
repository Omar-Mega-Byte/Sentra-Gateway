package com.omar.sentra.payment.common.error;

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
                accessMode = Schema.AccessMode.READ_ONLY, example = "amount") String field,
        @Schema(minLength = 1, maxLength = 64, pattern = "^[a-z][a-z0-9_]*$",
                requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY,
                example = "format") String code,
        @Schema(minLength = 1, maxLength = 256, requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY,
                example = "Amount must be a decimal string with exactly two fractional digits.") String message) {}
