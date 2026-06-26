package com.omar.sentra.gateway.routing;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive durable route repository.
 */
public interface RouteRepository {
    Flux<GatewayRoute> findAll();

    Flux<GatewayRoute> findEnabled();

    Mono<GatewayRoute> findById(String id);

    Mono<GatewayRoute> insert(GatewayRoute route);

    Mono<GatewayRoute> update(GatewayRoute route, long expectedVersion);

    Mono<Boolean> delete(String id);
}
