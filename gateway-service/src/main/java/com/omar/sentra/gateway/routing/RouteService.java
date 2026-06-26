package com.omar.sentra.gateway.routing;

import com.omar.sentra.gateway.common.error.ErrorCode;
import com.omar.sentra.gateway.common.error.GatewayException;
import com.omar.sentra.gateway.config.SentraProperties;
import java.net.InetAddress;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Validates, persists, and refreshes dynamic routes.
 */
@Service
public class RouteService {
    private static final List<String> RESERVED_PREFIXES =
            List.of("/api/v1/admin", "/actuator", "/v3/api-docs", "/swagger-ui");
    private static final Set<String> SAFE_RETRY_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final RouteRepository repository;
    private final SentraProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final AtomicLong generation = new AtomicLong();

    public RouteService(
            RouteRepository repository,
            SentraProperties properties,
            ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
    }

    public Flux<GatewayRoute> findAll() {
        return repository.findAll();
    }

    public Mono<GatewayRoute> find(String id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new GatewayException(ErrorCode.GW_RESOURCE_NOT_FOUND)));
    }

    public RouteValidationResult validate(RouteRequest request) {
        List<String> errors = new ArrayList<>();
        validatePaths(request, errors);
        validateTarget(request.targetUri(), errors);
        validateRewrite(request, errors);
        validateSecurity(request, errors);
        validateRetry(request, errors);
        return new RouteValidationResult(errors.isEmpty(), List.copyOf(errors));
    }

    public Mono<GatewayRoute> create(RouteRequest request) {
        RouteValidationResult validation = validate(request);
        if (!validation.valid()) {
            return Mono.error(new GatewayException(
                    ErrorCode.GW_REQUEST_INVALID, String.join("; ", validation.errors())));
        }
        return repository.findById(request.id())
                .flatMap(existing -> Mono.<GatewayRoute>error(new GatewayException(ErrorCode.GW_POLICY_CONFLICT)))
                .switchIfEmpty(Mono.defer(() -> repository.insert(toDomain(request, Instant.now()))))
                .doOnSuccess(ignored -> refresh());
    }

    public Mono<GatewayRoute> update(String id, RouteRequest request) {
        if (!id.equals(request.id())) {
            return Mono.error(new GatewayException(ErrorCode.GW_REQUEST_INVALID, "Path and body route IDs differ."));
        }
        RouteValidationResult validation = validate(request);
        if (!validation.valid()) {
            return Mono.error(new GatewayException(
                    ErrorCode.GW_REQUEST_INVALID, String.join("; ", validation.errors())));
        }
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new GatewayException(ErrorCode.GW_RESOURCE_NOT_FOUND)))
                .flatMap(existing -> repository.update(toDomain(request, existing.createdAt()), request.version()))
                .switchIfEmpty(Mono.error(new GatewayException(ErrorCode.GW_POLICY_CONFLICT)))
                .doOnSuccess(ignored -> refresh());
    }

    public Mono<GatewayRoute> setEnabled(String id, boolean enabled) {
        return find(id).flatMap(existing -> repository.update(new GatewayRoute(
                        existing.id(), existing.category(), existing.pathPatterns(), existing.methods(),
                        existing.targetUri(), existing.stripPrefix(),
                        existing.rewriteRegex(), existing.rewriteReplacement(), existing.order(), enabled,
                        existing.authentication(), existing.requiredRoles(), existing.requiredScopes(),
                        existing.signingRequired(), existing.rateLimitPolicyId(), existing.ipPolicyId(),
                        existing.riskPolicyId(), existing.connectTimeoutMs(), existing.responseTimeoutMs(),
                        existing.retryEnabled(), existing.retryMaxAttempts(), existing.retryMethods(),
                        existing.circuitBreakerEnabled(), existing.circuitBreakerName(), existing.auditMode(),
                        existing.version(), existing.createdAt(), Instant.now()), existing.version()))
                .switchIfEmpty(Mono.error(new GatewayException(ErrorCode.GW_POLICY_CONFLICT)))
                .doOnSuccess(ignored -> refresh());
    }

    public Mono<Void> delete(String id) {
        return repository.delete(id)
                .flatMap(deleted -> deleted
                        ? Mono.<Void>empty()
                        : Mono.error(new GatewayException(ErrorCode.GW_RESOURCE_NOT_FOUND)))
                .doOnSuccess(ignored -> refresh());
    }

    public long generation() {
        return generation.get();
    }

    private GatewayRoute toDomain(RouteRequest request, Instant createdAt) {
        Instant now = Instant.now();
        return new GatewayRoute(
                request.id(), request.category(), List.copyOf(request.pathPatterns()),
                request.methods().stream().map(value -> value.toUpperCase(Locale.ROOT)).toList(),
                request.targetUri(), request.stripPrefix(), request.rewriteRegex(),
                request.rewriteReplacement(), request.order(), request.enabled(),
                List.copyOf(request.authentication()), List.copyOf(request.requiredRoles()),
                List.copyOf(request.requiredScopes()), request.signingRequired(), request.rateLimitPolicyId(),
                request.ipPolicyId(), request.riskPolicyId(), request.connectTimeoutMs(),
                request.responseTimeoutMs(), request.retryPolicy().enabled(), request.retryPolicy().maxAttempts(),
                List.copyOf(request.retryPolicy().eligibleMethods()), request.circuitBreaker().enabled(),
                request.circuitBreaker().name(), request.auditMode(), request.version(), createdAt, now);
    }

    private void validatePaths(RouteRequest request, List<String> errors) {
        for (String path : request.pathPatterns()) {
            if (!path.startsWith("/")) {
                errors.add("Path patterns must start with '/'.");
            }
            if (path.contains("..") || RESERVED_PREFIXES.stream().anyMatch(path::startsWith)) {
                errors.add("Path pattern is reserved or unsafe: " + path);
            }
        }
    }

    private void validateTarget(String target, List<String> errors) {
        try {
            URI uri = URI.create(target);
            if (uri.getUserInfo() != null || uri.getFragment() != null) {
                errors.add("Target URI cannot contain user information or a fragment.");
            }
            if (uri.getScheme() == null
                    || properties.getRouting().getAllowedSchemes().stream()
                            .noneMatch(value -> value.equalsIgnoreCase(uri.getScheme()))) {
                errors.add("Target URI scheme is not allowed.");
            }
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                errors.add("Target URI requires a host.");
            } else if (!properties.getRouting().getAllowedServiceHosts().contains(host)) {
                errors.add("Target host is not allowlisted: " + host);
            } else if (isProhibitedAddress(host) && !"localhost".equals(host)) {
                errors.add("Target resolves to a prohibited address.");
            }
        } catch (IllegalArgumentException exception) {
            errors.add("Target URI is invalid.");
        }
    }

    private void validateRewrite(RouteRequest request, List<String> errors) {
        boolean hasRegex = request.rewriteRegex() != null && !request.rewriteRegex().isBlank();
        boolean hasReplacement = request.rewriteReplacement() != null && !request.rewriteReplacement().isBlank();
        if (hasRegex != hasReplacement) {
            errors.add("Rewrite regex and replacement must be supplied together.");
        }
    }

    private boolean isProhibitedAddress(String host) {
        if (!isIpLiteral(host)) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isMulticastAddress();
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isIpLiteral(String host) {
        return host.contains(":") || host.matches("\\d{1,3}(?:\\.\\d{1,3}){3}");
    }

    private void validateSecurity(RouteRequest request, List<String> errors) {
        if (request.category() != RouteCategory.PUBLIC && request.authentication().contains("NONE")) {
            errors.add("Protected route categories cannot accept NONE authentication.");
        }
        if (request.category() == RouteCategory.PARTNER
                && !request.authentication().contains("API_KEY")) {
            errors.add("Partner routes require API_KEY authentication.");
        }
        if ((request.category() == RouteCategory.USER || request.category() == RouteCategory.ADMIN)
                && !request.authentication().contains("JWT")) {
            errors.add("User and admin routes require JWT authentication.");
        }
        if (request.signingRequired() && !request.authentication().contains("API_KEY")) {
            errors.add("Signed routes require API_KEY authentication.");
        }
    }

    private void validateRetry(RouteRequest request, List<String> errors) {
        if (request.retryPolicy().enabled()
                && request.retryPolicy().eligibleMethods().stream().anyMatch(method -> !SAFE_RETRY_METHODS.contains(method))) {
            errors.add("Retries are limited to GET, HEAD, and OPTIONS.");
        }
    }

    private void refresh() {
        generation.incrementAndGet();
        eventPublisher.publishEvent(new RefreshRoutesEvent(this));
    }
}
