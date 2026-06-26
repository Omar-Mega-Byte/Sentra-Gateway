package com.sentra.notification.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed configuration catalog for the notification service.
 *
 * <p>Values are sourced through Spring Boot's normal precedence rules. Podman
 * Compose loads the documented {@code .env} file and exposes those values as
 * environment variables; direct JVM launches must provide equivalent
 * environment variables or command-line arguments.</p>
 */
@Validated
@ConfigurationProperties(prefix = "sentra")
public record NotificationServiceProperties(
        @NotBlank String serviceName,
        @NotBlank String environment,
        @NotBlank String instanceId,
        @NotNull Duration shutdownTimeout,
        @Valid @NotNull Repository repository,
        @Valid @NotNull Gateway gateway,
        @Valid @NotNull Limits limits,
        @Valid @NotNull Fault fault,
        @Valid @NotNull Management management,
        @Valid @NotNull Logging logging) {

    /** Route ID used by the notification list operation. */
    public static final String ROUTE_NOTIFICATIONS_LIST = "notifications-list";
    /** Route ID used by the preference mutation operation. */
    public static final String ROUTE_PREFERENCES_UPDATE = "notification-preferences-update";
    /** Route ID used by the admin test operation. */
    public static final String ROUTE_ADMIN_TEST = "admin-test-notification";
    /** Supported route IDs in their documented order. */
    public static final List<String> DOCUMENTED_ROUTE_IDS = List.of(
            ROUTE_NOTIFICATIONS_LIST,
            ROUTE_PREFERENCES_UPDATE,
            ROUTE_ADMIN_TEST);

    /**
     * Returns whether this configuration represents a local or automated test
     * environment where development-only fault controls may be enabled.
     *
     * @param activeProfiles Spring profiles active for this process
     * @return true when either the active profiles or explicit environment are local/test
     */
    public boolean localOrTest(List<String> activeProfiles) {
        return "local".equalsIgnoreCase(environment)
                || "test".equalsIgnoreCase(environment)
                || activeProfiles.stream().anyMatch(profile ->
                        "local".equalsIgnoreCase(profile) || "test".equalsIgnoreCase(profile));
    }

    /** Repository and deterministic seed configuration. */
    public record Repository(@NotBlank String mode, boolean seedEnabled, @NotBlank String seedDataset) {
    }

    /** Gateway trusted-context and peer configuration. */
    public record Gateway(
            boolean contextRequired,
            @NotEmpty List<@NotBlank String> allowedRouteIds,
            List<String> allowedPeers,
            @Min(1) @Max(4096) int trustedHeaderMaxLength,
            @Min(1) @Max(4096) int requestIdMaxLength,
            @Min(1) @Max(4096) int subjectMaxLength,
            @Min(1) @Max(4096) int tenantIdMaxLength) {
    }

    /** Request, pagination, and public field-size limits. */
    public record Limits(
            @Min(1024) @Max(1048576) int maxRequestBodyBytes,
            @Min(1) int defaultPageSize,
            @Min(1) @Max(500) int maxPageSize,
            @Min(100) @Max(1000000) int maxPageNumber,
            @Min(1) @Max(500) int maxTitleLength,
            @Min(1) @Max(5000) int maxMessageLength,
            @Min(1) @Max(255) int maxRecipientReferenceLength,
            @Min(1) @Max(100) int maxErrorDetails) {
    }

    /** Development-only fault simulation policy. */
    public record Fault(
            boolean controlsEnabled,
            boolean allowDelay,
            boolean allowStatus,
            boolean allowMalformed,
            boolean allowDisconnect,
            @Min(0) @Max(30000) int maxDelayMs,
            @NotEmpty List<@Min(400) @Max(599) Integer> allowedStatuses,
            @Min(0) @Max(100000) int failOnceCacheSize) {
    }

    /** Management, OpenAPI, and protected operations exposure settings. */
    public record Management(List<String> allowedCidrs, boolean metricsEnabled, boolean openapiEnabled, boolean swaggerUiEnabled) {
    }

    /** Redaction-oriented logging policy settings. */
    public record Logging(@NotBlank String rootLevel, @NotBlank String format, boolean includeNotificationIds, boolean includeOwnerReferences) {
    }
}
