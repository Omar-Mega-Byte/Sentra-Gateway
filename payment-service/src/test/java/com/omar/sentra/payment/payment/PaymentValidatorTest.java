package com.omar.sentra.payment.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.omar.sentra.payment.TestProperties;
import com.omar.sentra.payment.common.error.PaymentServiceException;
import com.omar.sentra.payment.web.CreatePaymentRequest;
import com.omar.sentra.payment.web.CreateRefundRequest;
import org.junit.jupiter.api.Test;

class PaymentValidatorTest {
    private final PaymentValidator validator = new PaymentValidator(TestProperties.defaults());

    @Test
    void canonicalizesAndFingerprintsPaymentFieldsDeterministically() {
        ValidatedCreatePayment first = validator.validate(new CreatePaymentRequest(
                "  acme-order-1002  ",
                "125.50",
                "USD",
                "Security gateway lab payment"));
        ValidatedCreatePayment second = validator.validate(new CreatePaymentRequest(
                "acme-order-1002",
                "125.50",
                "USD",
                "Security gateway lab payment"));

        assertThat(first.merchantReference()).isEqualTo("acme-order-1002");
        assertThat(first.amount().toPlainString()).isEqualTo("125.50");
        assertThat(first.fingerprint()).isEqualTo(second.fingerprint()).hasSize(64);
    }

    @Test
    void rejectsInvalidMoneyCurrencyReferenceAndRefundId() {
        assertThatThrownBy(() -> validator.validate(new CreatePaymentRequest(
                        "contains space",
                        "1.234",
                        "usd",
                        "description")))
                .isInstanceOfSatisfying(PaymentServiceException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("PAY_REQUEST_INVALID");
                    assertThat(exception.details()).extracting(detail -> detail.field())
                            .contains("merchantReference", "amount", "currency");
                });

        assertThatThrownBy(() -> validator.validate(new CreateRefundRequest(
                        "not-a-uuid",
                        "acme-refund-1002",
                        "0.00")))
                .isInstanceOfSatisfying(PaymentServiceException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("PAY_REQUEST_INVALID");
                    assertThat(exception.details()).extracting(detail -> detail.field())
                            .contains("paymentId", "amount");
                });
    }
}
