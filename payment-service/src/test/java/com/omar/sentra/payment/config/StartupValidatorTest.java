package com.omar.sentra.payment.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.omar.sentra.payment.TestProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class StartupValidatorTest {

    @Test
    void acceptsDocumentedTestConfiguration() {
        assertThatCode(() -> StartupValidator.validate(new String[] {"test"}, TestProperties.defaults()))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingProfileChangedRoutesAndDisabledSafety() {
        assertThatThrownBy(() -> StartupValidator.validate(new String[0], TestProperties.defaults()))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> StartupValidator.validate(
                        new String[] {"test"},
                        TestProperties.create(
                                true,
                                List.of("partner-payment-read"),
                                List.of("127.0.0.1/32"),
                                10000,
                                Duration.ofHours(24))))
                .isInstanceOf(IllegalStateException.class);

        PaymentServiceProperties defaults = TestProperties.defaults();
        PaymentServiceProperties unsafe = new PaymentServiceProperties(
                defaults.environment(),
                defaults.instanceId(),
                defaults.serviceName(),
                defaults.shutdownTimeout(),
                defaults.repository(),
                defaults.gateway(),
                new PaymentServiceProperties.Signature(
                        false,
                        "X-Sentra-Signature-Verified",
                        "X-Sentra-Signature-Key-Id",
                        "X-Sentra-Nonce-Status",
                        "accepted"),
                defaults.limits(),
                defaults.idempotency(),
                defaults.management(),
                defaults.logging());
        assertThatThrownBy(() -> StartupValidator.validate(new String[] {"test"}, unsafe))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void productionFailsClosedForSeedsAndWildcardPeers() {
        PaymentServiceProperties defaults = TestProperties.defaults();
        PaymentServiceProperties unsafe = new PaymentServiceProperties(
                "prod",
                defaults.instanceId(),
                "payment-service",
                defaults.shutdownTimeout(),
                new PaymentServiceProperties.Repository("memory", true, "default", List.of()),
                new PaymentServiceProperties.Gateway(
                        true,
                        defaults.gateway().allowedRouteIds(),
                        List.of("0.0.0.0/0"),
                        255,
                        128,
                        120,
                        120),
                defaults.signature(),
                defaults.limits(),
                defaults.idempotency(),
                defaults.management(),
                new PaymentServiceProperties.Logging("json", false, false));

        assertThatThrownBy(() -> StartupValidator.validate(new String[] {"prod"}, unsafe))
                .isInstanceOf(IllegalStateException.class);
    }
}
