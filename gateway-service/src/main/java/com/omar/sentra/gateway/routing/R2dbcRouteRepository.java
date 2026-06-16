package com.omar.sentra.gateway.routing;

import static com.omar.sentra.gateway.common.util.DbValues.instant;
import static com.omar.sentra.gateway.common.util.TextListCodec.decode;
import static com.omar.sentra.gateway.common.util.TextListCodec.encode;

import io.r2dbc.spi.Readable;
import java.time.Instant;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * SQL implementation of the dynamic route repository.
 */
@Repository
public class R2dbcRouteRepository implements RouteRepository {
    private static final String SELECT_COLUMNS = """
            SELECT id, category, path_patterns, methods, target_uri, strip_prefix,
                   rewrite_regex, rewrite_replacement, route_order, enabled,
                   authentication_types, required_roles, required_scopes, signing_required,
                   rate_limit_policy_id, ip_policy_id, risk_policy_id, connect_timeout_ms,
                   response_timeout_ms, retry_enabled, retry_max_attempts, retry_methods,
                   circuit_breaker_enabled, circuit_breaker_name, audit_mode, version, created_at, updated_at
              FROM gateway_routes
            """;

    private final DatabaseClient databaseClient;

    public R2dbcRouteRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Flux<GatewayRoute> findAll() {
        return databaseClient.sql(SELECT_COLUMNS + " ORDER BY route_order, id").map(this::map).all();
    }

    @Override
    public Flux<GatewayRoute> findEnabled() {
        return databaseClient.sql(SELECT_COLUMNS + " WHERE enabled = TRUE ORDER BY route_order, id")
                .map(this::map)
                .all();
    }

    @Override
    public Mono<GatewayRoute> findById(String id) {
        return databaseClient.sql(SELECT_COLUMNS + " WHERE id = :id")
                .bind("id", id)
                .map(this::map)
                .one();
    }

    @Override
    public Mono<GatewayRoute> insert(GatewayRoute route) {
        return bindRoute(databaseClient.sql("""
                INSERT INTO gateway_routes (
                    id, category, path_patterns, methods, target_uri, strip_prefix,
                    rewrite_regex, rewrite_replacement, route_order, enabled,
                    authentication_types, required_roles, required_scopes, signing_required,
                    rate_limit_policy_id, ip_policy_id, risk_policy_id, connect_timeout_ms,
                    response_timeout_ms, retry_enabled, retry_max_attempts, retry_methods,
                    circuit_breaker_enabled, circuit_breaker_name, audit_mode, version, created_at, updated_at
                ) VALUES (
                    :id, :category, :paths, :methods, :target, :stripPrefix,
                    :rewriteRegex, :rewriteReplacement, :routeOrder, :enabled,
                    :authentication, :roles, :scopes, :signingRequired, :ratePolicy, :ipPolicy, :riskPolicy,
                    :connectTimeout, :responseTimeout, :retryEnabled, :retryAttempts, :retryMethods,
                    :circuitEnabled, :circuitName, :auditMode, 1, :createdAt, :updatedAt
                )
                """), route)
                .fetch()
                .rowsUpdated()
                .then(findById(route.id()));
    }

    @Override
    public Mono<GatewayRoute> update(GatewayRoute route, long expectedVersion) {
        return bindRoute(databaseClient.sql("""
                UPDATE gateway_routes SET
                    category=:category, path_patterns=:paths, methods=:methods, target_uri=:target,
                    strip_prefix=:stripPrefix, rewrite_regex=:rewriteRegex,
                    rewrite_replacement=:rewriteReplacement, route_order=:routeOrder, enabled=:enabled,
                    authentication_types=:authentication, required_roles=:roles, required_scopes=:scopes,
                    signing_required=:signingRequired, rate_limit_policy_id=:ratePolicy,
                    ip_policy_id=:ipPolicy, risk_policy_id=:riskPolicy, connect_timeout_ms=:connectTimeout,
                    response_timeout_ms=:responseTimeout, retry_enabled=:retryEnabled,
                    retry_max_attempts=:retryAttempts, retry_methods=:retryMethods,
                    circuit_breaker_enabled=:circuitEnabled, circuit_breaker_name=:circuitName,
                    audit_mode=:auditMode, version=version+1, created_at=:createdAt, updated_at=:updatedAt
                WHERE id=:id AND version=:expectedVersion
                """), route)
                .bind("expectedVersion", expectedVersion)
                .fetch()
                .rowsUpdated()
                .flatMap(updated -> updated == 1 ? findById(route.id()) : Mono.empty());
    }

    @Override
    public Mono<Boolean> delete(String id) {
        return databaseClient.sql("DELETE FROM gateway_routes WHERE id=:id")
                .bind("id", id)
                .fetch()
                .rowsUpdated()
                .map(count -> count == 1);
    }

    private DatabaseClient.GenericExecuteSpec bindRoute(DatabaseClient.GenericExecuteSpec spec, GatewayRoute route) {
        return spec.bind("id", route.id())
                .bind("category", route.category().name())
                .bind("paths", encode(route.pathPatterns()))
                .bind("methods", encode(route.methods()))
                .bind("target", route.targetUri())
                .bind("stripPrefix", route.stripPrefix())
                .bind("rewriteRegex", nullable(route.rewriteRegex(), String.class))
                .bind("rewriteReplacement", nullable(route.rewriteReplacement(), String.class))
                .bind("routeOrder", route.order())
                .bind("enabled", route.enabled())
                .bind("authentication", encode(route.authentication()))
                .bind("roles", encode(route.requiredRoles()))
                .bind("scopes", encode(route.requiredScopes()))
                .bind("signingRequired", route.signingRequired())
                .bind("ratePolicy", nullable(route.rateLimitPolicyId(), String.class))
                .bind("ipPolicy", nullable(route.ipPolicyId(), String.class))
                .bind("riskPolicy", nullable(route.riskPolicyId(), String.class))
                .bind("connectTimeout", route.connectTimeoutMs())
                .bind("responseTimeout", route.responseTimeoutMs())
                .bind("retryEnabled", route.retryEnabled())
                .bind("retryAttempts", route.retryMaxAttempts())
                .bind("retryMethods", encode(route.retryMethods()))
                .bind("circuitEnabled", route.circuitBreakerEnabled())
                .bind("circuitName", nullable(route.circuitBreakerName(), String.class))
                .bind("auditMode", route.auditMode())
                .bind("createdAt", route.createdAt())
                .bind("updatedAt", route.updatedAt());
    }

    private GatewayRoute map(Readable row) {
        return new GatewayRoute(
                row.get("id", String.class),
                RouteCategory.valueOf(row.get("category", String.class)),
                decode(row.get("path_patterns", String.class)),
                decode(row.get("methods", String.class)),
                row.get("target_uri", String.class),
                required(row, "strip_prefix", Integer.class),
                row.get("rewrite_regex", String.class),
                row.get("rewrite_replacement", String.class),
                required(row, "route_order", Integer.class),
                required(row, "enabled", Boolean.class),
                decode(row.get("authentication_types", String.class)),
                decode(row.get("required_roles", String.class)),
                decode(row.get("required_scopes", String.class)),
                required(row, "signing_required", Boolean.class),
                row.get("rate_limit_policy_id", String.class),
                row.get("ip_policy_id", String.class),
                row.get("risk_policy_id", String.class),
                required(row, "connect_timeout_ms", Integer.class),
                required(row, "response_timeout_ms", Integer.class),
                required(row, "retry_enabled", Boolean.class),
                required(row, "retry_max_attempts", Integer.class),
                decode(row.get("retry_methods", String.class)),
                required(row, "circuit_breaker_enabled", Boolean.class),
                row.get("circuit_breaker_name", String.class),
                row.get("audit_mode", String.class),
                required(row, "version", Long.class),
                instant(row, "created_at"),
                instant(row, "updated_at"));
    }

    private static <T> T required(Readable row, String column, Class<T> type) {
        T value = row.get(column, type);
        if (value == null) {
            throw new IllegalStateException("Database column is null: " + column);
        }
        return value;
    }

    private static <T> org.springframework.r2dbc.core.Parameter nullable(T value, Class<T> type) {
        return value == null
                ? org.springframework.r2dbc.core.Parameter.empty(type)
                : org.springframework.r2dbc.core.Parameter.from(value);
    }
}
