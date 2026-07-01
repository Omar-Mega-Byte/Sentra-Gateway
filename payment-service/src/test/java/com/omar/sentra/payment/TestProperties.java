package com.omar.sentra.payment;

import com.omar.sentra.payment.config.PaymentServiceProperties;
import java.time.Duration;
import java.util.List;

public final class TestProperties {
    private TestProperties() {}

    public static PaymentServiceProperties create(
            boolean seedEnabled,
            List<String> routes,
            List<String> peers,
            int maxRecords,
            Duration retention) {
        return new PaymentServiceProperties(
                "test",
                "payment-service-test",
                "payment-service",
                Duration.ofSeconds(20),
                new PaymentServiceProperties.Repository("memory", seedEnabled, "default", List.of()),
                new PaymentServiceProperties.Gateway(
                        true,
                        routes,
                        peers,
                        255,
                        128,
                        120,
                        120),
                new PaymentServiceProperties.Signature(
                        true,
                        "X-Sentra-Signature-Verified",
                        "X-Sentra-Signature-Key-Id",
                        "X-Sentra-Nonce-Status",
                        "accepted"),
                new PaymentServiceProperties.Limits(
                        16384,
                        128,
                        255,
                        "10000.00",
                        List.of(),
                        20),
                new PaymentServiceProperties.Idempotency(
                        true,
                        true,
                        128,
                        retention,
                        maxRecords,
                        Duration.ofMinutes(5)),
                new PaymentServiceProperties.Management(
                        8083,
                        List.of("127.0.0.1/32"),
                        true,
                        true,
                        true),
                new PaymentServiceProperties.Logging("text", false, false));
    }

    public static PaymentServiceProperties defaults() {
        return create(
                true,
                List.of("partner-payment-read", "partner-payment-create", "partner-refund-create"),
                List.of("127.0.0.1/32"),
                10000,
                Duration.ofHours(24));
    }
}
