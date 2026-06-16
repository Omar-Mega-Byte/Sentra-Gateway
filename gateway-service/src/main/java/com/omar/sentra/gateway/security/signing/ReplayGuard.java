package com.omar.sentra.gateway.security.signing;

import java.time.Duration;
import reactor.core.publisher.Mono;

/**
 * Atomic one-use nonce store.
 */
public interface ReplayGuard {
    Mono<Boolean> claim(String keyId, String nonce, Duration ttl);
}
