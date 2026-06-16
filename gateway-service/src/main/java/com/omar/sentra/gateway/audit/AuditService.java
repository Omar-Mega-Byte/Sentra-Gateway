package com.omar.sentra.gateway.audit;

import com.omar.sentra.gateway.common.request.RequestAttributes;
import com.omar.sentra.gateway.config.SentraProperties;
import java.time.Instant;
import java.util.UUID;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Creates redacted audit events from final gateway decisions.
 */
@Service
public class AuditService {
    private final AuditRepository repository;
    private final SentraProperties properties;

    public AuditService(AuditRepository repository, SentraProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public Mono<Void> recordFinal(ServerWebExchange exchange) {
        HttpStatusCode status = exchange.getResponse().getStatusCode();
        int statusCode = status == null ? 200 : status.value();
        String decision = exchange.getAttributeOrDefault(
                RequestAttributes.DECISION, statusCode < 400 ? "ALLOW" : "DENY");
        String reason = exchange.getAttributeOrDefault(
                RequestAttributes.REASON_CODE, statusCode < 400 ? "GW_ALLOWED" : "GW_REQUEST_FAILED");
        long start = exchange.getAttributeOrDefault(RequestAttributes.START_NANOS, System.nanoTime());
        long latency = Math.max(0, (System.nanoTime() - start) / 1_000_000);
        org.springframework.cloud.gateway.route.Route route =
                exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        return exchange.getPrincipal()
                .cast(Authentication.class)
                .map(authentication -> authentication.getName())
                .defaultIfEmpty("anonymous")
                .flatMap(subject -> repository.insert(new AuditEvent(
                        UUID.randomUUID(),
                        Instant.now(),
                        exchange.getAttributeOrDefault(RequestAttributes.REQUEST_ID, "unknown"),
                        null,
                        statusCode < 400 ? "GATEWAY_REQUEST" : "SECURITY_DECISION",
                        decision,
                        reason,
                        route == null ? null : route.getId(),
                        exchange.getRequest().getMethod().name(),
                        exchange.getRequest().getPath().value(),
                        "anonymous".equals(subject) ? "ANONYMOUS" : "AUTHENTICATED",
                        subject,
                        exchange.getAttributeOrDefault(RequestAttributes.CLIENT_IP, "unknown"),
                        statusCode,
                        latency,
                        properties.getInstanceId(),
                        properties.getEnvironment())))
                .onErrorResume(error -> Mono.empty());
    }

    public Mono<Void> adminAction(
            String actor, String action, String targetType, String targetId, String summary, String requestId) {
        return repository.insertAdminAction(new AdminAction(
                UUID.randomUUID(), Instant.now(), actor, action, targetType, targetId, "SUCCESS", summary, requestId));
    }
}
