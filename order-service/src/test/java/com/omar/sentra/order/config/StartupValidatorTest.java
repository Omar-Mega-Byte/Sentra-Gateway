package com.omar.sentra.order.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.omar.sentra.order.TestProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class StartupValidatorTest {

    @Test
    void acceptsDocumentedTestConfiguration() {
        assertThatCode(() -> StartupValidator.validate(
                        new String[] {"test"},
                        TestProperties.defaults()))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingProfileAndChangedRouteCatalog() {
        assertThatThrownBy(() -> StartupValidator.validate(
                        new String[0],
                        TestProperties.defaults()))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> StartupValidator.validate(
                        new String[] {"test"},
                        TestProperties.create(
                                true,
                                true,
                                List.of("orders-list"),
                                List.of(),
                                10000,
                                Duration.ofHours(24))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void productionFailsClosedForSeedsAndWildcardPeers() {
        OrderServiceProperties test = TestProperties.defaults();
        OrderServiceProperties unsafe = new OrderServiceProperties(
                "prod",
                test.instanceId(),
                test.serviceName(),
                test.shutdownTimeout(),
                test.repository(),
                new OrderServiceProperties.Gateway(
                        true,
                        test.gateway().allowedRouteIds(),
                        List.of("0.0.0.0/0"),
                        255,
                        128,
                        255,
                        128),
                test.limits(),
                test.idempotency(),
                test.management(),
                new OrderServiceProperties.Logging("json", false, false));

        assertThatThrownBy(() -> StartupValidator.validate(new String[] {"prod"}, unsafe))
                .isInstanceOf(IllegalStateException.class);
    }
}
