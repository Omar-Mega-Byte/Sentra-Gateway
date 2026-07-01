package com.omar.sentra.payment.payment;

import static com.omar.sentra.payment.common.error.ServiceErrors.requestInvalid;

import com.omar.sentra.payment.common.error.ErrorDetail;
import com.omar.sentra.payment.config.PaymentServiceProperties;
import com.omar.sentra.payment.web.CreatePaymentRequest;
import com.omar.sentra.payment.web.CreateRefundRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Performs strict payment and refund request validation without floating-point
 * money parsing.
 */
@Component
public class PaymentValidator {
    private final PaymentServiceProperties properties;
    private final BigDecimal maximumAmount;

    public PaymentValidator(PaymentServiceProperties properties) {
        this.properties = properties;
        maximumAmount = new BigDecimal(properties.limits().maxAmount());
    }

    /**
     * Validates and canonicalizes a payment create request.
     *
     * @param request submitted body
     * @return canonical payment request
     */
    public ValidatedCreatePayment validate(CreatePaymentRequest request) {
        List<ErrorDetail> details = new ArrayList<>();
        String reference = merchantReference(request == null ? null : request.merchantReference(), true, details);
        BigDecimal amount = amount(request == null ? null : request.amount(), maximumAmount, "amount", details);
        String currency = currency(request == null ? null : request.currency(), details);
        String description = description(request == null ? null : request.description(), details);
        if (!details.isEmpty()) {
            throw requestInvalid(details);
        }
        return new ValidatedCreatePayment(
                reference,
                amount,
                currency,
                description,
                fingerprint("payment", reference, amount.toPlainString(), currency, description == null ? "" : description));
    }

    /**
     * Validates and canonicalizes a refund create request.
     *
     * @param request submitted body
     * @return canonical refund request
     */
    public ValidatedCreateRefund validate(CreateRefundRequest request) {
        List<ErrorDetail> details = new ArrayList<>();
        UUID paymentId = canonicalUuid(request == null ? null : request.paymentId(), "paymentId", details);
        String reference = merchantReference(request == null ? null : request.merchantReference(), false, details);
        BigDecimal amount = amount(request == null ? null : request.amount(), maximumAmount, "amount", details);
        if (!details.isEmpty()) {
            throw requestInvalid(details);
        }
        return new ValidatedCreateRefund(
                paymentId,
                reference,
                amount,
                fingerprint("refund", paymentId.toString(), reference == null ? "" : reference, amount.toPlainString()));
    }

    private String merchantReference(String value, boolean required, List<ErrorDetail> details) {
        if (value == null) {
            if (required) {
                details.add(new ErrorDetail("merchantReference", "required", "Merchant reference is required."));
            }
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            if (required) {
                details.add(new ErrorDetail("merchantReference", "required", "Merchant reference is required."));
            } else {
                details.add(new ErrorDetail("merchantReference", "format", "Merchant reference must not be blank when present."));
            }
            return null;
        }
        if (trimmed.length() > properties.limits().maxMerchantReferenceLength()
                || !trimmed.chars().allMatch(character -> character >= 0x21 && character <= 0x7E)) {
            details.add(new ErrorDetail(
                    "merchantReference",
                    "format",
                    "Merchant reference must be visible ASCII within the configured length."));
        }
        return trimmed;
    }

    private BigDecimal amount(String value, BigDecimal maximum, String field, List<ErrorDetail> details) {
        if (value == null || value.isBlank()) {
            details.add(new ErrorDetail(field, "required", "Amount is required."));
            return BigDecimal.ZERO;
        }
        if (!value.matches("\\d+\\.\\d{2}")) {
            details.add(new ErrorDetail(field, "format", "Amount must be a decimal string with exactly two fractional digits."));
            return BigDecimal.ZERO;
        }
        BigDecimal amount = new BigDecimal(value);
        if (amount.compareTo(new BigDecimal("0.01")) < 0 || amount.compareTo(maximum) > 0) {
            details.add(new ErrorDetail(field, "range", "Amount is outside the configured range."));
        }
        return amount;
    }

    private String currency(String value, List<ErrorDetail> details) {
        if (value == null || value.isBlank()) {
            details.add(new ErrorDetail("currency", "required", "Currency is required."));
            return null;
        }
        if (!value.matches("[A-Z]{3}")) {
            details.add(new ErrorDetail("currency", "format", "Currency must be three uppercase letters."));
            return value;
        }
        if (!properties.limits().currencyAllowedValues().isEmpty()
                && !properties.limits().currencyAllowedValues().contains(value)) {
            details.add(new ErrorDetail("currency", "allowed", "Currency is not allowed by configuration."));
        }
        return value;
    }

    private String description(String value, List<ErrorDetail> details) {
        if (value == null) {
            return null;
        }
        if (value.length() > properties.limits().maxDescriptionLength()) {
            details.add(new ErrorDetail("description", "length", "Description exceeds the configured length."));
        }
        if (value.chars().anyMatch(character -> character < 0x20 && character != '\t')) {
            details.add(new ErrorDetail("description", "format", "Description contains unsupported control characters."));
        }
        return value;
    }

    private UUID canonicalUuid(String value, String field, List<ErrorDetail> details) {
        if (value == null || value.isBlank()) {
            details.add(new ErrorDetail(field, "required", "Payment ID is required."));
            return new UUID(0L, 0L);
        }
        try {
            UUID id = UUID.fromString(value);
            if (!id.toString().equals(value)) {
                throw new IllegalArgumentException("Non-canonical UUID.");
            }
            return id;
        } catch (IllegalArgumentException exception) {
            details.add(new ErrorDetail(field, "format", "Payment ID must be a canonical UUID."));
            return new UUID(0L, 0L);
        }
    }

    private static String fingerprint(String type, String... fields) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(type.getBytes(StandardCharsets.UTF_8));
            for (String field : fields) {
                digest.update((byte) '\n');
                digest.update(field.getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the JDK.", exception);
        }
    }
}
