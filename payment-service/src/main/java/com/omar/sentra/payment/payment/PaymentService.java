package com.omar.sentra.payment.payment;

import static com.omar.sentra.payment.common.error.ServiceErrors.idempotencyKeyRequired;
import static com.omar.sentra.payment.common.error.ServiceErrors.paymentNotFound;

import com.omar.sentra.payment.common.request.TrustedRequestContext;
import com.omar.sentra.payment.config.PaymentServiceProperties;
import com.omar.sentra.payment.idempotency.IdempotencyStore;
import com.omar.sentra.payment.idempotency.IdempotentResult;
import com.omar.sentra.payment.idempotency.StoredMutation;
import com.omar.sentra.payment.observability.PaymentMetrics;
import com.omar.sentra.payment.web.CreatePaymentRequest;
import com.omar.sentra.payment.web.CreateRefundRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Coordinates trusted payment and refund operations, validation, repository
 * mutation, and idempotency.
 */
@Service
public class PaymentService {
    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final PaymentRepository repository;
    private final PaymentValidator validator;
    private final IdempotencyStore idempotencyStore;
    private final Clock clock;
    private final PaymentServiceProperties properties;
    private final PaymentMetrics metrics;

    public PaymentService(
            PaymentRepository repository,
            PaymentValidator validator,
            IdempotencyStore idempotencyStore,
            Clock clock,
            PaymentServiceProperties properties,
            PaymentMetrics metrics) {
        this.repository = repository;
        this.validator = validator;
        this.idempotencyStore = idempotencyStore;
        this.clock = clock;
        this.properties = properties;
        this.metrics = metrics;
    }

    /**
     * Reads one owned payment or returns the safe not-found failure for unknown
     * and foreign-client identifiers.
     *
     * @param context trusted gateway context
     * @param id payment ID
     * @return owned payment
     */
    public Payment getPayment(TrustedRequestContext context, UUID id) {
        return repository.findPaymentForClient(id, context.clientId()).orElseThrow(() -> paymentNotFound());
    }

    /**
     * Creates or replays a deterministic payment mutation.
     *
     * @param context trusted gateway context
     * @param request submitted body
     * @param servletRequest servlet request for idempotency headers
     * @return original or replayed idempotent result
     */
    public IdempotentResult<Payment> createPayment(
            TrustedRequestContext context,
            CreatePaymentRequest request,
            HttpServletRequest servletRequest) {
        ValidatedCreatePayment validated = validator.validate(request);
        String key = idempotencyKey(servletRequest);
        return idempotencyStore.execute(
                "payment-create",
                context.routeId(),
                context.clientId(),
                key,
                validated.fingerprint(),
                () -> {
                    Instant now = Instant.now(clock);
                    PaymentStatus status = properties.repository().mockDeclineReferences()
                            .contains(validated.merchantReference())
                            ? PaymentStatus.DECLINED
                            : PaymentStatus.AUTHORIZED;
                    Payment payment = new Payment(
                            UUID.randomUUID(),
                            context.clientId(),
                            validated.merchantReference(),
                            validated.amount(),
                            validated.currency(),
                            status,
                            now,
                            now);
                    Payment created = repository.createPayment(payment);
                    metrics.mutation("payment-create", status.name().toLowerCase(java.util.Locale.ROOT));
                    return new StoredMutation<>(created, "/internal/v1/payments/" + created.id());
                });
    }

    /**
     * Creates or replays a deterministic refund mutation.
     *
     * @param context trusted gateway context
     * @param request submitted body
     * @param servletRequest servlet request for idempotency headers
     * @return original or replayed idempotent result
     */
    public IdempotentResult<Refund> createRefund(
            TrustedRequestContext context,
            CreateRefundRequest request,
            HttpServletRequest servletRequest) {
        ValidatedCreateRefund validated = validator.validate(request);
        String key = idempotencyKey(servletRequest);
        return idempotencyStore.execute(
                "refund-create",
                context.routeId(),
                context.clientId(),
                key,
                validated.fingerprint(),
                () -> {
                    Refund created = repository.createRefund(
                            UUID.randomUUID(),
                            context.clientId(),
                            validated.paymentId(),
                            validated.merchantReference(),
                            validated.amount(),
                            Instant.now(clock));
                    metrics.mutation("refund-create", created.status().name().toLowerCase(java.util.Locale.ROOT));
                    return new StoredMutation<>(created, "/internal/v1/refunds/" + created.id());
                });
    }

    private String idempotencyKey(HttpServletRequest request) {
        List<String> values = Collections.list(request.getHeaders(IDEMPOTENCY_KEY));
        if (values.size() != 1) {
            throw idempotencyKeyRequired();
        }
        String value = values.getFirst();
        if (value == null
                || value.isBlank()
                || value.length() > properties.idempotency().keyMaxLength()
                || value.chars().anyMatch(character -> character < 0x21 || character > 0x7E)) {
            throw idempotencyKeyRequired();
        }
        return value;
    }
}
