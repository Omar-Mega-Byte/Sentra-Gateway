package com.omar.sentra.gateway.audit;

import java.time.Instant;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive audit persistence.
 */
public interface AuditRepository {
    Mono<Void> insert(AuditEvent event);

    Mono<Void> insertAdminAction(AdminAction action);

    Flux<AuditEvent> search(Instant from, Instant to, String requestId, String routeId, int limit, int offset);

    Mono<AuditEvent> findById(UUID id);

    Flux<AdminAction> findAdminActions(int limit, int offset);
}
