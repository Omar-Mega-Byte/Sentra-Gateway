package com.omar.sentra.gateway.common.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.omar.sentra.gateway.audit.AuditService;
import com.omar.sentra.gateway.config.SentraProperties;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class RequestContextGlobalFilterTest {
    @Test
    void stripsSpoofableSentraAndFaultInjectionHeadersButPreservesClientSigningKeyId() {
        AuditService auditService = mock(AuditService.class);
        when(auditService.recordFinal(any(ServerWebExchange.class))).thenReturn(Mono.empty());
        RequestContextGlobalFilter filter = new RequestContextGlobalFilter(new SentraProperties(), auditService);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/notifications")
                .header("X-Request-Id", "request-123")
                .header("X-Sentra-Subject", "spoofed")
                .header("X-Sentra-Key-Id", "spoofed-key")
                .header("X-Sentra-Signature-Verified", "true")
                .header("X-Sentra-Signature-Key-Id", "spoofed-signature-key")
                .header("X-Sentra-Nonce-Status", "accepted")
                .header("X-Sentra-Test-Status", "500")
                .header("X-Sentra-Test-Delay-Millis", "10000")
                .build());
        AtomicReference<ServerHttpRequest> forwarded = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, capture(forwarded))).verifyComplete();

        HttpHeaders headers = forwarded.get().getHeaders();
        assertThat(headers.getFirst("X-Sentra-Request-Id")).isEqualTo("request-123");
        assertThat(headers.containsHeader("X-Sentra-Subject")).isFalse();
        assertThat(headers.getFirst("X-Sentra-Key-Id")).isEqualTo("spoofed-key");
        assertThat(headers.containsHeader("X-Sentra-Signature-Verified")).isFalse();
        assertThat(headers.containsHeader("X-Sentra-Signature-Key-Id")).isFalse();
        assertThat(headers.containsHeader("X-Sentra-Nonce-Status")).isFalse();
        assertThat(headers.containsHeader("X-Sentra-Test-Status")).isFalse();
        assertThat(headers.containsHeader("X-Sentra-Test-Delay-Millis")).isFalse();
    }

    private static GatewayFilterChain capture(AtomicReference<ServerHttpRequest> forwarded) {
        return exchange -> {
            forwarded.set(exchange.getRequest());
            return Mono.empty();
        };
    }
}
