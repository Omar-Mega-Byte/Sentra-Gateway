package com.omar.sentra.payment.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.omar.sentra.payment.TestProperties;
import com.omar.sentra.payment.common.error.PaymentServiceException;
import com.omar.sentra.payment.observability.PaymentMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryPaymentRepositoryTest {
    private static final Instant NOW = Instant.parse("2026-06-16T12:00:00Z");

    @Test
    void readsAreClientScopedAndForeignRecordsAreHidden() {
        InMemoryPaymentRepository repository = repository(true);

        assertThat(repository.findPaymentForClient(PaymentSeedData.ACME_CAPTURED_PAYMENT_ID, PaymentSeedData.ACME_CLIENT))
                .isPresent();
        assertThat(repository.findPaymentForClient(PaymentSeedData.OTHER_CAPTURED_PAYMENT_ID, PaymentSeedData.ACME_CLIENT))
                .isEmpty();
    }

    @Test
    void paymentAndRefundReferencesAreUniquePerClient() {
        InMemoryPaymentRepository repository = repository(true);
        Payment duplicate = new Payment(
                UUID.randomUUID(),
                PaymentSeedData.ACME_CLIENT,
                "acme-order-1001",
                new BigDecimal("1.00"),
                "USD",
                PaymentStatus.AUTHORIZED,
                NOW,
                NOW);

        assertThatThrownBy(() -> repository.createPayment(duplicate))
                .isInstanceOfSatisfying(PaymentServiceException.class,
                        exception -> assertThat(exception.code()).isEqualTo("PAY_REFERENCE_CONFLICT"));

        assertThatThrownBy(() -> repository.createRefund(
                        UUID.randomUUID(),
                        PaymentSeedData.ACME_CLIENT,
                        PaymentSeedData.ACME_CAPTURED_PAYMENT_ID,
                        "acme-refund-1001",
                        new BigDecimal("1.00"),
                        NOW))
                .isInstanceOfSatisfying(PaymentServiceException.class,
                        exception -> assertThat(exception.code()).isEqualTo("PAY_REFERENCE_CONFLICT"));
    }

    @Test
    void refundRulesProtectOwnershipStateAndRemainingAmount() {
        InMemoryPaymentRepository repository = repository(true);

        assertThatThrownBy(() -> repository.createRefund(
                        UUID.randomUUID(),
                        PaymentSeedData.ACME_CLIENT,
                        PaymentSeedData.OTHER_CAPTURED_PAYMENT_ID,
                        "acme-refund-foreign",
                        new BigDecimal("1.00"),
                        NOW))
                .isInstanceOfSatisfying(PaymentServiceException.class,
                        exception -> assertThat(exception.code()).isEqualTo("PAY_PAYMENT_NOT_FOUND"));

        assertThatThrownBy(() -> repository.createRefund(
                        UUID.randomUUID(),
                        PaymentSeedData.ACME_CLIENT,
                        PaymentSeedData.ACME_DECLINED_PAYMENT_ID,
                        "acme-refund-declined",
                        new BigDecimal("1.00"),
                        NOW))
                .isInstanceOfSatisfying(PaymentServiceException.class,
                        exception -> assertThat(exception.code()).isEqualTo("PAY_REFUND_NOT_ALLOWED"));

        assertThatThrownBy(() -> repository.createRefund(
                        UUID.randomUUID(),
                        PaymentSeedData.ACME_CLIENT,
                        PaymentSeedData.ACME_CAPTURED_PAYMENT_ID,
                        "acme-refund-too-large",
                        new BigDecimal("101.00"),
                        NOW))
                .isInstanceOfSatisfying(PaymentServiceException.class,
                        exception -> assertThat(exception.code()).isEqualTo("PAY_REFUND_NOT_ALLOWED"));
    }

    private static InMemoryPaymentRepository repository(boolean seed) {
        var properties = TestProperties.create(
                seed,
                TestProperties.defaults().gateway().allowedRouteIds(),
                TestProperties.defaults().gateway().allowedPeers(),
                10000,
                java.time.Duration.ofHours(24));
        return new InMemoryPaymentRepository(
                properties,
                new PaymentMetrics(new SimpleMeterRegistry(), properties));
    }
}
