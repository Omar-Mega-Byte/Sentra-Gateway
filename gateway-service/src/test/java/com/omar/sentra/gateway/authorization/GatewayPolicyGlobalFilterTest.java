package com.omar.sentra.gateway.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omar.sentra.gateway.common.request.RequestAttributes;
import com.omar.sentra.gateway.config.SentraProperties;
import com.omar.sentra.gateway.ratelimit.RateLimitDecision;
import com.omar.sentra.gateway.ratelimit.RateLimitService;
import com.omar.sentra.gateway.security.apikey.ApiKeyPrincipal;
import com.omar.sentra.gateway.security.apikey.ApiKeyService;
import com.omar.sentra.gateway.security.ip.IpPolicyService;
import com.omar.sentra.gateway.security.risk.RiskService;
import com.omar.sentra.gateway.security.signing.RequestSigningService;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class GatewayPolicyGlobalFilterTest {
    private static final String API_KEY = "partner-secret";
    private static final UUID CLIENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID KEY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private ApiKeyService apiKeys;
    private RequestSigningService signing;
    private IpPolicyService ipPolicy;
    private RiskService risk;
    private RateLimitService rateLimits;
    private GatewayPolicyGlobalFilter filter;

    @BeforeEach
    void setUp() {
        apiKeys = mock(ApiKeyService.class);
        signing = mock(RequestSigningService.class);
        ipPolicy = mock(IpPolicyService.class);
        risk = mock(RiskService.class);
        rateLimits = mock(RateLimitService.class);
        filter = new GatewayPolicyGlobalFilter(
                apiKeys, signing, ipPolicy, risk, rateLimits, new SentraProperties());

        when(ipPolicy.enforce(nullable(String.class), anyString(), anyString())).thenReturn(Mono.empty());
        when(risk.evaluate(nullable(String.class), anyString(), any(ServerWebExchange.class)))
                .thenReturn(Mono.just("ALLOW"));
        when(rateLimits.consume(nullable(String.class), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(new RateLimitDecision(true, 5, 0)));
    }

    @Test
    void forwardsVerifiedPartnerSignatureEvidenceAndStripsClientSuppliedCredentials() {
        String routeId = "partner-payment-create";
        ApiKeyPrincipal principal = principal(List.of("payments:write"));
        when(apiKeys.verify(API_KEY, routeId)).thenReturn(Mono.just(principal));
        when(signing.verify(any(ServerHttpRequest.class), any(byte[].class), eq(principal))).thenReturn(Mono.empty());

        MockServerWebExchange exchange = exchange(route(routeId, true, List.of("payments:write")), request());
        AtomicReference<ServerHttpRequest> forwarded = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, capture(forwarded))).verifyComplete();

        HttpHeaders headers = forwarded.get().getHeaders();
        assertThat(headers.containsHeader(HttpHeaders.AUTHORIZATION)).isFalse();
        assertThat(headers.containsHeader(HttpHeaders.COOKIE)).isFalse();
        assertThat(headers.containsHeader("X-API-Key")).isFalse();
        assertThat(headers.containsHeader("X-Sentra-Signature")).isFalse();
        assertThat(headers.getFirst("X-Sentra-Client-Id")).isEqualTo(CLIENT_ID.toString());
        assertThat(headers.getFirst("X-Sentra-Key-Id")).isEqualTo(KEY_ID.toString());
        assertThat(headers.getFirst("X-Sentra-Signature-Verified")).isEqualTo("true");
        assertThat(headers.getFirst("X-Sentra-Signature-Key-Id")).isEqualTo(KEY_ID.toString());
        assertThat(headers.getFirst("X-Sentra-Nonce-Status")).isEqualTo("accepted");
        assertThat(headers.getFirst("X-Sentra-Scopes")).isEqualTo("payments:write");
    }

    @Test
    void doesNotForwardSignatureEvidenceWhenPartnerRouteIsUnsigned() {
        String routeId = "partner-payment-read";
        ApiKeyPrincipal principal = principal(List.of("payments:read"));
        when(apiKeys.verify(API_KEY, routeId)).thenReturn(Mono.just(principal));

        MockServerWebExchange exchange = exchange(route(routeId, false, List.of("payments:read")), request());
        AtomicReference<ServerHttpRequest> forwarded = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, capture(forwarded))).verifyComplete();

        HttpHeaders headers = forwarded.get().getHeaders();
        assertThat(headers.getFirst("X-Sentra-Key-Id")).isEqualTo(KEY_ID.toString());
        assertThat(headers.containsHeader("X-Sentra-Signature-Verified")).isFalse();
        assertThat(headers.containsHeader("X-Sentra-Signature-Key-Id")).isFalse();
        assertThat(headers.containsHeader("X-Sentra-Nonce-Status")).isFalse();
        verify(signing, never()).verify(any(), any(), any());
    }

    private MockServerHttpRequest request() {
        return MockServerHttpRequest.post("/api/v1/partner/payments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer leaked")
                .header(HttpHeaders.COOKIE, "session=leaked")
                .header("X-API-Key", API_KEY)
                .header("X-Signature", "external-signature")
                .header("X-Sentra-Key-Id", UUID.randomUUID().toString())
                .header("X-Sentra-Timestamp", "1")
                .header("X-Sentra-Nonce", "client-nonce")
                .header("X-Sentra-Signature", "client-signature")
                .header("X-Sentra-Signature-Verified", "false")
                .header("X-Sentra-Signature-Key-Id", UUID.randomUUID().toString())
                .header("X-Sentra-Nonce-Status", "replayed")
                .build();
    }

    private static MockServerWebExchange exchange(Route route, MockServerHttpRequest request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);
        exchange.getAttributes().put(RequestAttributes.CLIENT_IP, "203.0.113.10");
        return exchange;
    }

    private static Route route(String id, boolean signingRequired, List<String> scopes) {
        return Route.async()
                .id(id)
                .uri(URI.create("http://payment-service:8083"))
                .predicate(exchange -> true)
                .metadata(Map.of(
                        "sentra.category", "PARTNER",
                        "sentra.requiredScopes", scopes,
                        "sentra.signingRequired", signingRequired))
                .build();
    }

    private static GatewayFilterChain capture(AtomicReference<ServerHttpRequest> forwarded) {
        return exchange -> {
            forwarded.set(exchange.getRequest());
            return Mono.empty();
        };
    }

    private static ApiKeyPrincipal principal(List<String> scopes) {
        return new ApiKeyPrincipal(
                CLIENT_ID,
                KEY_ID,
                "partner",
                "tenant-a",
                scopes,
                List.of(),
                API_KEY);
    }
}
