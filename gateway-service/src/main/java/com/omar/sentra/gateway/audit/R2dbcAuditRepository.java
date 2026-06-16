package com.omar.sentra.gateway.audit;

import static com.omar.sentra.gateway.common.util.DbValues.instant;

import io.r2dbc.spi.Readable;
import java.time.Instant;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * SQL audit repository.
 */
@Repository
public class R2dbcAuditRepository implements AuditRepository {
    private final DatabaseClient db;

    public R2dbcAuditRepository(DatabaseClient db) {
        this.db = db;
    }

    @Override
    public Mono<Void> insert(AuditEvent e) {
        return db.sql("""
                INSERT INTO audit_events(id,event_time,request_id,trace_id,event_type,decision,reason_code,
                    route_id,method,path,actor_type,subject_ref,source_ip,status,latency_ms,instance_id,environment)
                VALUES(:id,:time,:requestId,:traceId,:eventType,:decision,:reason,:routeId,:method,:path,
                    :actorType,:subjectRef,:sourceIp,:status,:latency,:instanceId,:environment)
                """)
                .bind("id", e.id()).bind("time", e.eventTime()).bind("requestId", e.requestId())
                .bind("traceId", nullable(e.traceId(), String.class)).bind("eventType", e.eventType())
                .bind("decision", e.decision()).bind("reason", e.reasonCode())
                .bind("routeId", nullable(e.routeId(), String.class)).bind("method", nullable(e.method(), String.class))
                .bind("path", nullable(e.path(), String.class)).bind("actorType", nullable(e.actorType(), String.class))
                .bind("subjectRef", nullable(e.subjectRef(), String.class))
                .bind("sourceIp", nullable(e.sourceIp(), String.class)).bind("status", e.status())
                .bind("latency", e.latencyMs()).bind("instanceId", e.instanceId())
                .bind("environment", e.environment()).fetch().rowsUpdated().then();
    }

    @Override
    public Mono<Void> insertAdminAction(AdminAction a) {
        return db.sql("""
                INSERT INTO admin_action_logs(id,event_time,actor,action,target_type,target_id,result,
                    change_summary,request_id)
                VALUES(:id,:time,:actor,:action,:targetType,:targetId,:result,:summary,:requestId)
                """)
                .bind("id", a.id()).bind("time", a.eventTime()).bind("actor", a.actor())
                .bind("action", a.action()).bind("targetType", a.targetType()).bind("targetId", a.targetId())
                .bind("result", a.result()).bind("summary", a.changeSummary())
                .bind("requestId", nullable(a.requestId(), String.class)).fetch().rowsUpdated().then();
    }

    @Override
    public Flux<AuditEvent> search(
            Instant from, Instant to, String requestId, String routeId, int limit, int offset) {
        return db.sql("""
                SELECT * FROM audit_events
                 WHERE event_time >= :from AND event_time <= :to
                   AND (:requestId IS NULL OR request_id=:requestId)
                   AND (:routeId IS NULL OR route_id=:routeId)
                 ORDER BY event_time DESC LIMIT :limit OFFSET :offset
                """)
                .bind("from", from).bind("to", to)
                .bind("requestId", nullable(requestId, String.class))
                .bind("routeId", nullable(routeId, String.class))
                .bind("limit", limit).bind("offset", offset)
                .map(this::map).all();
    }

    @Override
    public Mono<AuditEvent> findById(UUID id) {
        return db.sql("SELECT * FROM audit_events WHERE id=:id").bind("id", id).map(this::map).one();
    }

    @Override
    public Flux<AdminAction> findAdminActions(int limit, int offset) {
        return db.sql("SELECT * FROM admin_action_logs ORDER BY event_time DESC LIMIT :limit OFFSET :offset")
                .bind("limit", limit).bind("offset", offset)
                .map(row -> new AdminAction(
                        row.get("id", UUID.class), instant(row, "event_time"), row.get("actor", String.class),
                        row.get("action", String.class), row.get("target_type", String.class),
                        row.get("target_id", String.class), row.get("result", String.class),
                        row.get("change_summary", String.class), row.get("request_id", String.class)))
                .all();
    }

    private AuditEvent map(Readable row) {
        return new AuditEvent(
                row.get("id", UUID.class), instant(row, "event_time"), row.get("request_id", String.class),
                row.get("trace_id", String.class), row.get("event_type", String.class),
                row.get("decision", String.class), row.get("reason_code", String.class),
                row.get("route_id", String.class), row.get("method", String.class), row.get("path", String.class),
                row.get("actor_type", String.class), row.get("subject_ref", String.class),
                row.get("source_ip", String.class), row.get("status", Integer.class),
                row.get("latency_ms", Long.class), row.get("instance_id", String.class),
                row.get("environment", String.class));
    }

    private static <T> Parameter nullable(T value, Class<T> type) {
        return value == null ? Parameter.empty(type) : Parameter.from(value);
    }
}
