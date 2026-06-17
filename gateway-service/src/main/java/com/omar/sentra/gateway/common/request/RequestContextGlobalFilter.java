package com.omar.sentra.gateway.common.request;

import com.omar.sentra.gateway.audit.AuditService;
import com.omar.sentra.gateway.config.SentraProperties;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Establishes correlation, removes spoofable headers, and finalizes audit evidence.
 */
@Component
public class RequestContextGlobalFilter implements GlobalFilter, Ordered {
    private static final Set<String> RESERVED_HEADERS = Set.of(
            "x-sentra-request-id",
            "x-sentra-subject",
            "x-sentra-actor-type",
            "x-sentra-tenant-id",
            "x-sentra-roles",
            "x-sentra-scopes",
            "x-sentra-client-id",
            "x-sentra-route-id",
            "x-sentra-source-ip",
            "x-sentra-auth-time",
            "x-sentra-signature-verified",
            "x-sentra-signature-key-id",
            "x-sentra-nonce-status",
            "x-sentra-test-delay-millis",
            "x-sentra-test-status",
            "x-sentra-test-malformed",
            "x-sentra-test-disconnect");

    private final SentraProperties properties;
    private final AuditService auditService;

    public RequestContextGlobalFilter(SentraProperties properties, AuditService auditService) {
        this.properties = properties;
        this.auditService = auditService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = requestId(exchange.getRequest().getHeaders());
        String sourceIp = sourceIp(exchange.getRequest().getRemoteAddress());
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    RESERVED_HEADERS.forEach(headers::remove);
                    headers.set("X-Sentra-Request-Id", requestId);
                })
                .build();
        ServerWebExchange mutated = exchange.mutate().request(request).build();
        mutated.getAttributes().put(RequestAttributes.REQUEST_ID, requestId);
        mutated.getAttributes().put(RequestAttributes.START_NANOS, System.nanoTime());
        mutated.getAttributes().put(RequestAttributes.CLIENT_IP, sourceIp);
        mutated.getResponse().getHeaders().set("X-Request-Id", requestId);
        return chain.filter(mutated).then(Mono.defer(() -> auditService.recordFinal(mutated)));
    }

    @Override
    public int getOrder() {
        return -1000;
    }

    private String requestId(HttpHeaders headers) {
        String supplied = headers.getFirst("X-Request-Id");
        if (supplied == null
                || supplied.isBlank()
                || supplied.length() > properties.getSecurity().getRequestIdMaxLength()
                || !supplied.matches("[A-Za-z0-9._:-]+")) {
            return UUID.randomUUID().toString();
        }
        return supplied;
    }

    private static String sourceIp(InetSocketAddress address) {
        return address == null || address.getAddress() == null
                ? "unknown"
                : address.getAddress().getHostAddress();
    }
}
