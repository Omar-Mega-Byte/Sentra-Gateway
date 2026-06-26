package com.sentra.notification.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * Unit tests for fail-closed startup configuration validation.
 */
class StartupConfigurationValidatorTest {

    @Test
    void acceptsLocalDevelopmentSettings() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        assertThatCode(() -> new StartupConfigurationValidator(localProperties(), environment).afterSingletonsInstantiated())
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsProductionFaultControlsAndSeedData() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        NotificationServiceProperties properties = new NotificationServiceProperties(
                "notification-service",
                "prod",
                "notification-service-prod",
                Duration.ofSeconds(20),
                new NotificationServiceProperties.Repository("memory", true, "default"),
                new NotificationServiceProperties.Gateway(true, NotificationServiceProperties.DOCUMENTED_ROUTE_IDS, List.of("10.0.0.10"), 255, 128, 255, 128),
                new NotificationServiceProperties.Limits(16384, 20, 100, 10000, 120, 1000, 128, 20),
                new NotificationServiceProperties.Fault(true, true, true, true, true, 5000, List.of(500, 502, 503, 504), 1000),
                new NotificationServiceProperties.Management(List.of("10.0.0.0/24"), true, false, false),
                new NotificationServiceProperties.Logging("INFO", "json", false, false));

        assertThatThrownBy(() -> new StartupConfigurationValidator(properties, environment).afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FAULT_CONTROLS_ENABLED")
                .hasMessageContaining("NOTIFICATION_SEED_ENABLED");
    }

    @Test
    void rejectsUnknownRouteCatalog() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        NotificationServiceProperties properties = prodPropertiesWithRoutes(List.of("notifications-list", "notification-*", "admin-test-notification"));

        assertThatThrownBy(() -> new StartupConfigurationValidator(properties, environment).afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GATEWAY_ALLOWED_ROUTE_IDS");
    }

    private NotificationServiceProperties localProperties() {
        return new NotificationServiceProperties(
                "notification-service",
                "local",
                "notification-service-local",
                Duration.ofSeconds(20),
                new NotificationServiceProperties.Repository("memory", true, "default"),
                new NotificationServiceProperties.Gateway(true, NotificationServiceProperties.DOCUMENTED_ROUTE_IDS, List.of("gateway-service"), 255, 128, 255, 128),
                new NotificationServiceProperties.Limits(16384, 20, 100, 10000, 120, 1000, 128, 20),
                new NotificationServiceProperties.Fault(true, true, true, true, true, 5000, List.of(500, 502, 503, 504), 1000),
                new NotificationServiceProperties.Management(List.of(), true, true, true),
                new NotificationServiceProperties.Logging("INFO", "text", false, false));
    }

    private NotificationServiceProperties prodPropertiesWithRoutes(List<String> routeIds) {
        return new NotificationServiceProperties(
                "notification-service",
                "prod",
                "notification-service-prod",
                Duration.ofSeconds(20),
                new NotificationServiceProperties.Repository("memory", false, "default"),
                new NotificationServiceProperties.Gateway(true, routeIds, List.of("10.0.0.10"), 255, 128, 255, 128),
                new NotificationServiceProperties.Limits(16384, 20, 100, 10000, 120, 1000, 128, 20),
                new NotificationServiceProperties.Fault(false, false, false, false, false, 5000, List.of(500, 502, 503, 504), 1000),
                new NotificationServiceProperties.Management(List.of("10.0.0.0/24"), true, false, false),
                new NotificationServiceProperties.Logging("INFO", "json", false, false));
    }
}
