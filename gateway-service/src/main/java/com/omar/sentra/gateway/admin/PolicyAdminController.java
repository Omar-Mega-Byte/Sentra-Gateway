package com.omar.sentra.gateway.admin;

import com.omar.sentra.gateway.common.error.ErrorCode;
import com.omar.sentra.gateway.common.error.GatewayException;
import com.omar.sentra.gateway.ratelimit.RateLimitPolicy;
import com.omar.sentra.gateway.security.ip.IpRule;
import com.omar.sentra.gateway.security.risk.RiskRule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Rate-limit, IP, and risk policy administration.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Security Policies")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "basicAuth")
public class PolicyAdminController {
    private final PolicyRepository repository;

    public PolicyAdminController(PolicyRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/rate-limits")
    @Operation(summary = "List rate-limit policies")
    public Flux<RateLimitPolicy> rateLimits() {
        return repository.findRateLimits();
    }

    @GetMapping("/rate-limits/{id}")
    public Mono<RateLimitPolicy> rateLimit(@PathVariable String id) {
        return repository.findRateLimit(id).switchIfEmpty(notFound());
    }

    @PostMapping("/rate-limits")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RateLimitPolicy> createRate(@Valid @RequestBody RateLimitRequest request) {
        return repository.saveRateLimit(request.toDomain(Instant.now()), true);
    }

    @PutMapping("/rate-limits/{id}")
    public Mono<RateLimitPolicy> updateRate(
            @PathVariable String id, @Valid @RequestBody RateLimitRequest request) {
        return repository.saveRateLimit(request.withId(id).toDomain(Instant.EPOCH), false)
                .switchIfEmpty(conflict());
    }

    @DeleteMapping("/rate-limits/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteRate(@PathVariable String id) {
        return repository.deleteRateLimit(id).flatMap(deleted -> deleted ? Mono.empty() : notFound());
    }

    @GetMapping("/ip-rules")
    public Flux<IpRule> ipRules() {
        return repository.findIpRules();
    }

    @GetMapping("/ip-rules/{id}")
    public Mono<IpRule> ipRule(@PathVariable String id) {
        return repository.findIpRule(id).switchIfEmpty(notFound());
    }

    @PostMapping("/ip-rules")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<IpRule> createIp(@Valid @RequestBody IpRuleRequest request) {
        return repository.saveIpRule(request.toDomain(Instant.now()), true);
    }

    @PutMapping("/ip-rules/{id}")
    public Mono<IpRule> updateIp(@PathVariable String id, @Valid @RequestBody IpRuleRequest request) {
        return repository.saveIpRule(request.withId(id).toDomain(Instant.EPOCH), false)
                .switchIfEmpty(conflict());
    }

    @DeleteMapping("/ip-rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteIp(@PathVariable String id) {
        return repository.deleteIpRule(id).flatMap(deleted -> deleted ? Mono.empty() : notFound());
    }

    @GetMapping("/risk-rules")
    public Flux<RiskRule> riskRules() {
        return repository.findRiskRules();
    }

    @GetMapping("/risk-rules/{id}")
    public Mono<RiskRule> riskRule(@PathVariable String id) {
        return repository.findRiskRule(id).switchIfEmpty(notFound());
    }

    @PostMapping("/risk-rules")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RiskRule> createRisk(@Valid @RequestBody RiskRuleRequest request) {
        return repository.saveRiskRule(request.toDomain(Instant.now()), true);
    }

    @PutMapping("/risk-rules/{id}")
    public Mono<RiskRule> updateRisk(
            @PathVariable String id, @Valid @RequestBody RiskRuleRequest request) {
        return repository.saveRiskRule(request.withId(id).toDomain(Instant.EPOCH), false)
                .switchIfEmpty(conflict());
    }

    @DeleteMapping("/risk-rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteRisk(@PathVariable String id) {
        return repository.deleteRiskRule(id).flatMap(deleted -> deleted ? Mono.empty() : notFound());
    }

    private static <T> Mono<T> notFound() {
        return Mono.error(new GatewayException(ErrorCode.GW_RESOURCE_NOT_FOUND));
    }

    private static <T> Mono<T> conflict() {
        return Mono.error(new GatewayException(ErrorCode.GW_POLICY_CONFLICT));
    }

    /**
     * Rate-limit policy body.
     */
    public record RateLimitRequest(
            @NotBlank String id,
            @NotBlank String subjectType,
            String routeId,
            @Pattern(regexp = "GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS") String method,
            @Min(1) int capacity,
            @Min(1) int refillTokens,
            @Min(1) int refillPeriodSeconds,
            int priority,
            @Pattern(regexp = "DENY|ALLOW|LOCAL_FALLBACK") String redisOutageMode,
            boolean responseHeadersEnabled,
            boolean enabled,
            @Min(0) long version) {
        RateLimitRequest withId(String value) {
            return new RateLimitRequest(value, subjectType, routeId, method, capacity, refillTokens,
                    refillPeriodSeconds, priority, redisOutageMode, responseHeadersEnabled, enabled, version);
        }

        RateLimitPolicy toDomain(Instant createdAt) {
            return new RateLimitPolicy(id, subjectType, routeId, method, capacity, refillTokens,
                    refillPeriodSeconds, priority, redisOutageMode, responseHeadersEnabled, enabled,
                    version, createdAt, Instant.now());
        }
    }

    /**
     * IP rule body.
     */
    public record IpRuleRequest(
            @NotBlank String id,
            @NotBlank String network,
            @Pattern(regexp = "ALLOW|BLOCK|TEMP_BLOCK") String action,
            String routeId,
            int priority,
            @NotBlank String reason,
            @NotNull Instant validFrom,
            Instant expiresAt,
            boolean enabled,
            @Min(0) long version) {
        IpRuleRequest withId(String value) {
            return new IpRuleRequest(value, network, action, routeId, priority, reason, validFrom,
                    expiresAt, enabled, version);
        }

        IpRule toDomain(Instant createdAt) {
            return new IpRule(id, network, action, routeId, priority, reason, validFrom, expiresAt,
                    enabled, version, createdAt, Instant.now());
        }
    }

    /**
     * Risk rule body.
     */
    public record RiskRuleRequest(
            @NotBlank String id,
            @Pattern(regexp = "HEADER_COUNT|QUERY_PARAMETER_COUNT|PATH_SEGMENTS") String signal,
            @Min(1) int thresholdValue,
            @Min(1) @Max(100) int weight,
            @Pattern(regexp = "OBSERVE|THROTTLE|TEMP_BLOCK|DENY") String action,
            String routeId,
            boolean enabled,
            @Min(0) long version) {
        RiskRuleRequest withId(String value) {
            return new RiskRuleRequest(value, signal, thresholdValue, weight, action, routeId, enabled, version);
        }

        RiskRule toDomain(Instant createdAt) {
            return new RiskRule(id, signal, thresholdValue, weight, action, routeId, enabled,
                    version, createdAt, Instant.now());
        }
    }
}
