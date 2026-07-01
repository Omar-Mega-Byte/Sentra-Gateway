package com.omar.sentra.payment.payment;

import static com.omar.sentra.payment.common.error.ServiceErrors.paymentNotFound;
import static com.omar.sentra.payment.common.error.ServiceErrors.referenceConflict;
import static com.omar.sentra.payment.common.error.ServiceErrors.refundNotAllowed;

import com.omar.sentra.payment.config.PaymentServiceProperties;
import com.omar.sentra.payment.observability.PaymentMetrics;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Synchronized deterministic in-memory repository for the baseline service.
 */
@Repository
public class InMemoryPaymentRepository implements PaymentRepository {
    private final PaymentServiceProperties properties;
    private final PaymentMetrics metrics;
    private final Map<UUID, Payment> payments = new HashMap<>();
    private final Map<UUID, Refund> refunds = new HashMap<>();
    private boolean ready;

    public InMemoryPaymentRepository(PaymentServiceProperties properties, PaymentMetrics metrics) {
        this.properties = properties;
        this.metrics = metrics;
        reset();
    }

    @Override
    public synchronized Optional<Payment> findPaymentForClient(UUID id, String clientId) {
        Optional<Payment> result = Optional.ofNullable(payments.get(id))
                .filter(payment -> payment.clientId().equals(clientId));
        metrics.repository("payment-read", result.isPresent() ? "found" : "not-found");
        return result;
    }

    @Override
    public synchronized Payment createPayment(Payment payment) {
        if (referenceExists(payment.clientId(), payment.merchantReference(), true)) {
            metrics.repository("payment-create", "reference-conflict");
            throw referenceConflict();
        }
        payments.put(payment.id(), payment);
        metrics.repository("payment-create", "created");
        return payment;
    }

    @Override
    public synchronized Refund createRefund(
            UUID refundId,
            String clientId,
            UUID paymentId,
            String merchantReference,
            BigDecimal amount,
            Instant createdAt) {
        Payment payment = payments.get(paymentId);
        if (payment == null || !payment.clientId().equals(clientId)) {
            metrics.repository("refund-create", "payment-not-found");
            throw paymentNotFound();
        }
        if (merchantReference != null && referenceExists(clientId, merchantReference, false)) {
            metrics.repository("refund-create", "reference-conflict");
            throw referenceConflict();
        }
        if (payment.status() != PaymentStatus.CAPTURED || refundableAmount(paymentId).compareTo(amount) < 0) {
            metrics.repository("refund-create", "not-allowed");
            throw refundNotAllowed();
        }

        Refund refund = new Refund(
                refundId,
                paymentId,
                clientId,
                merchantReference,
                amount,
                payment.currency(),
                RefundStatus.ACCEPTED,
                createdAt);
        refunds.put(refund.id(), refund);
        if (refundableAmount(paymentId).compareTo(BigDecimal.ZERO) == 0) {
            payments.put(paymentId, payment.withStatus(PaymentStatus.REFUNDED, createdAt));
        }
        metrics.repository("refund-create", "created");
        return refund;
    }

    @Override
    public synchronized void reset() {
        payments.clear();
        refunds.clear();
        if (properties.repository().seedEnabled()) {
            PaymentSeedData.payments().forEach(payment -> payments.put(payment.id(), payment));
            PaymentSeedData.refunds().forEach(refund -> refunds.put(refund.id(), refund));
        }
        ready = true;
    }

    @Override
    public synchronized long paymentCount() {
        return payments.size();
    }

    @Override
    public synchronized long refundCount() {
        return refunds.size();
    }

    @Override
    public synchronized boolean ready() {
        return ready;
    }

    @Override
    public String mode() {
        return "memory";
    }

    private boolean referenceExists(String clientId, String reference, boolean paymentReference) {
        if (reference == null) {
            return false;
        }
        if (paymentReference) {
            return payments.values().stream()
                    .anyMatch(payment -> payment.clientId().equals(clientId)
                            && payment.merchantReference().equals(reference));
        }
        return refunds.values().stream()
                .anyMatch(refund -> refund.clientId().equals(clientId)
                        && reference.equals(refund.merchantReference()));
    }

    private BigDecimal refundableAmount(UUID paymentId) {
        Payment payment = payments.get(paymentId);
        BigDecimal refunded = refunds.values().stream()
                .filter(refund -> refund.paymentId().equals(paymentId) && refund.status() == RefundStatus.ACCEPTED)
                .map(Refund::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return payment.amount().subtract(refunded);
    }
}
