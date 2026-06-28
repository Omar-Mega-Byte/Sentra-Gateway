package com.omar.sentra.order;

import com.omar.sentra.order.config.OrderServiceProperties;
import java.time.Duration;
import java.util.List;

public final class TestProperties {
    private TestProperties() {}

    public static OrderServiceProperties create(
            boolean seedEnabled,
            boolean contextRequired,
            List<String> routes,
            List<String> peers,
            int maxRecords,
            Duration retention) {
        return new OrderServiceProperties(
                "test",
                "order-service-test",
                "order-service",
                Duration.ofSeconds(20),
                new OrderServiceProperties.Repository("memory", seedEnabled, "default"),
                new OrderServiceProperties.Gateway(
                        contextRequired,
                        routes,
                        peers,
                        255,
                        128,
                        255,
                        128),
                new OrderServiceProperties.Limits(
                        32768,
                        100,
                        20,
                        10000,
                        50,
                        64,
                        100,
                        20),
                new OrderServiceProperties.Idempotency(
                        true,
                        128,
                        retention,
                        maxRecords,
                        Duration.ofMinutes(5)),
                new OrderServiceProperties.Management(
                        8082,
                        List.of("127.0.0.1/32"),
                        true,
                        true,
                        true),
                new OrderServiceProperties.Logging("text", false, false));
    }

    public static OrderServiceProperties defaults() {
        return create(
                true,
                true,
                List.of(
                        "orders-list",
                        "orders-get",
                        "orders-create",
                        "orders-cancel",
                        "admin-orders-list",
                        "admin-orders-update"),
                List.of("127.0.0.1/32"),
                10000,
                Duration.ofHours(24));
    }
}
