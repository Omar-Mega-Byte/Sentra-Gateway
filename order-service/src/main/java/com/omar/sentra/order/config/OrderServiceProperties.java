package com.omar.sentra.order.config;

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
 * Type-safe order-service configuration bound from environment-backed
 * properties.
 *
 * @param environment deployment environment label
 * @param instanceId instance identity
 * @param serviceName fixed service name
 * @param shutdownTimeout graceful shutdown budget
 * @param repository repository settings
 * @param gateway trusted gateway-context settings
 * @param limits request and domain limits
 * @param idempotency idempotency settings
 * @param management management and OpenAPI settings
 * @param logging logging safety settings
 */
@Validated
@ConfigurationProperties(prefix = "sentra.order")
public record OrderServiceProperties(
        @NotBlank String environment,
        @NotBlank String instanceId,
        @NotBlank String serviceName,
        @NotNull Duration shutdownTimeout,
        @Valid @NotNull Repository repository,
        @Valid @NotNull Gateway gateway,
        @Valid @NotNull Limits limits,
        @Valid @NotNull Idempotency idempotency,
        @Valid @NotNull Management management,
        @Valid @NotNull Logging logging) {

    /**
     * Repository configuration.
     *
     * @param mode repository implementation mode
     * @param seedEnabled whether deterministic sample orders are loaded
     * @param seedDataset deterministic dataset name
     */
    public record Repository(
            @NotBlank @Pattern(regexp = "memory|postgres") String mode,
            boolean seedEnabled,
            @NotBlank @Pattern(regexp = "default") String seedDataset) {}

    /**
     * Trusted gateway context configuration.
     *
     * @param contextRequired whether gateway-created context is mandatory
     * @param allowedRouteIds exact accepted route identifiers
     * @param allowedPeers peer IP, CIDR, or service-name allowlist
     * @param trustedHeaderMaxLength generic trusted-header maximum
     * @param requestIdMaxLength request-ID maximum
     * @param subjectMaxLength subject maximum
     * @param tenantIdMaxLength tenant maximum
     */
    public record Gateway(
            boolean contextRequired,
            @NotNull List<@NotBlank String> allowedRouteIds,
            @NotNull List<@NotBlank String> allowedPeers,
            @Min(32) @Max(4096) int trustedHeaderMaxLength,
            @Min(16) @Max(512) int requestIdMaxLength,
            @Min(16) @Max(1024) int subjectMaxLength,
            @Min(16) @Max(512) int tenantIdMaxLength) {

        public Gateway {
            allowedRouteIds = List.copyOf(allowedRouteIds);
            allowedPeers = List.copyOf(allowedPeers);
        }
    }

    /**
     * Request, pagination, and order limits.
     *
     * @param maxRequestBodyBytes create-body maximum
     * @param maxPageSize maximum page size
     * @param defaultPageSize default page size
     * @param maxPageNumber maximum zero-based page
     * @param maxItemsPerOrder maximum item count
     * @param maxSkuLength maximum SKU length
     * @param maxItemQuantity maximum item quantity
     * @param maxErrorDetails maximum validation detail count
     */
    public record Limits(
            @Min(1024) @Max(1048576) int maxRequestBodyBytes,
            @Min(1) @Max(500) int maxPageSize,
            @Min(1) @Max(500) int defaultPageSize,
            @Min(100) @Max(1000000) int maxPageNumber,
            @Min(1) @Max(500) int maxItemsPerOrder,
            @Min(8) @Max(255) int maxSkuLength,
            @Min(1) @Max(100000) int maxItemQuantity,
            @Min(1) @Max(100) int maxErrorDetails) {}

    /**
     * Idempotency policy.
     *
     * @param enabled whether keyed create idempotency is enabled
     * @param keyMaxLength maximum visible ASCII key length
     * @param retention live record retention
     * @param maxRecords maximum live records
     * @param cleanupInterval scheduled cleanup interval
     */
    public record Idempotency(
            boolean enabled,
            @Min(16) @Max(255) int keyMaxLength,
            @NotNull Duration retention,
            @Min(100) @Max(1000000) int maxRecords,
            @NotNull Duration cleanupInterval) {}

    /**
     * Management endpoint configuration.
     *
     * @param port management listener port
     * @param allowedCidrs operations-network allowlist
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
     * @param format output format label
     * @param includeOrderIds whether order identifiers may be logged
     * @param includeOwnerReferences whether subject or tenant references may be logged
     */
    public record Logging(
            @NotBlank @Pattern(regexp = "text|json") String format,
            boolean includeOrderIds,
            boolean includeOwnerReferences) {}
}
