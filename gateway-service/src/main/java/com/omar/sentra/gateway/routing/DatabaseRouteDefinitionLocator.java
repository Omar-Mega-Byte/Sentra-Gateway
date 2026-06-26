package com.omar.sentra.gateway.routing;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Supplies Spring Cloud Gateway routes from the durable route repository.
 */
@Component
@DependsOnDatabaseInitialization
public class DatabaseRouteDefinitionLocator implements RouteDefinitionLocator {
    private final RouteRepository repository;

    public DatabaseRouteDefinitionLocator(RouteRepository repository) {
        this.repository = repository;
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return repository.findEnabled().map(this::definition);
    }

    private RouteDefinition definition(GatewayRoute route) {
        RouteDefinition definition = new RouteDefinition();
        definition.setId(route.id());
        definition.setUri(URI.create(route.targetUri()));
        definition.setOrder(route.order());
        definition.setPredicates(java.util.List.of(
                new PredicateDefinition("Path=" + String.join(",", route.pathPatterns())),
                new PredicateDefinition("Method=" + String.join(",", route.methods()))));
        java.util.ArrayList<FilterDefinition> filters = new java.util.ArrayList<>();
        if (route.stripPrefix() > 0) {
            filters.add(new FilterDefinition("StripPrefix=" + route.stripPrefix()));
        }
        if (route.rewriteRegex() != null && route.rewriteReplacement() != null
                && !route.rewriteRegex().isBlank() && !route.rewriteReplacement().isBlank()) {
            FilterDefinition rewrite = new FilterDefinition();
            rewrite.setName("RewritePath");
            rewrite.addArg("regexp", route.rewriteRegex());
            rewrite.addArg("replacement", route.rewriteReplacement());
            filters.add(rewrite);
        }
        if (route.retryEnabled()) {
            FilterDefinition retry = new FilterDefinition();
            retry.setName("Retry");
            retry.addArg("retries", Integer.toString(Math.max(0, route.retryMaxAttempts() - 1)));
            retry.addArg("series", "SERVER_ERROR");
            retry.addArg("methods", String.join(",", route.retryMethods()));
            filters.add(retry);
        }
        if (route.circuitBreakerEnabled()) {
            FilterDefinition circuitBreaker = new FilterDefinition();
            circuitBreaker.setName("CircuitBreaker");
            circuitBreaker.addArg(
                    "name",
                    route.circuitBreakerName() == null || route.circuitBreakerName().isBlank()
                            ? route.id()
                            : route.circuitBreakerName());
            filters.add(circuitBreaker);
        }
        definition.setFilters(filters);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("sentra.category", route.category().name());
        metadata.put("sentra.authentication", route.authentication());
        metadata.put("sentra.requiredRoles", route.requiredRoles());
        metadata.put("sentra.requiredScopes", route.requiredScopes());
        metadata.put("sentra.signingRequired", route.signingRequired());
        put(metadata, "sentra.rateLimitPolicyId", route.rateLimitPolicyId());
        put(metadata, "sentra.ipPolicyId", route.ipPolicyId());
        put(metadata, "sentra.riskPolicyId", route.riskPolicyId());
        metadata.put("connect-timeout", route.connectTimeoutMs());
        metadata.put("response-timeout", (long) route.responseTimeoutMs());
        definition.setMetadata(metadata);
        return definition;
    }

    private static void put(Map<String, Object> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }
}
