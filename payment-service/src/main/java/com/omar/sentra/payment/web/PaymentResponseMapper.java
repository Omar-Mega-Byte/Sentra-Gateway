package com.omar.sentra.payment.web;

import com.omar.sentra.payment.payment.Payment;
import com.omar.sentra.payment.payment.Refund;
import org.springframework.stereotype.Component;

/**
 * Converts internal payment records into partner-safe DTOs.
 */
@Component
public class PaymentResponseMapper {

    public PaymentResponse payment(Payment payment) {
        return new PaymentResponse(
                payment.id(),
                payment.merchantReference(),
                payment.amount().toPlainString(),
                payment.currency(),
                payment.status(),
                payment.createdAt(),
                payment.updatedAt());
    }

    public RefundResponse refund(Refund refund) {
        return new RefundResponse(
                refund.id(),
                refund.paymentId(),
                refund.merchantReference(),
                refund.amount().toPlainString(),
                refund.currency(),
                refund.status(),
                refund.createdAt());
    }
}
