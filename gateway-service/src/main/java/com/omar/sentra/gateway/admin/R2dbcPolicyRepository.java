package com.omar.sentra.gateway.admin;

import static com.omar.sentra.gateway.common.util.DbValues.instant;

import com.omar.sentra.gateway.ratelimit.RateLimitPolicy;
import com.omar.sentra.gateway.security.ip.IpRule;
import com.omar.sentra.gateway.security.risk.RiskRule;
import io.r2dbc.spi.Readable;
import java.time.Instant;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * SQL policy repository.
 */
@Repository
public class R2dbcPolicyRepository implements PolicyRepository {
    private final DatabaseClient db;

    public R2dbcPolicyRepository(DatabaseClient db) {
        this.db = db;
    }

    @Override
    public Flux<RateLimitPolicy> findRateLimits() {
        return db.sql("SELECT * FROM rate_limit_policies ORDER BY priority DESC,id").map(this::rate).all();
    }

    @Override
    public Mono<RateLimitPolicy> findRateLimit(String id) {
        return db.sql("SELECT * FROM rate_limit_policies WHERE id=:id").bind("id", id).map(this::rate).one();
    }

    @Override
    public Mono<RateLimitPolicy> saveRateLimit(RateLimitPolicy p, boolean create) {
        DatabaseClient.GenericExecuteSpec spec = create
                ? db.sql("""
                    INSERT INTO rate_limit_policies(id,subject_type,route_id,method,capacity,refill_tokens,
                        refill_period_seconds,priority,redis_outage_mode,response_headers_enabled,enabled,
                        version,created_at,updated_at)
                    VALUES(:id,:subject,:route,:method,:capacity,:tokens,:period,:priority,:outage,:headers,
                        :enabled,1,:created,:updated)
                    """)
                : db.sql("""
                    UPDATE rate_limit_policies SET subject_type=:subject,route_id=:route,method=:method,
                        capacity=:capacity,refill_tokens=:tokens,refill_period_seconds=:period,priority=:priority,
                        redis_outage_mode=:outage,response_headers_enabled=:headers,enabled=:enabled,
                        version=version+1,created_at=:created,updated_at=:updated
                    WHERE id=:id AND version=:version
                    """).bind("version", p.version());
        return spec.bind("id", p.id())
                .bind("subject", p.subjectType())
                .bind("route", nullable(p.routeId(), String.class))
                .bind("method", nullable(p.method(), String.class))
                .bind("capacity", p.capacity())
                .bind("tokens", p.refillTokens())
                .bind("period", p.refillPeriodSeconds())
                .bind("priority", p.priority())
                .bind("outage", p.redisOutageMode())
                .bind("headers", p.responseHeadersEnabled())
                .bind("enabled", p.enabled())
                .bind("created", p.createdAt())
                .bind("updated", p.updatedAt())
                .fetch().rowsUpdated()
                .flatMap(count -> count == 1 ? findRateLimit(p.id()) : Mono.empty());
    }

    @Override
    public Mono<Boolean> deleteRateLimit(String id) {
        return delete("rate_limit_policies", id);
    }

    @Override
    public Flux<IpRule> findIpRules() {
        return db.sql("SELECT * FROM ip_rules ORDER BY priority DESC,id").map(this::ip).all();
    }

    @Override
    public Mono<IpRule> findIpRule(String id) {
        return db.sql("SELECT * FROM ip_rules WHERE id=:id").bind("id", id).map(this::ip).one();
    }

    @Override
    public Mono<IpRule> saveIpRule(IpRule p, boolean create) {
        DatabaseClient.GenericExecuteSpec spec = create
                ? db.sql("""
                    INSERT INTO ip_rules(id,network,action,route_id,priority,reason,valid_from,expires_at,
                        enabled,version,created_at,updated_at)
                    VALUES(:id,:network,:action,:route,:priority,:reason,:validFrom,:expiresAt,:enabled,
                        1,:created,:updated)
                    """)
                : db.sql("""
                    UPDATE ip_rules SET network=:network,action=:action,route_id=:route,priority=:priority,
                        reason=:reason,valid_from=:validFrom,expires_at=:expiresAt,enabled=:enabled,
                        version=version+1,created_at=:created,updated_at=:updated
                    WHERE id=:id AND version=:version
                    """).bind("version", p.version());
        return spec.bind("id", p.id())
                .bind("network", p.network())
                .bind("action", p.action())
                .bind("route", nullable(p.routeId(), String.class))
                .bind("priority", p.priority())
                .bind("reason", p.reason())
                .bind("validFrom", p.validFrom())
                .bind("expiresAt", nullable(p.expiresAt(), Instant.class))
                .bind("enabled", p.enabled())
                .bind("created", p.createdAt())
                .bind("updated", p.updatedAt())
                .fetch().rowsUpdated()
                .flatMap(count -> count == 1 ? findIpRule(p.id()) : Mono.empty());
    }

    @Override
    public Mono<Boolean> deleteIpRule(String id) {
        return delete("ip_rules", id);
    }

    @Override
    public Flux<RiskRule> findRiskRules() {
        return db.sql("SELECT * FROM risk_rules ORDER BY id").map(this::risk).all();
    }

    @Override
    public Mono<RiskRule> findRiskRule(String id) {
        return db.sql("SELECT * FROM risk_rules WHERE id=:id").bind("id", id).map(this::risk).one();
    }

    @Override
    public Mono<RiskRule> saveRiskRule(RiskRule p, boolean create) {
        DatabaseClient.GenericExecuteSpec spec = create
                ? db.sql("""
                    INSERT INTO risk_rules(id,signal,threshold_value,weight,action,route_id,enabled,version,
                        created_at,updated_at)
                    VALUES(:id,:signal,:threshold,:weight,:action,:route,:enabled,1,:created,:updated)
                    """)
                : db.sql("""
                    UPDATE risk_rules SET signal=:signal,threshold_value=:threshold,weight=:weight,
                        action=:action,route_id=:route,enabled=:enabled,version=version+1,updated_at=:updated
                        ,created_at=:created WHERE id=:id AND version=:version
                    """).bind("version", p.version());
        return spec.bind("id", p.id())
                .bind("signal", p.signal())
                .bind("threshold", p.thresholdValue())
                .bind("weight", p.weight())
                .bind("action", p.action())
                .bind("route", nullable(p.routeId(), String.class))
                .bind("enabled", p.enabled())
                .bind("created", p.createdAt())
                .bind("updated", p.updatedAt())
                .fetch().rowsUpdated()
                .flatMap(count -> count == 1 ? findRiskRule(p.id()) : Mono.empty());
    }

    @Override
    public Mono<Boolean> deleteRiskRule(String id) {
        return delete("risk_rules", id);
    }

    private Mono<Boolean> delete(String table, String id) {
        return db.sql("DELETE FROM " + table + " WHERE id=:id")
                .bind("id", id)
                .fetch().rowsUpdated().map(count -> count == 1);
    }

    private RateLimitPolicy rate(Readable row) {
        return new RateLimitPolicy(
                row.get("id", String.class), row.get("subject_type", String.class),
                row.get("route_id", String.class), row.get("method", String.class),
                row.get("capacity", Integer.class), row.get("refill_tokens", Integer.class),
                row.get("refill_period_seconds", Integer.class), row.get("priority", Integer.class),
                row.get("redis_outage_mode", String.class), row.get("response_headers_enabled", Boolean.class),
                row.get("enabled", Boolean.class), row.get("version", Long.class),
                instant(row, "created_at"), instant(row, "updated_at"));
    }

    private IpRule ip(Readable row) {
        return new IpRule(
                row.get("id", String.class), row.get("network", String.class), row.get("action", String.class),
                row.get("route_id", String.class), row.get("priority", Integer.class),
                row.get("reason", String.class), instant(row, "valid_from"), instant(row, "expires_at"),
                row.get("enabled", Boolean.class), row.get("version", Long.class),
                instant(row, "created_at"), instant(row, "updated_at"));
    }

    private RiskRule risk(Readable row) {
        return new RiskRule(
                row.get("id", String.class), row.get("signal", String.class),
                row.get("threshold_value", Integer.class), row.get("weight", Integer.class),
                row.get("action", String.class), row.get("route_id", String.class),
                row.get("enabled", Boolean.class), row.get("version", Long.class),
                instant(row, "created_at"), instant(row, "updated_at"));
    }

    private static <T> Parameter nullable(T value, Class<T> type) {
        return value == null ? Parameter.empty(type) : Parameter.from(value);
    }
}
