package com.omar.sentra.gateway.security.apikey;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive API client and key persistence.
 */
public interface ApiClientRepository {
    Flux<ApiClient> findClients();

    Mono<ApiClient> findClient(UUID id);

    Mono<ApiClient> insertClient(ApiClient client);

    Mono<ApiClient> updateClient(ApiClient client, long expectedVersion);

    Flux<ApiKeyRecord> findKeysByClient(UUID clientId);

    Mono<ApiKeyRecord> findKey(UUID id);

    Mono<ApiKeyRecord> findActiveKeyByPrefix(String prefix);

    Mono<ApiKeyRecord> insertKey(ApiKeyRecord key);

    Mono<Boolean> revokeKey(UUID id);

    Mono<Void> touchLastUsed(UUID id);
}
