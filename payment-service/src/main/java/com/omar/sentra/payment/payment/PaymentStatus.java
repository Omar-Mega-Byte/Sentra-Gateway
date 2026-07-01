package com.omar.sentra.payment.payment;

/**
 * Payment states exposed by the deterministic payment service.
 */
public enum PaymentStatus {
    AUTHORIZED,
    CAPTURED,
    DECLINED,
    REFUNDED
}
