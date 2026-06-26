package com.omar.sentra.gateway.security.signing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.omar.sentra.gateway.common.error.GatewayException;
import com.omar.sentra.gateway.config.SentraProperties;
import com.omar.sentra.gateway.security.apikey.ApiKeyPrincipal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class RequestSigningServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");
    private final ReplayGuard replayGuard = (key, nonce, ttl) -> Mono.just(true);
    private final SentraProperties properties = new SentraProperties();
    private final RequestSigningService service =
            new RequestSigningService(replayGuard, properties, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void canonicalizesQueryDeterministically() {
        ServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.POST, URI.create("/api/v1/payments?z=last&a=hello%20world&a=first"))
                .build();

        String canonical = service.canonical(
                request, "{}".getBytes(StandardCharsets.UTF_8), Long.toString(NOW.getEpochSecond()), "nonce", "key");

        assertThat(canonical).contains("\na=first&a=hello%20world&z=last\n");
    }

    @Test
    void acceptsValidSignatureAndClaimsNonce() throws Exception {
        UUID keyId = UUID.randomUUID();
        String apiKey = "sgw_test_abcdef123456_abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";
        byte[] body = "{\"amount\":10}".getBytes(StandardCharsets.UTF_8);
        String timestamp = Long.toString(NOW.getEpochSecond());
        ServerHttpRequest unsigned = MockServerHttpRequest.post("/api/v1/payments?currency=USD")
                .header("X-Sentra-Key-Id", keyId.toString())
                .header("X-Sentra-Timestamp", timestamp)
                .header("X-Sentra-Nonce", "0123456789abcdef")
                .build();
        String signature = hmac(apiKey, service.canonical(
                unsigned, body, timestamp, "0123456789abcdef", keyId.toString()));
        ServerHttpRequest signed = unsigned.mutate().header("X-Sentra-Signature", signature).build();
        ApiKeyPrincipal principal =
                new ApiKeyPrincipal(UUID.randomUUID(), keyId, "partner", null, List.of(), List.of(), apiKey);

        StepVerifier.create(service.verify(signed, body, principal)).verifyComplete();
    }

    @Test
    void rejectsStaleTimestamp() {
        UUID keyId = UUID.randomUUID();
        ServerHttpRequest request = MockServerHttpRequest.post("/api/v1/payments")
                .header("X-Sentra-Key-Id", keyId.toString())
                .header("X-Sentra-Timestamp", "1")
                .header("X-Sentra-Nonce", "0123456789abcdef")
                .header("X-Sentra-Signature", "invalid")
                .build();
        ApiKeyPrincipal principal = new ApiKeyPrincipal(
                UUID.randomUUID(), keyId, "partner", null, List.of(), List.of(), "secret");

        assertThatThrownBy(() -> service.verify(request, new byte[0], principal).block())
                .isInstanceOf(GatewayException.class);
    }

    private static String hmac(String secret, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
