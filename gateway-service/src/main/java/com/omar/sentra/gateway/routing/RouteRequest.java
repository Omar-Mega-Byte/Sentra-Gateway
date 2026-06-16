package com.omar.sentra.gateway.routing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Create or replace dynamic route request.
 */
@Schema(name = "RouteRequest")
public record RouteRequest(
        @NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9-]{1,118}[a-z0-9]") String id,
        @NotNull RouteCategory category,
        @NotEmpty @Size(max = 20) List<@NotBlank String> pathPatterns,
        @NotEmpty @Size(max = 10) List<@Pattern(regexp = "GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS") String> methods,
        @NotBlank String targetUri,
        @Min(0) @Max(20) int stripPrefix,
        @Size(max = 500) String rewriteRegex,
        @Size(max = 500) String rewriteReplacement,
        @Min(-10000) @Max(10000) int order,
        boolean enabled,
        @NotEmpty List<@Pattern(regexp = "JWT|API_KEY|NONE|INTERNAL") String> authentication,
        @Size(max = 50) List<@NotBlank String> requiredRoles,
        @Size(max = 100) List<@NotBlank String> requiredScopes,
        boolean signingRequired,
        String rateLimitPolicyId,
        String ipPolicyId,
        String riskPolicyId,
        @Min(50) @Max(3000) int connectTimeoutMs,
        @Min(100) @Max(10000) int responseTimeoutMs,
        @Valid @NotNull RetryPolicy retryPolicy,
        @Valid @NotNull CircuitBreakerPolicy circuitBreaker,
        @NotBlank String auditMode,
        @Min(0) long version) {

    /**
     * Retry settings carried by a route.
     */
    public record RetryPolicy(
            boolean enabled,
            @Min(1) @Max(2) int maxAttempts,
            @Size(max = 3) List<@Pattern(regexp = "GET|HEAD|OPTIONS") String> eligibleMethods) {
    }

    /**
     * Circuit-breaker settings carried by a route.
     */
    public record CircuitBreakerPolicy(boolean enabled, @Size(max = 120) String name) {
    }
}
