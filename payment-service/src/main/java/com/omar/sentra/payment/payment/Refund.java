package com.omar.sentra.payment.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Internal refund record containing trusted owner identity and domain state.
 *
 * @param id service-owned refund identifier
 * @param paymentId payment being refunded
 * @param clientId trusted partner client owner
 * @param merchantReference optional partner refund reference
 * @param amount fixed-scale refund amount
 * @param currency inherited payment currency
 * @param status deterministic refund status
 * @param createdAt creation time
 */
public record Refund(
        UUID id,
        UUID paymentId,
        String clientId,
        String merchantReference,
        BigDecimal amount,
        String currency,
        RefundStatus status,
        Instant createdAt) {}
