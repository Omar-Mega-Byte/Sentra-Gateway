package com.omar.sentra.user;

import com.omar.sentra.user.config.UserServiceProperties;
import java.time.Duration;
import java.util.List;

public final class TestProperties {
    private TestProperties() {}

    public static UserServiceProperties create(
            boolean seedEnabled,
            boolean contextRequired,
            List<String> routes,
            boolean openApi,
            boolean swagger,
            boolean metrics,
            List<String> managementCidrs) {
        return new UserServiceProperties(
                "test",
                "test-instance",
                "user-service",
                Duration.ofSeconds(20),
                new UserServiceProperties.Repository("memory", seedEnabled, true),
                new UserServiceProperties.Gateway(
                        contextRequired,
                        routes,
                        List.of(),
                        255,
                        128),
                new UserServiceProperties.Limits(
                        16384,
                        100,
                        500,
                        2048,
                        254,
                        35,
                        64,
                        20),
                new UserServiceProperties.PublicProfile(
                        true,
                        "public, max-age=60",
                        "no-store"),
                new UserServiceProperties.Management(
                        8081,
                        managementCidrs,
                        openApi,
                        swagger,
                        metrics),
                new UserServiceProperties.Logging(false));
    }

    public static UserServiceProperties defaults() {
        return create(
                true,
                true,
                List.of("user-public-profile", "user-profile-read", "user-profile-update"),
                true,
                true,
                true,
                List.of("127.0.0.1/32"));
    }
}
