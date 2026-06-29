package com.omar.sentra.payment.idempotency;

import java.util.function.Supplier;

/**
 * Scoped idempotency store for high-risk payment mutations.
 */
public interface IdempotencyStore {
    <T> IdempotentResult<T> execute(
            String operation,
            String routeId,
            String clientId,
            String key,
            String fingerprint,
            Supplier<StoredMutation<T>> mutation);

    void cleanupExpired();

    void reset();

    boolean ready();
}
