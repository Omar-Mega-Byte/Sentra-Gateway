package com.omar.sentra.payment.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Deterministic in-memory records used by local and test profiles.
 */
public final class PaymentSeedData {
    public static final String ACME_CLIENT = "partner-acme";
    public static final String OTHER_CLIENT = "partner-other";
    public static final UUID ACME_CAPTURED_PAYMENT_ID = UUID.fromString("40000000-0000-4000-8000-000000000001");
    public static final UUID ACME_DECLINED_PAYMENT_ID = UUID.fromString("40000000-0000-4000-8000-000000000002");
    public static final UUID OTHER_CAPTURED_PAYMENT_ID = UUID.fromString("50000000-0000-4000-8000-000000000001");
    public static final UUID ACME_ACCEPTED_REFUND_ID = UUID.fromString("60000000-0000-4000-8000-000000000001");

    private static final Instant PAYMENT_TIME = Instant.parse("2026-06-01T10:00:00Z");
    private static final Instant REFUND_TIME = Instant.parse("2026-06-02T10:00:00Z");

    private PaymentSeedData() {}

    public static List<Payment> payments() {
        return List.of(
                new Payment(
                        ACME_CAPTURED_PAYMENT_ID,
                        ACME_CLIENT,
                        "acme-order-1001",
                        new BigDecimal("125.50"),
                        "USD",
                        PaymentStatus.CAPTURED,
                        PAYMENT_TIME,
                        PAYMENT_TIME),
                new Payment(
                        ACME_DECLINED_PAYMENT_ID,
                        ACME_CLIENT,
                        "acme-order-declined-1001",
                        new BigDecimal("25.00"),
                        "USD",
                        PaymentStatus.DECLINED,
                        PAYMENT_TIME,
                        PAYMENT_TIME),
                new Payment(
                        OTHER_CAPTURED_PAYMENT_ID,
                        OTHER_CLIENT,
                        "other-order-1001",
                        new BigDecimal("64.00"),
                        "USD",
                        PaymentStatus.CAPTURED,
                        PAYMENT_TIME,
                        PAYMENT_TIME));
    }

    public static List<Refund> refunds() {
        return List.of(new Refund(
                ACME_ACCEPTED_REFUND_ID,
                ACME_CAPTURED_PAYMENT_ID,
                ACME_CLIENT,
                "acme-refund-1001",
                new BigDecimal("25.00"),
                "USD",
                RefundStatus.ACCEPTED,
                REFUND_TIME));
    }
}
