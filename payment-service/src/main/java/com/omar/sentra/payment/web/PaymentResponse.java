package com.omar.sentra.payment.web;

import com.omar.sentra.payment.payment.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Partner-safe payment representation without client, key, signature, nonce, or
 * idempotency internals.
 *
 * @param id payment ID
 * @param merchantReference partner reference
 * @param amount amount string
 * @param currency uppercase currency
 * @param status payment status
 * @param createdAt creation timestamp
 * @param updatedAt update timestamp
 */
@Schema(
        description = "Partner-safe payment response",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record PaymentResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid",
                example = "40000000-0000-4000-8000-000000000001") UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "acme-order-1001") String merchantReference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string", example = "125.50") String amount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "USD") String currency,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"AUTHORIZED", "CAPTURED", "DECLINED", "REFUNDED"},
                example = "CAPTURED") PaymentStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time",
                example = "2026-06-01T10:00:00Z") Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time",
                example = "2026-06-01T10:00:00Z") Instant updatedAt) {}
