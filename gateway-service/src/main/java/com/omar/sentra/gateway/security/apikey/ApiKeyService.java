package com.omar.sentra.gateway.security.apikey;

import com.omar.sentra.gateway.common.error.ErrorCode;
import com.omar.sentra.gateway.common.error.GatewayException;
import com.omar.sentra.gateway.config.SentraProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Generates, verifies, rotates, and revokes API keys.
 */
@Service
public class ApiKeyService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();
    private static final String PREFIX = "sgw_";

    private final ApiClientRepository repository;
    private final SentraProperties properties;
    private final Clock clock;

    @Autowired
    public ApiKeyService(ApiClientRepository repository, SentraProperties properties) {
        this(repository, properties, Clock.systemUTC());
    }

    ApiKeyService(ApiClientRepository repository, SentraProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    public Mono<IssuedApiKey> issue(
            UUID clientId, List<String> scopes, List<String> allowedRoutes, Instant expiresAt, UUID rotatedFrom) {
        return repository.findClient(clientId)
                .switchIfEmpty(Mono.error(new GatewayException(ErrorCode.GW_RESOURCE_NOT_FOUND)))
                .filter(client -> client.status() == ClientStatus.ACTIVE)
                .switchIfEmpty(Mono.error(new GatewayException(ErrorCode.GW_PERMISSION_DENIED)))
                .flatMap(client -> {
                    Instant now = clock.instant();
                    UUID id = UUID.randomUUID();
                    String prefix = randomHex(6);
                    String secret = randomBase64(32);
                    String plaintext = PREFIX + properties.getEnvironment() + "_" + prefix + "_" + secret;
                    ApiKeyRecord record = new ApiKeyRecord(
                            id,
                            clientId,
                            prefix,
                            verifier(plaintext),
                            "v1",
                            List.copyOf(scopes),
                            List.copyOf(allowedRoutes),
                            KeyStatus.ACTIVE,
                            now,
                            expiresAt,
                            rotatedFrom,
                            null,
                            now);
                    return repository.insertKey(record)
                            .map(saved -> new IssuedApiKey(
                                    saved.id(),
                                    plaintext,
                                    saved.prefix(),
                                    saved.createdAt(),
                                    saved.expiresAt(),
                                    "This API key will not be shown again."));
                });
    }

    public Mono<ApiKeyPrincipal> verify(String presentedKey, String routeId) {
        ParsedKey parsed = parse(presentedKey);
        Instant now = clock.instant();
        return repository.findActiveKeyByPrefix(parsed.prefix())
                .filter(key -> MessageDigest.isEqual(
                        key.verifier().getBytes(StandardCharsets.US_ASCII),
                        verifier(presentedKey).getBytes(StandardCharsets.US_ASCII)))
                .filter(key -> !now.isBefore(key.validFrom()))
                .filter(key -> key.expiresAt() == null || now.isBefore(key.expiresAt()))
                .filter(key -> key.allowedRoutes().isEmpty() || key.allowedRoutes().contains(routeId))
                .flatMap(key -> repository.findClient(key.clientId())
                        .filter(client -> client.status() == ClientStatus.ACTIVE)
                        .map(client -> new ApiKeyPrincipal(
                                client.id(),
                                key.id(),
                                client.name(),
                                client.tenantId(),
                                key.scopes(),
                                key.allowedRoutes(),
                                presentedKey))
                        .delayUntil(ignored -> repository.touchLastUsed(key.id())))
                .switchIfEmpty(Mono.error(new GatewayException(ErrorCode.GW_API_KEY_INVALID)));
    }

    public Mono<IssuedApiKey> rotate(UUID keyId, Instant expiresAt) {
        return repository.findKey(keyId)
                .switchIfEmpty(Mono.error(new GatewayException(ErrorCode.GW_RESOURCE_NOT_FOUND)))
                .filter(key -> key.status() == KeyStatus.ACTIVE)
                .switchIfEmpty(Mono.error(new GatewayException(ErrorCode.GW_API_KEY_INVALID)))
                .flatMap(key -> issue(key.clientId(), key.scopes(), key.allowedRoutes(), expiresAt, key.id())
                        .delayUntil(issued -> repository.revokeKey(key.id())
                                .flatMap(revoked -> revoked
                                        ? Mono.empty()
                                        : Mono.error(new GatewayException(ErrorCode.GW_POLICY_CONFLICT)))));
    }

    public Mono<Void> revoke(UUID keyId) {
        return repository.revokeKey(keyId)
                .flatMap(updated -> updated
                        ? Mono.empty()
                        : Mono.error(new GatewayException(ErrorCode.GW_RESOURCE_NOT_FOUND)));
    }

    private ParsedKey parse(String key) {
        if (key == null || !key.startsWith(PREFIX)) {
            throw new GatewayException(ErrorCode.GW_API_KEY_INVALID);
        }
        String[] parts = key.split("_", 4);
        if (parts.length != 4 || parts[2].length() != 12 || parts[3].length() < 40) {
            throw new GatewayException(ErrorCode.GW_API_KEY_INVALID);
        }
        return new ParsedKey(parts[2]);
    }

    private String verifier(String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    properties.getSecurity().getApiKeyPepper().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HEX.formatHex(mac.doFinal(key.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot calculate API-key verifier", exception);
        }
    }

    private static String randomHex(int bytes) {
        byte[] value = new byte[bytes];
        RANDOM.nextBytes(value);
        return HEX.formatHex(value);
    }

    private static String randomBase64(int bytes) {
        byte[] value = new byte[bytes];
        RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private record ParsedKey(String prefix) {
    }
}
