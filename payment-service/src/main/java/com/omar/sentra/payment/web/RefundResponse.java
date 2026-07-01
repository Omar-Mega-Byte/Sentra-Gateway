package com.omar.sentra.payment.web;

import com.omar.sentra.payment.payment.RefundStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Partner-safe refund representation without client, key, signature, nonce, or
 * idempotency internals.
 *
 * @param id refund ID
 * @param paymentId payment ID
 * @param merchantReference optional partner refund reference
 * @param amount amount string
 * @param currency uppercase currency
 * @param status refund status
 * @param createdAt creation timestamp
 */
@Schema(
        description = "Partner-safe refund response",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record RefundResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid",
                example = "60000000-0000-4000-8000-000000000001") UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid",
                example = "40000000-0000-4000-8000-000000000001") UUID paymentId,
        @Schema(nullable = true, example = "acme-refund-1001") String merchantReference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string", example = "25.00") String amount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "USD") String currency,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"ACCEPTED", "DECLINED"},
                example = "ACCEPTED") RefundStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time",
                example = "2026-06-02T10:00:00Z") Instant createdAt) {}
