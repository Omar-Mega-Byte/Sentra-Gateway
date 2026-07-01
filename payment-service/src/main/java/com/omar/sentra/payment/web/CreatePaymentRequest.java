package com.omar.sentra.payment.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Partner-submitted payment create body.
 *
 * @param merchantReference partner reference unique per trusted client
 * @param amount positive two-decimal amount string
 * @param currency uppercase 3-letter currency code
 * @param description optional client description
 */
@Schema(
        description = "Create-payment request. Owner, security, idempotency, and server-controlled fields are rejected.",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record CreatePaymentRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 128,
                pattern = "^[!-~]{1,128}$", example = "acme-order-1002")
                String merchantReference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string",
                pattern = "^\\d+\\.\\d{2}$", example = "125.50")
                String amount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 3, maxLength = 3,
                pattern = "^[A-Z]{3}$", example = "USD")
                String currency,
        @Schema(nullable = true, maxLength = 255, example = "Security gateway lab payment")
                String description) {}
