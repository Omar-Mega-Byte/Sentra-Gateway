package com.omar.sentra.gateway.security.signing;

import com.omar.sentra.gateway.common.error.ErrorCode;
import com.omar.sentra.gateway.common.error.GatewayException;
import com.omar.sentra.gateway.config.SentraProperties;
import com.omar.sentra.gateway.security.apikey.ApiKeyPrincipal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Implements Sentra signing protocol version 1.
 */
@Service
public class RequestSigningService {
    private static final HexFormat HEX = HexFormat.of();

    private final ReplayGuard replayGuard;
    private final SentraProperties properties;
    private final Clock clock;

    @Autowired
    public RequestSigningService(ReplayGuard replayGuard, SentraProperties properties) {
        this(replayGuard, properties, Clock.systemUTC());
    }

    RequestSigningService(ReplayGuard replayGuard, SentraProperties properties, Clock clock) {
        this.replayGuard = replayGuard;
        this.properties = properties;
        this.clock = clock;
    }

    public Mono<Void> verify(ServerHttpRequest request, byte[] body, ApiKeyPrincipal principal) {
        HttpHeaders headers = request.getHeaders();
        if (headers.containsHeader(HttpHeaders.CONTENT_ENCODING)) {
            return Mono.error(new GatewayException(ErrorCode.GW_SIGNATURE_INVALID));
        }
        String keyId = required(headers, "X-Sentra-Key-Id");
        String timestampText = required(headers, "X-Sentra-Timestamp");
        String nonce = required(headers, "X-Sentra-Nonce");
        String signature = required(headers, "X-Sentra-Signature");
        if (!principal.keyId().equals(parseUuid(keyId)) || nonce.length() < 16 || nonce.length() > 200) {
            return Mono.error(new GatewayException(ErrorCode.GW_SIGNATURE_INVALID));
        }
        long epoch;
        try {
            epoch = Long.parseLong(timestampText);
        } catch (NumberFormatException exception) {
            return Mono.error(new GatewayException(ErrorCode.GW_SIGNATURE_INVALID));
        }
        Duration difference = Duration.between(Instant.ofEpochSecond(epoch), clock.instant()).abs();
        if (difference.compareTo(properties.getSecurity().getSigning().getTimestampSkew()) > 0) {
            return Mono.error(new GatewayException(ErrorCode.GW_SIGNATURE_INVALID));
        }
        String canonical = canonical(request, body, timestampText, nonce, keyId);
        String expected = hmac(principal.presentedKey(), canonical);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII), signature.getBytes(StandardCharsets.US_ASCII))) {
            return Mono.error(new GatewayException(ErrorCode.GW_SIGNATURE_INVALID));
        }
        return replayGuard.claim(keyId, nonce, properties.getSecurity().getSigning().getNonceTtl())
                .onErrorMap(error -> new GatewayException(ErrorCode.GW_DEPENDENCY_UNAVAILABLE))
                .flatMap(claimed -> claimed
                        ? Mono.empty()
                        : Mono.error(new GatewayException(ErrorCode.GW_REPLAY_DETECTED)));
    }

    public String canonical(
            ServerHttpRequest request, byte[] body, String timestamp, String nonce, String keyId) {
        return String.join(
                "\n",
                request.getMethod().name(),
                normalizePath(request.getURI().getRawPath()),
                canonicalQuery(request.getURI().getRawQuery()),
                sha256(body),
                timestamp,
                nonce,
                keyId);
    }

    private String canonicalQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return "";
        }
        List<QueryPart> parts = new ArrayList<>();
        for (String pair : rawQuery.split("&", -1)) {
            String[] values = pair.split("=", 2);
            String name = encode(decode(values[0]));
            String value = values.length == 2 ? encode(decode(values[1])) : "";
            parts.add(new QueryPart(name, value));
        }
        parts.sort(Comparator.comparing(QueryPart::name).thenComparing(QueryPart::value));
        return String.join("&", parts.stream().map(part -> part.name() + "=" + part.value()).toList());
    }

    private String normalizePath(String rawPath) {
        String value = rawPath == null || rawPath.isBlank() ? "/" : rawPath;
        return java.net.URI.create(value).normalize().getRawPath();
    }

    private String hmac(String secret, String canonical) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot calculate request signature", exception);
        }
    }

    private static String sha256(byte[] body) {
        try {
            return HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(body));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String required(HttpHeaders headers, String name) {
        String value = headers.getFirst(name);
        if (value == null || value.isBlank()) {
            throw new GatewayException(ErrorCode.GW_SIGNATURE_INVALID);
        }
        return value;
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new GatewayException(ErrorCode.GW_SIGNATURE_INVALID);
        }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%7E", "~");
    }

    private record QueryPart(String name, String value) {
    }
}
