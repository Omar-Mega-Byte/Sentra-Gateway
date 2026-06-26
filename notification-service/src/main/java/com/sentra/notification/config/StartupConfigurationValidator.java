package com.sentra.notification.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Enforces the documented startup safety rules before the service accepts
 * traffic.
 */
@Component
public class StartupConfigurationValidator implements SmartInitializingSingleton {

    private final NotificationServiceProperties properties;
    private final Environment environment;

    /**
     * Creates a startup validator around bound configuration and active
     * profiles.
     *
     * @param properties validated typed service properties
     * @param environment Spring environment used to inspect active profiles
     */
    public StartupConfigurationValidator(NotificationServiceProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<String> errors = new ArrayList<>();
        List<String> activeProfiles = List.of(environment.getActiveProfiles());
        boolean productionLike = !properties.localOrTest(activeProfiles);

        if (activeProfiles.isEmpty()) {
            errors.add("SPRING_PROFILES_ACTIVE must be set to local, test, or prod");
        }
        validateRepository(errors, productionLike);
        validateGateway(errors, productionLike);
        validateLimits(errors);
        validateFaults(errors, productionLike);
        validateManagement(errors, productionLike);
        validateLogging(errors, productionLike);

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid notification-service configuration: " + String.join("; ", errors));
        }
    }

    private void validateRepository(List<String> errors, boolean productionLike) {
        if (!"memory".equalsIgnoreCase(properties.repository().mode())) {
            errors.add("NOTIFICATION_REPOSITORY_MODE must be memory");
        }
        if (!"default".equalsIgnoreCase(properties.repository().seedDataset())) {
            errors.add("NOTIFICATION_SEED_DATASET must be default");
        }
        if (productionLike && properties.repository().seedEnabled()) {
            errors.add("NOTIFICATION_SEED_ENABLED must be false in production-like profiles");
        }
    }

    private void validateGateway(List<String> errors, boolean productionLike) {
        List<String> routeIds = properties.gateway().allowedRouteIds();
        Set<String> uniqueRouteIds = new HashSet<>(routeIds);
        if (uniqueRouteIds.size() != routeIds.size()) {
            errors.add("GATEWAY_ALLOWED_ROUTE_IDS must not contain duplicates");
        }
        if (routeIds.stream().anyMatch(route -> route.contains("*"))) {
            errors.add("GATEWAY_ALLOWED_ROUTE_IDS must not contain wildcards");
        }
        if (!uniqueRouteIds.equals(new HashSet<>(NotificationServiceProperties.DOCUMENTED_ROUTE_IDS))) {
            errors.add("GATEWAY_ALLOWED_ROUTE_IDS must exactly match the documented notification route IDs");
        }
        if (productionLike && !properties.gateway().contextRequired()) {
            errors.add("GATEWAY_CONTEXT_REQUIRED must remain true in production-like profiles");
        }
        if (productionLike && blankList(properties.gateway().allowedPeers())) {
            errors.add("GATEWAY_ALLOWED_PEERS must not be empty in production-like profiles");
        }
    }

    private void validateLimits(List<String> errors) {
        NotificationServiceProperties.Limits limits = properties.limits();
        if (limits.defaultPageSize() > limits.maxPageSize()) {
            errors.add("DEFAULT_PAGE_SIZE must be less than or equal to MAX_PAGE_SIZE");
        }
        if (limits.maxTitleLength() > limits.maxMessageLength()) {
            errors.add("MAX_TITLE_LENGTH must be less than or equal to MAX_MESSAGE_LENGTH");
        }
    }

    private void validateFaults(List<String> errors, boolean productionLike) {
        NotificationServiceProperties.Fault fault = properties.fault();
        if (productionLike && fault.controlsEnabled()) {
            errors.add("FAULT_CONTROLS_ENABLED must be false in production-like profiles");
        }
        if (productionLike && (fault.allowDelay() || fault.allowStatus() || fault.allowMalformed() || fault.allowDisconnect())) {
            errors.add("FAULT_ALLOW_* flags must be false in production-like profiles");
        }
        if (fault.allowedStatuses().stream().anyMatch(status -> status < 400 || status > 599)) {
            errors.add("FAULT_ALLOWED_STATUSES must contain only 4xx or 5xx statuses");
        }
    }

    private void validateManagement(List<String> errors, boolean productionLike) {
        if (productionLike && properties.management().swaggerUiEnabled() && blankList(properties.management().allowedCidrs())) {
            errors.add("SWAGGER_UI_ENABLED requires protected management access in production-like profiles");
        }
    }

    private void validateLogging(List<String> errors, boolean productionLike) {
        String root = properties.logging().rootLevel().toUpperCase(Locale.ROOT);
        if (productionLike && ("DEBUG".equals(root) || "TRACE".equals(root))) {
            errors.add("LOG_LEVEL_ROOT must not default to DEBUG or TRACE in production-like profiles");
        }
        if (properties.logging().includeOwnerReferences()) {
            errors.add("LOG_INCLUDE_OWNER_REFERENCES must remain false");
        }
        if (productionLike && properties.logging().includeNotificationIds()) {
            errors.add("LOG_INCLUDE_NOTIFICATION_IDS must remain false in production-like profiles");
        }
    }

    private boolean blankList(List<String> values) {
        return values == null || values.stream().map(value -> value == null ? "" : value.trim()).allMatch(String::isBlank);
    }
}
