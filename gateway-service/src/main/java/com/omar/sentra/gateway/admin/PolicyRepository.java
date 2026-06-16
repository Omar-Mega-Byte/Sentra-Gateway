package com.omar.sentra.gateway.admin;

import com.omar.sentra.gateway.ratelimit.RateLimitPolicy;
import com.omar.sentra.gateway.security.ip.IpRule;
import com.omar.sentra.gateway.security.risk.RiskRule;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive persistence for gateway security policies.
 */
public interface PolicyRepository {
    Flux<RateLimitPolicy> findRateLimits();

    Mono<RateLimitPolicy> findRateLimit(String id);

    Mono<RateLimitPolicy> saveRateLimit(RateLimitPolicy policy, boolean create);

    Mono<Boolean> deleteRateLimit(String id);

    Flux<IpRule> findIpRules();

    Mono<IpRule> findIpRule(String id);

    Mono<IpRule> saveIpRule(IpRule rule, boolean create);

    Mono<Boolean> deleteIpRule(String id);

    Flux<RiskRule> findRiskRules();

    Mono<RiskRule> findRiskRule(String id);

    Mono<RiskRule> saveRiskRule(RiskRule rule, boolean create);

    Mono<Boolean> deleteRiskRule(String id);
}
