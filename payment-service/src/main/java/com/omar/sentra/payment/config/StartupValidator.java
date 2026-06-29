package com.omar.sentra.payment.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Rejects unsupported or unsafe payment-service configuration before traffic is
 * accepted.
 */
@Component
public class StartupValidator implements SmartInitializingSingleton {
    private static final Set<String> DEVELOPMENT_PROFILES = Set.of("local", "test");
    private static final Set<String> ROUTES =
            Set.of("partner-payment-read", "partner-payment-create", "partner-refund-create");

    private final Environment environment;
    private final PaymentServiceProperties properties;

    public StartupValidator(Environment environment, PaymentServiceProperties properties) {
        this.environment = environment;
        this.properties = properties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        validate(environment.getActiveProfiles(), properties);
        if (productionLike(environment.getActiveProfiles())) {
            validateLogLevel(environment.getProperty("logging.level.root", "INFO"));
            rejectBootDebugSwitch(environment, "debug");
            rejectBootDebugSwitch(environment, "trace");
        }
    }

    /**
     * Validates profiles and the complete typed configuration.
     *
     * @param activeProfiles active Spring profiles
     * @param properties payment-service properties
     */
    public static void validate(String[] activeProfiles, PaymentServiceProperties properties) {
        if (activeProfiles.length == 0) {
            throw new IllegalStateException("An explicit Spring profile is required.");
        }
        if (!"memory".equals(properties.repository().mode())
                || !"default".equals(properties.repository().seedDataset())) {
            throw new IllegalStateException("Only the documented memory repository and dataset are supported.");
        }
        durationBetween(properties.shutdownTimeout(), Duration.ofSeconds(5), Duration.ofSeconds(120), "SHUTDOWN_TIMEOUT");
        durationBetween(properties.idempotency().retention(), Duration.ofMinutes(1), Duration.ofDays(7), "IDEMPOTENCY_RETENTION");
        durationBetween(properties.idempotency().cleanupInterval(), Duration.ofSeconds(10), Duration.ofHours(1), "IDEMPOTENCY_CLEANUP_INTERVAL");
        validateMoney(properties.limits().maxAmount());

        Set<String> configuredRoutes = new HashSet<>(properties.gateway().allowedRouteIds());
        if (configuredRoutes.size() != properties.gateway().allowedRouteIds().size()
                || !configuredRoutes.equals(ROUTES)
                || configuredRoutes.stream().anyMatch(StartupValidator::wildcard)) {
            throw new IllegalStateException("Gateway route IDs must exactly match the documented catalog.");
        }
        if (!properties.signature().evidenceRequiredForMutations()) {
            throw new IllegalStateException("Mutation signature evidence must be required.");
        }
        if (properties.signature().verifiedHeader().isBlank()
                || properties.signature().keyIdHeader().isBlank()
                || properties.signature().nonceStatusHeader().isBlank()
                || properties.signature().nonceAcceptedValue().isBlank()) {
            throw new IllegalStateException("Signature evidence headers must be configured.");
        }
        if (!properties.idempotency().enabled() || !properties.idempotency().requiredForMutations()) {
            throw new IllegalStateException("Mutation idempotency must be enabled and required.");
        }
        for (String currency : properties.limits().currencyAllowedValues()) {
            if (!currency.matches("[A-Z]{3}")) {
                throw new IllegalStateException("CURRENCY_ALLOWED_VALUES must contain uppercase 3-letter codes.");
            }
        }

        boolean productionLike = productionLike(activeProfiles);
        if (!productionLike) {
            return;
        }
        if (DEVELOPMENT_PROFILES.contains(properties.environment().toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("SENTRA_ENVIRONMENT is required for production-like profiles.");
        }
        if (!"payment-service".equals(properties.serviceName())) {
            throw new IllegalStateException("SERVICE_NAME must be payment-service in production-like profiles.");
        }
        if (properties.repository().seedEnabled() || !properties.repository().mockDeclineReferences().isEmpty()) {
            throw new IllegalStateException("Deterministic payment seeds and mock declines are forbidden in production.");
        }
        if (!properties.gateway().contextRequired()) {
            throw new IllegalStateException("Gateway trusted context cannot be disabled in production.");
        }
        if (properties.gateway().allowedPeers().isEmpty()
                || properties.gateway().allowedPeers().stream().anyMatch(StartupValidator::wildcard)) {
            throw new IllegalStateException("Production requires a non-wildcard gateway peer allowlist.");
        }
        if (properties.management().allowedCidrs().isEmpty()
                || properties.management().allowedCidrs().stream().anyMatch(StartupValidator::wildcard)) {
            throw new IllegalStateException("Production requires a non-wildcard management CIDR allowlist.");
        }
        if (properties.management().swaggerUiEnabled() || properties.management().openapiEnabled()) {
            throw new IllegalStateException("OpenAPI and Swagger UI must be disabled in production.");
        }
        if (properties.logging().includePaymentIds() || properties.logging().includeClientIds()) {
            throw new IllegalStateException("Production logging cannot expose payment or client identifiers.");
        }
        if (!"json".equals(properties.logging().format())) {
            throw new IllegalStateException("Production logging must use the json format.");
        }
    }

    private static void validateMoney(String amount) {
        if (!amount.matches("\\d+\\.\\d{2}") || new BigDecimal(amount).compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("MAX_AMOUNT must be a positive decimal string with two fractional digits.");
        }
    }

    private static void validateLogLevel(String level) {
        if ("DEBUG".equalsIgnoreCase(level) || "TRACE".equalsIgnoreCase(level)) {
            throw new IllegalStateException("The root production log level cannot be DEBUG or TRACE.");
        }
    }

    private static boolean productionLike(String[] activeProfiles) {
        return Arrays.stream(activeProfiles)
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .anyMatch(profile -> !DEVELOPMENT_PROFILES.contains(profile));
    }

    private static void rejectBootDebugSwitch(Environment environment, String property) {
        String value = environment.getProperty(property);
        if (value != null && !"false".equalsIgnoreCase(value)) {
            throw new IllegalStateException("Production cannot enable Spring Boot " + property + " logging.");
        }
    }

    private static void durationBetween(Duration value, Duration minimum, Duration maximum, String name) {
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalStateException(name + " is outside the documented range.");
        }
    }

    private static boolean wildcard(String value) {
        return "*".equals(value) || "0.0.0.0/0".equals(value) || "::/0".equals(value);
    }
}
