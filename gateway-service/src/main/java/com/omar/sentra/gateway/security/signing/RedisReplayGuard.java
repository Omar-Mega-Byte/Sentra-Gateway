package com.omar.sentra.gateway.security.signing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Redis SET-NX replay guard.
 */
@Component
public class RedisReplayGuard implements ReplayGuard {
    private final ReactiveStringRedisTemplate redis;

    public RedisReplayGuard(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Mono<Boolean> claim(String keyId, String nonce, Duration ttl) {
        String key = "sentra:nonce:" + keyId + ":" + sha256(nonce);
        return redis.opsForValue().setIfAbsent(key, "1", ttl).defaultIfEmpty(false);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
