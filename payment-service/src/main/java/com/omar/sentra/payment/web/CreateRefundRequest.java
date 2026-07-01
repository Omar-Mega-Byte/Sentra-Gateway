package com.omar.sentra.payment.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Partner-submitted refund create body.
 *
 * @param paymentId canonical payment UUID owned by the trusted client
 * @param merchantReference optional partner refund reference unique per client
 * @param amount positive two-decimal refund amount string
 */
@Schema(
        description = "Create-refund request. Currency is inherited from the payment and must not be submitted.",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record CreateRefundRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid",
                example = "40000000-0000-4000-8000-000000000001")
                String paymentId,
        @Schema(nullable = true, minLength = 1, maxLength = 128,
                pattern = "^[!-~]{1,128}$", example = "acme-refund-1002")
                String merchantReference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string",
                pattern = "^\\d+\\.\\d{2}$", example = "25.00")
                String amount) {}
