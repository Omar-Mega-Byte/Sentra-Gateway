package com.omar.sentra.gateway.security.apikey;

import static org.assertj.core.api.Assertions.assertThat;

import com.omar.sentra.gateway.config.SentraProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ApiKeyServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");
    private FakeRepository repository;
    private ApiKeyService service;
    private UUID clientId;

    @BeforeEach
    void setUp() {
        repository = new FakeRepository();
        SentraProperties properties = new SentraProperties();
        properties.setEnvironment("test");
        properties.getSecurity().setApiKeyPepper("test-pepper-that-is-at-least-thirty-two-bytes");
        service = new ApiKeyService(repository, properties, Clock.fixed(NOW, ZoneOffset.UTC));
        clientId = UUID.randomUUID();
        repository.clients.put(clientId, new ApiClient(
                clientId, "partner", "owner", "tenant", ClientStatus.ACTIVE, 1, NOW, NOW));
    }

    @Test
    void issuesPlaintextOnceAndVerifiesIt() {
        IssuedApiKey issued = service.issue(
                        clientId, List.of("payments:write"), List.of("payment-create"),
                        NOW.plusSeconds(3600), null)
                .block();

        assertThat(issued).isNotNull();
        assertThat(issued.apiKey()).startsWith("sgw_test_");
        ApiKeyRecord stored = repository.keys.get(issued.keyId());
        assertThat(stored.verifier()).doesNotContain(issued.apiKey());

        StepVerifier.create(service.verify(issued.apiKey(), "payment-create"))
                .assertNext(principal -> {
                    assertThat(principal.clientId()).isEqualTo(clientId);
                    assertThat(principal.scopes()).contains("payments:write");
                })
                .verifyComplete();
    }

    @Test
    void revocationStopsVerification() {
        IssuedApiKey issued = service.issue(clientId, List.of(), List.of(), NOW.plusSeconds(60), null).block();
        service.revoke(issued.keyId()).block();
        StepVerifier.create(service.verify(issued.apiKey(), "route"))
                .expectError()
                .verify();
    }

    @Test
    void rotationCreatesSuccessorAndRevokesPredecessor() {
        IssuedApiKey original =
                service.issue(clientId, List.of("payments:write"), List.of("payment-create"), null, null).block();

        IssuedApiKey successor = service.rotate(original.keyId(), NOW.plusSeconds(3600)).block();

        assertThat(successor).isNotNull();
        assertThat(repository.keys.get(original.keyId()).status()).isEqualTo(KeyStatus.REVOKED);
        assertThat(repository.keys.get(successor.keyId()).rotatedFrom()).isEqualTo(original.keyId());
        StepVerifier.create(service.verify(original.apiKey(), "payment-create")).expectError().verify();
        StepVerifier.create(service.verify(successor.apiKey(), "payment-create")).expectNextCount(1).verifyComplete();
    }

    private static final class FakeRepository implements ApiClientRepository {
        private final Map<UUID, ApiClient> clients = new LinkedHashMap<>();
        private final Map<UUID, ApiKeyRecord> keys = new LinkedHashMap<>();

        @Override
        public Flux<ApiClient> findClients() {
            return Flux.fromIterable(clients.values());
        }

        @Override
        public Mono<ApiClient> findClient(UUID id) {
            return Mono.justOrEmpty(clients.get(id));
        }

        @Override
        public Mono<ApiClient> insertClient(ApiClient client) {
            clients.put(client.id(), client);
            return Mono.just(client);
        }

        @Override
        public Mono<ApiClient> updateClient(ApiClient client, long expectedVersion) {
            clients.put(client.id(), client);
            return Mono.just(client);
        }

        @Override
        public Flux<ApiKeyRecord> findKeysByClient(UUID clientId) {
            return Flux.fromIterable(keys.values()).filter(key -> key.clientId().equals(clientId));
        }

        @Override
        public Mono<ApiKeyRecord> findKey(UUID id) {
            return Mono.justOrEmpty(keys.get(id));
        }

        @Override
        public Mono<ApiKeyRecord> findActiveKeyByPrefix(String prefix) {
            return Flux.fromIterable(keys.values())
                    .filter(key -> key.prefix().equals(prefix) && key.status() == KeyStatus.ACTIVE)
                    .next();
        }

        @Override
        public Mono<ApiKeyRecord> insertKey(ApiKeyRecord key) {
            keys.put(key.id(), key);
            return Mono.just(key);
        }

        @Override
        public Mono<Boolean> revokeKey(UUID id) {
            ApiKeyRecord key = keys.get(id);
            if (key == null || key.status() != KeyStatus.ACTIVE) {
                return Mono.just(false);
            }
            keys.put(id, new ApiKeyRecord(
                    key.id(), key.clientId(), key.prefix(), key.verifier(), key.pepperVersion(), key.scopes(),
                    key.allowedRoutes(), KeyStatus.REVOKED, key.validFrom(), key.expiresAt(), key.rotatedFrom(),
                    key.lastUsedAt(), key.createdAt()));
            return Mono.just(true);
        }

        @Override
        public Mono<Void> touchLastUsed(UUID id) {
            return Mono.empty();
        }
    }
}
