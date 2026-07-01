package com.omar.sentra.payment.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Client-scoped repository contract for deterministic payment and refund state.
 */
public interface PaymentRepository {
    Optional<Payment> findPaymentForClient(UUID id, String clientId);

    Payment createPayment(Payment payment);

    Refund createRefund(
            UUID refundId,
            String clientId,
            UUID paymentId,
            String merchantReference,
            BigDecimal amount,
            Instant createdAt);

    void reset();

    long paymentCount();

    long refundCount();

    boolean ready();

    String mode();
}
