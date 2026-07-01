package com.omar.sentra.payment.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Canonical create-refund request after strict validation.
 *
 * @param paymentId canonical payment identifier
 * @param merchantReference optional trimmed merchant reference
 * @param amount fixed-scale amount
 * @param fingerprint idempotency request fingerprint
 */
public record ValidatedCreateRefund(
        UUID paymentId,
        String merchantReference,
        BigDecimal amount,
        String fingerprint) {}
