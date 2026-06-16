package com.omar.sentra.gateway.ratelimit;

import java.util.List;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Atomic Redis token bucket using server time.
 */
@Component
public class RedisTokenBucket {
    private static final RedisScript<List> SCRIPT = RedisScript.of("""
            local now = redis.call('TIME')
            local now_ms = now[1] * 1000 + math.floor(now[2] / 1000)
            local data = redis.call('HMGET', KEYS[1], 'tokens', 'ts')
            local tokens = tonumber(data[1])
            local ts = tonumber(data[2])
            local capacity = tonumber(ARGV[1])
            local refill = tonumber(ARGV[2])
            local period_ms = tonumber(ARGV[3]) * 1000
            local ttl = tonumber(ARGV[4])
            if tokens == nil then tokens = capacity end
            if ts == nil then ts = now_ms end
            local elapsed = math.max(0, now_ms - ts)
            tokens = math.min(capacity, tokens + (elapsed / period_ms) * refill)
            local allowed = 0
            local retry = 0
            if tokens >= 1 then
              tokens = tokens - 1
              allowed = 1
            else
              retry = math.ceil(((1 - tokens) / refill) * (period_ms / 1000))
            end
            redis.call('HMSET', KEYS[1], 'tokens', tokens, 'ts', now_ms)
            redis.call('EXPIRE', KEYS[1], ttl)
            return {allowed, math.floor(tokens), retry}
            """, List.class);

    private final ReactiveStringRedisTemplate redis;

    public RedisTokenBucket(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    public Mono<RateLimitDecision> consume(String key, RateLimitPolicy policy) {
        long ttl = Math.max(60, policy.refillPeriodSeconds() * 2L);
        return redis.execute(
                        SCRIPT,
                        List.of("sentra:rl:" + key),
                        List.of(
                                Integer.toString(policy.capacity()),
                                Integer.toString(policy.refillTokens()),
                                Integer.toString(policy.refillPeriodSeconds()),
                                Long.toString(ttl)))
                .next()
                .map(result -> new RateLimitDecision(
                        number(result.get(0)) == 1,
                        number(result.get(1)),
                        number(result.get(2))));
    }

    private static long number(Object value) {
        return value instanceof Number number ? number.longValue() : Long.parseLong(value.toString());
    }
}
