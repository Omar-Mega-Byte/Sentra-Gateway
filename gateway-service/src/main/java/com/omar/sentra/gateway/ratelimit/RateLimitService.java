package com.omar.sentra.gateway.ratelimit;

import com.omar.sentra.gateway.admin.PolicyRepository;
import com.omar.sentra.gateway.common.error.ErrorCode;
import com.omar.sentra.gateway.common.error.GatewayException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Loads and applies a route-selected distributed rate-limit policy.
 */
@Service
public class RateLimitService {
    private final PolicyRepository repository;
    private final RedisTokenBucket tokenBucket;

    public RateLimitService(PolicyRepository repository, RedisTokenBucket tokenBucket) {
        this.repository = repository;
        this.tokenBucket = tokenBucket;
    }

    public Mono<RateLimitDecision> consume(String policyId, String subject, String routeId, String method) {
        if (policyId == null) {
            return Mono.just(new RateLimitDecision(true, Long.MAX_VALUE, 0));
        }
        return repository.findRateLimit(policyId)
                .filter(RateLimitPolicy::enabled)
                .filter(policy -> policy.routeId() == null || policy.routeId().equals(routeId))
                .filter(policy -> policy.method() == null || policy.method().equals(method))
                .flatMap(policy -> tokenBucket.consume(policy.id() + ":" + subject + ":" + routeId, policy)
                        .onErrorResume(error -> outage(policy)))
                .defaultIfEmpty(new RateLimitDecision(true, Long.MAX_VALUE, 0))
                .flatMap(decision -> decision.allowed()
                        ? Mono.just(decision)
                        : Mono.error(new GatewayException(ErrorCode.GW_RATE_LIMITED)));
    }

    private Mono<RateLimitDecision> outage(RateLimitPolicy policy) {
        return "ALLOW".equals(policy.redisOutageMode())
                ? Mono.just(new RateLimitDecision(true, -1, 0))
                : Mono.error(new GatewayException(ErrorCode.GW_DEPENDENCY_UNAVAILABLE));
    }
}
