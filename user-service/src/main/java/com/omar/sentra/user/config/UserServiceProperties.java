package com.omar.sentra.user.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe user-service configuration bound from environment-backed properties.
 *
 * @param environment deployment environment label
 * @param instanceId instance identity
 * @param serviceName fixed service name
 * @param shutdownTimeout graceful shutdown budget
 * @param repository repository settings
 * @param gateway trusted gateway-context settings
 * @param limits request and field limits
 * @param publicProfile public-profile policy
 * @param management management and OpenAPI settings
 * @param logging logging safety settings
 */
@Validated
@ConfigurationProperties(prefix = "sentra.user")
public record UserServiceProperties(
        @NotBlank String environment,
        @NotBlank String instanceId,
        @NotBlank String serviceName,
        @NotNull Duration shutdownTimeout,
        @Valid @NotNull Repository repository,
        @Valid @NotNull Gateway gateway,
        @Valid @NotNull Limits limits,
        @Valid @NotNull PublicProfile publicProfile,
        @Valid @NotNull Management management,
        @Valid @NotNull Logging logging) {

    /**
     * Repository configuration.
     *
     * @param mode repository implementation mode
     * @param seedEnabled whether deterministic sample profiles are loaded
     * @param emailUnique whether normalized emails must be unique
     */
    public record Repository(
            @NotBlank @Pattern(regexp = "memory") String mode,
            boolean seedEnabled,
            boolean emailUnique) {}

    /**
     * Trusted gateway context configuration.
     *
     * @param contextRequired whether routed provenance headers are mandatory
     * @param allowedRouteIds accepted gateway route identifiers
     * @param allowedPeers optional peer IP, CIDR, or service-name allowlist
     * @param trustedHeaderMaxLength generic trusted-header maximum
     * @param requestIdMaxLength request-ID maximum
     */
    public record Gateway(
            boolean contextRequired,
            @NotNull List<@NotBlank String> allowedRouteIds,
            @NotNull List<@NotBlank String> allowedPeers,
            @Min(32) @Max(4096) int trustedHeaderMaxLength,
            @Min(16) @Max(512) int requestIdMaxLength) {

        public Gateway {
            allowedRouteIds = List.copyOf(allowedRouteIds);
            allowedPeers = List.copyOf(allowedPeers);
        }
    }

    /**
     * Request and profile-field limits.
     *
     * @param maxRequestBodyBytes mutation-body maximum
     * @param maxDisplayNameLength display-name maximum
     * @param maxBioLength biography maximum
     * @param maxAvatarUrlLength avatar URL maximum
     * @param maxEmailLength email maximum
     * @param maxLocaleLength locale maximum
     * @param maxTimezoneLength time-zone maximum
     * @param maxErrorDetails validation-detail maximum
     */
    public record Limits(
            @Min(1024) @Max(1048576) int maxRequestBodyBytes,
            @Min(1) @Max(200) int maxDisplayNameLength,
            @Min(0) @Max(5000) int maxBioLength,
            @Min(128) @Max(8192) int maxAvatarUrlLength,
            @Min(64) @Max(320) int maxEmailLength,
            @Min(2) @Max(64) int maxLocaleLength,
            @Min(16) @Max(128) int maxTimezoneLength,
            @Min(1) @Max(100) int maxErrorDetails) {}

    /**
     * Public profile response policy.
     *
     * @param enabled whether public lookup is enabled
     * @param cacheControl successful public response cache policy
     * @param notFoundCacheControl public lookup failure cache policy
     */
    public record PublicProfile(
            boolean enabled,
            @NotBlank String cacheControl,
            @NotBlank String notFoundCacheControl) {}

    /**
     * Management endpoint configuration.
     *
     * @param port management HTTP port
     * @param allowedCidrs optional operations-network allowlist
     * @param openapiEnabled whether OpenAPI is published
     * @param swaggerUiEnabled whether Swagger UI is published
     * @param metricsEnabled whether metrics are enabled
     */
    public record Management(
            @Min(1) @Max(65535) int port,
            @NotNull List<@NotBlank String> allowedCidrs,
            boolean openapiEnabled,
            boolean swaggerUiEnabled,
            boolean metricsEnabled) {

        public Management {
            allowedCidrs = List.copyOf(allowedCidrs);
        }
    }

    /**
     * Logging safety configuration.
     *
     * @param includeProfileIds whether profile IDs may be logged
     */
    public record Logging(boolean includeProfileIds) {}
}
