package com.omar.sentra.payment.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Internal payment record containing trusted owner identity and domain state.
 *
 * @param id service-owned payment identifier
 * @param clientId trusted partner client owner
 * @param merchantReference partner-supplied reference
 * @param amount fixed-scale payment amount
 * @param currency uppercase 3-letter currency
 * @param status deterministic payment status
 * @param createdAt creation time
 * @param updatedAt last update time
 */
public record Payment(
        UUID id,
        String clientId,
        String merchantReference,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public Payment withStatus(PaymentStatus newStatus, Instant updatedAt) {
        return new Payment(id, clientId, merchantReference, amount, currency, newStatus, createdAt, updatedAt);
    }
}
