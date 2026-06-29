package com.omar.sentra.payment.config;

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
 * Type-safe payment-service configuration bound from environment-backed
 * properties.
 *
 * @param environment deployment environment label
 * @param instanceId instance identity
 * @param serviceName fixed service name
 * @param shutdownTimeout graceful shutdown budget
 * @param repository repository and deterministic simulation settings
 * @param gateway trusted gateway-context settings
 * @param signature signature-evidence header settings
 * @param limits request and payment domain limits
 * @param idempotency mutation idempotency settings
 * @param management management and OpenAPI settings
 * @param logging logging safety settings
 */
@Validated
@ConfigurationProperties(prefix = "sentra.payment")
public record PaymentServiceProperties(
        @NotBlank String environment,
        @NotBlank String instanceId,
        @NotBlank String serviceName,
        @NotNull Duration shutdownTimeout,
        @Valid @NotNull Repository repository,
        @Valid @NotNull Gateway gateway,
        @Valid @NotNull Signature signature,
        @Valid @NotNull Limits limits,
        @Valid @NotNull Idempotency idempotency,
        @Valid @NotNull Management management,
        @Valid @NotNull Logging logging) {

    /**
     * Repository and deterministic mock-provider configuration.
     *
     * @param mode repository implementation mode
     * @param seedEnabled whether deterministic payment/refund data is loaded
     * @param seedDataset deterministic dataset name
     * @param mockDeclineReferences local/test references that should decline
     */
    public record Repository(
            @NotBlank @Pattern(regexp = "memory") String mode,
            boolean seedEnabled,
            @NotBlank @Pattern(regexp = "default") String seedDataset,
            @NotNull List<String> mockDeclineReferences) {

        public Repository {
            mockDeclineReferences = visibleList(mockDeclineReferences);
        }
    }

    /**
     * Trusted gateway context configuration.
     *
     * @param contextRequired whether gateway-created context is mandatory
     * @param allowedRouteIds exact accepted route identifiers
     * @param allowedPeers peer IP, CIDR, or service-name allowlist
     * @param trustedHeaderMaxLength generic trusted-header maximum
     * @param requestIdMaxLength request-ID maximum
     * @param clientIdMaxLength client-ID maximum
     * @param keyIdMaxLength key-ID maximum
     */
    public record Gateway(
            boolean contextRequired,
            @NotNull List<@NotBlank String> allowedRouteIds,
            @NotNull List<String> allowedPeers,
            @Min(32) @Max(4096) int trustedHeaderMaxLength,
            @Min(16) @Max(512) int requestIdMaxLength,
            @Min(16) @Max(1024) int clientIdMaxLength,
            @Min(16) @Max(512) int keyIdMaxLength) {

        public Gateway {
            allowedRouteIds = visibleList(allowedRouteIds);
            allowedPeers = visibleList(allowedPeers);
        }
    }

    /**
     * Signature-validation evidence settings consumed from gateway-created
     * headers.
     *
     * @param evidenceRequiredForMutations whether mutation routes require evidence
     * @param verifiedHeader header containing boolean verification result
     * @param keyIdHeader header containing signing-key identifier
     * @param nonceStatusHeader header containing replay nonce result
     * @param nonceAcceptedValue required accepted nonce status value
     */
    public record Signature(
            boolean evidenceRequiredForMutations,
            @NotBlank String verifiedHeader,
            @NotBlank String keyIdHeader,
            @NotBlank String nonceStatusHeader,
            @NotBlank String nonceAcceptedValue) {}

    /**
     * Request and payment-domain validation limits.
     *
     * @param maxRequestBodyBytes mutation body maximum
     * @param maxMerchantReferenceLength merchant reference maximum
     * @param maxDescriptionLength payment description maximum
     * @param maxAmount maximum accepted amount as a decimal string
     * @param currencyAllowedValues optional uppercase currency allowlist
     * @param maxErrorDetails maximum validation details returned
     */
    public record Limits(
            @Min(1024) @Max(1048576) int maxRequestBodyBytes,
            @Min(8) @Max(255) int maxMerchantReferenceLength,
            @Min(0) @Max(1000) int maxDescriptionLength,
            @NotBlank String maxAmount,
            @NotNull List<String> currencyAllowedValues,
            @Min(1) @Max(100) int maxErrorDetails) {

        public Limits {
            currencyAllowedValues = visibleList(currencyAllowedValues);
        }
    }

    /**
     * Idempotency policy for high-risk mutations.
     *
     * @param enabled whether idempotency is enabled
     * @param requiredForMutations whether mutation routes require a key
     * @param keyMaxLength maximum visible ASCII key length
     * @param retention live record retention
     * @param maxRecords maximum live records
     * @param cleanupInterval scheduled cleanup interval
     */
    public record Idempotency(
            boolean enabled,
            boolean requiredForMutations,
            @Min(16) @Max(255) int keyMaxLength,
            @NotNull Duration retention,
            @Min(100) @Max(1000000) int maxRecords,
            @NotNull Duration cleanupInterval) {}

    /**
     * Management endpoint and OpenAPI configuration.
     *
     * @param port management listener port
     * @param allowedCidrs operations-network allowlist
     * @param openapiEnabled whether OpenAPI is published
     * @param swaggerUiEnabled whether Swagger UI is published
     * @param metricsEnabled whether metrics are enabled
     */
    public record Management(
            @Min(1) @Max(65535) int port,
            @NotNull List<String> allowedCidrs,
            boolean openapiEnabled,
            boolean swaggerUiEnabled,
            boolean metricsEnabled) {

        public Management {
            allowedCidrs = visibleList(allowedCidrs);
        }
    }

    /**
     * Logging safety configuration.
     *
     * @param format output format label
     * @param includePaymentIds whether payment identifiers may be logged
     * @param includeClientIds whether client identifiers may be logged
     */
    public record Logging(
            @NotBlank @Pattern(regexp = "text|json") String format,
            boolean includePaymentIds,
            boolean includeClientIds) {}

    private static List<String> visibleList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }
}
