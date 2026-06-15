package com.omar.sentra.user.config;

import java.util.Arrays;
import java.util.Set;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Rejects unsupported or unsafe runtime configuration before the service
 * accepts traffic.
 */
@Component
public class StartupValidator implements SmartInitializingSingleton {
    private static final Set<String> DEVELOPMENT_PROFILES = Set.of("local", "test");

    private final Environment environment;
    private final UserServiceProperties properties;

    public StartupValidator(Environment environment, UserServiceProperties properties) {
        this.environment = environment;
        this.properties = properties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        validate(environment.getActiveProfiles(), properties);
    }

    /**
     * Validates the active profiles and complete typed configuration.
     *
     * @param activeProfiles active Spring profiles
     * @param properties typed user-service properties
     */
    public static void validate(String[] activeProfiles, UserServiceProperties properties) {
        if (activeProfiles.length == 0) {
            throw new IllegalStateException("An explicit Spring profile is required.");
        }
        if (!"memory".equals(properties.repository().mode())) {
            throw new IllegalStateException("Only the documented memory repository mode is supported.");
        }
        if (properties.shutdownTimeout().isZero() || properties.shutdownTimeout().isNegative()) {
            throw new IllegalStateException("SHUTDOWN_TIMEOUT must be positive.");
        }

        boolean productionLike = Arrays.stream(activeProfiles)
                .map(String::toLowerCase)
                .anyMatch(profile -> !DEVELOPMENT_PROFILES.contains(profile));
        if (!productionLike) {
            return;
        }
        if (!"user-service".equals(properties.serviceName())) {
            throw new IllegalStateException("SERVICE_NAME must be user-service in production-like profiles.");
        }
        if (!properties.gateway().contextRequired()) {
            throw new IllegalStateException("Gateway trusted context cannot be disabled in production.");
        }
        if (properties.gateway().allowedRouteIds().isEmpty()) {
            throw new IllegalStateException("Production requires at least one allowed gateway route ID.");
        }
        if (properties.repository().seedEnabled()) {
            throw new IllegalStateException("Deterministic profile seeds are forbidden in production.");
        }
        if (properties.management().openapiEnabled() || properties.management().swaggerUiEnabled()) {
            throw new IllegalStateException("OpenAPI and Swagger UI must be disabled in production.");
        }
        if (properties.logging().includeProfileIds()) {
            throw new IllegalStateException("Profile identifiers must not be logged in production.");
        }
        if (properties.management().metricsEnabled()
                && properties.management().allowedCidrs().isEmpty()) {
            throw new IllegalStateException(
                    "Production metrics require an operations-network CIDR allowlist.");
        }
    }
}
