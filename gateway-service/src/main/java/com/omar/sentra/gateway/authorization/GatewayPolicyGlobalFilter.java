package com.omar.sentra.gateway.authorization;

import com.omar.sentra.gateway.common.error.ErrorCode;
import com.omar.sentra.gateway.common.error.GatewayException;
import com.omar.sentra.gateway.common.request.RequestAttributes;
import com.omar.sentra.gateway.config.SentraProperties;
import com.omar.sentra.gateway.ratelimit.RateLimitDecision;
import com.omar.sentra.gateway.ratelimit.RateLimitService;
import com.omar.sentra.gateway.security.apikey.ApiKeyPrincipal;
import com.omar.sentra.gateway.security.apikey.ApiKeyService;
import com.omar.sentra.gateway.security.ip.IpPolicyService;
import com.omar.sentra.gateway.security.risk.RiskService;
import com.omar.sentra.gateway.security.signing.RequestSigningService;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Enforces route metadata security before the downstream call.
 */
@Component
public class GatewayPolicyGlobalFilter implements GlobalFilter, Ordered {
    private final ApiKeyService apiKeys;
    private final RequestSigningService signing;
    private final IpPolicyService ipPolicy;
    private final RiskService risk;
    private final RateLimitService rateLimits;
    private final SentraProperties properties;

    public GatewayPolicyGlobalFilter(
            ApiKeyService apiKeys,
            RequestSigningService signing,
            IpPolicyService ipPolicy,
            RiskService risk,
            RateLimitService rateLimits,
            SentraProperties properties) {
        this.apiKeys = apiKeys;
        this.signing = signing;
        this.ipPolicy = ipPolicy;
        this.risk = risk;
        this.rateLimits = rateLimits;
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route == null) {
            return chain.filter(exchange);
        }
        Map<String, Object> metadata = route.getMetadata();
        String category = string(metadata, "sentra.category", "PUBLIC");
        String routeId = route.getId();
        String clientIp = exchange.getAttributeOrDefault(RequestAttributes.CLIENT_IP, "unknown");
        return ipPolicy.enforce(string(metadata, "sentra.ipPolicyId", null), routeId, clientIp)
                .then(authenticate(exchange, metadata, routeId, category))
                .flatMap(identity -> body(exchange, bool(metadata, "sentra.signingRequired"))
                        .flatMap(body -> verifySigning(exchange, metadata, identity.apiKey(), body)
                                .then(risk.evaluate(string(metadata, "sentra.riskPolicyId", null), routeId, exchange))
                                .flatMap(riskAction -> rateLimits.consume(
                                        string(metadata, "sentra.rateLimitPolicyId", null),
                                        identity.subject(),
                                        routeId,
                                        exchange.getRequest().getMethod().name()))
                                .flatMap(decision -> forward(exchange, chain, routeId, identity, body, decision))));
    }

    @Override
    public int getOrder() {
        return -500;
    }

    private Mono<Identity> authenticate(
            ServerWebExchange exchange, Map<String, Object> metadata, String routeId, String category) {
        if ("PARTNER".equals(category)) {
            String key = exchange.getRequest().getHeaders().getFirst("X-API-Key");
            return apiKeys.verify(key, routeId)
                    .map(principal -> {
                        requireScopes(principal.scopes(), list(metadata, "sentra.requiredScopes"));
                        return new Identity(
                                principal.clientId().toString(),
                                "API_CLIENT",
                                principal.tenantId(),
                                Set.of(),
                                Set.copyOf(principal.scopes()),
                                principal);
                    });
        }
        if ("PUBLIC".equals(category)) {
            return Mono.just(new Identity(
                    "ip:" + exchange.getAttributeOrDefault(RequestAttributes.CLIENT_IP, "unknown"),
                    "ANONYMOUS",
                    null,
                    Set.of(),
                    Set.of(),
                    null));
        }
        return exchange.getPrincipal()
                .cast(Authentication.class)
                .filter(Authentication::isAuthenticated)
                .switchIfEmpty(Mono.error(new GatewayException(ErrorCode.GW_AUTH_REQUIRED)))
                .map(authentication -> {
                    Set<String> authorities = authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toSet());
                    Set<String> roles = authorities.stream()
                            .filter(value -> value.startsWith("ROLE_"))
                            .map(value -> value.substring(5))
                            .collect(Collectors.toSet());
                    Set<String> scopes = authorities.stream()
                            .filter(value -> value.startsWith("SCOPE_"))
                            .map(value -> value.substring(6))
                            .collect(Collectors.toSet());
                    requireRoles(roles, list(metadata, "sentra.requiredRoles"));
                    requireScopes(scopes, list(metadata, "sentra.requiredScopes"));
                    return new Identity(authentication.getName(), "USER", tenantId(authentication), roles, scopes, null);
                });
    }

    private Mono<byte[]> body(ServerWebExchange exchange, boolean signingRequired) {
        if (!signingRequired) {
            return Mono.just(new byte[0]);
        }
        long contentLength = exchange.getRequest().getHeaders().getContentLength();
        if (contentLength > properties.getLimits().getMaxSignedBodyBytes()) {
            return Mono.error(new GatewayException(ErrorCode.GW_BODY_TOO_LARGE));
        }
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .map(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);
                    if (bytes.length > properties.getLimits().getMaxSignedBodyBytes()) {
                        throw new GatewayException(ErrorCode.GW_BODY_TOO_LARGE);
                    }
                    return bytes;
                })
                .defaultIfEmpty(new byte[0]);
    }

    private Mono<Void> verifySigning(
            ServerWebExchange exchange, Map<String, Object> metadata, ApiKeyPrincipal principal, byte[] body) {
        if (!bool(metadata, "sentra.signingRequired")) {
            return Mono.empty();
        }
        if (principal == null) {
            return Mono.error(new GatewayException(ErrorCode.GW_SIGNATURE_INVALID));
        }
        return signing.verify(exchange.getRequest(), body, principal);
    }

    private Mono<Void> forward(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            String routeId,
            Identity identity,
            byte[] body,
            RateLimitDecision decision) {
        ServerHttpRequest request = decorate(exchange, routeId, identity, body);
        if (decision.remaining() != Long.MAX_VALUE) {
            exchange.getResponse().getHeaders().set("RateLimit-Remaining", Long.toString(decision.remaining()));
        }
        return chain.filter(exchange.mutate().request(request).build());
    }

    private ServerHttpRequest decorate(
            ServerWebExchange exchange, String routeId, Identity identity, byte[] body) {
        ServerHttpRequest base = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove("X-API-Key");
                    headers.remove("X-Sentra-Key-Id");
                    headers.remove("X-Sentra-Timestamp");
                    headers.remove("X-Sentra-Nonce");
                    headers.remove("X-Sentra-Signature");
                    headers.set("X-Sentra-Subject", identity.subject());
                    headers.set("X-Sentra-Actor-Type", identity.actorType());
                    headers.set("X-Sentra-Route-Id", routeId);
                    headers.set("X-Sentra-Source-Ip",
                            exchange.getAttributeOrDefault(RequestAttributes.CLIENT_IP, "unknown"));
                    set(headers, "X-Sentra-Tenant-Id", identity.tenantId());
                    set(headers, "X-Sentra-Roles", encodeHeaderList(identity.roles()));
                    set(headers, "X-Sentra-Scopes", encodeHeaderList(identity.scopes()));
                    if (identity.apiKey() != null) {
                        headers.set("X-Sentra-Client-Id", identity.apiKey().clientId().toString());
                    }
                })
                .build();
        if (body.length == 0) {
            return base;
        }
        return new ServerHttpRequestDecorator(base) {
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(super.getHeaders());
                headers.setContentLength(body.length);
                return headers;
            }

            @Override
            public Flux<DataBuffer> getBody() {
                return Flux.defer(() -> Flux.just(exchange.getResponse().bufferFactory().wrap(body)));
            }
        };
    }

    private static void requireRoles(Collection<String> actual, List<String> required) {
        if (!actual.containsAll(required)) {
            throw new GatewayException(ErrorCode.GW_PERMISSION_DENIED);
        }
    }

    private static void requireScopes(Collection<String> actual, List<String> required) {
        if (!actual.containsAll(required)) {
            throw new GatewayException(ErrorCode.GW_PERMISSION_DENIED);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> list(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    private static String string(Map<String, Object> metadata, String key, String defaultValue) {
        Object value = metadata.get(key);
        return value == null ? defaultValue : value.toString();
    }

    private static boolean bool(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(value));
    }

    private static void set(HttpHeaders headers, String name, String value) {
        if (value != null && !value.isBlank()) {
            headers.set(name, value);
        }
    }

    private static String tenantId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwt) {
            Object tenant = jwt.getTokenAttributes().getOrDefault(
                    "tenant_id",
                    jwt.getTokenAttributes().get("tenant"));
            return tenant == null ? null : tenant.toString();
        }
        return null;
    }

    private static String encodeHeaderList(Collection<String> values) {
        return values.stream()
                .sorted()
                .map(value -> value.replace("\\", "\\\\").replace(",", "\\,"))
                .collect(Collectors.joining(","));
    }

    private record Identity(
            String subject,
            String actorType,
            String tenantId,
            Set<String> roles,
            Set<String> scopes,
            ApiKeyPrincipal apiKey) {
    }
}
