package com.omar.sentra.payment.payment;

import java.math.BigDecimal;

/**
 * Canonical create-payment request after strict validation.
 *
 * @param merchantReference trimmed merchant reference
 * @param amount fixed-scale amount
 * @param currency uppercase currency code
 * @param description optional description
 * @param fingerprint idempotency request fingerprint
 */
public record ValidatedCreatePayment(
        String merchantReference,
        BigDecimal amount,
        String currency,
        String description,
        String fingerprint) {}
