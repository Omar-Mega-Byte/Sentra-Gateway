package com.omar.sentra.gateway.security.apikey;

import static com.omar.sentra.gateway.common.util.DbValues.instant;
import static com.omar.sentra.gateway.common.util.TextListCodec.decode;
import static com.omar.sentra.gateway.common.util.TextListCodec.encode;

import io.r2dbc.spi.Readable;
import java.time.Instant;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * SQL implementation of API client and key persistence.
 */
@Repository
public class R2dbcApiClientRepository implements ApiClientRepository {
    private static final String CLIENT_COLUMNS =
            "SELECT id,name,owner,tenant_id,status,version,created_at,updated_at FROM api_clients";
    private static final String KEY_COLUMNS = """
            SELECT id,client_id,prefix,verifier,pepper_version,scopes,allowed_routes,status,
                   valid_from,expires_at,rotated_from,last_used_at,created_at FROM api_keys
            """;

    private final DatabaseClient databaseClient;

    public R2dbcApiClientRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Flux<ApiClient> findClients() {
        return databaseClient.sql(CLIENT_COLUMNS + " ORDER BY name").map(this::mapClient).all();
    }

    @Override
    public Mono<ApiClient> findClient(UUID id) {
        return databaseClient.sql(CLIENT_COLUMNS + " WHERE id=:id")
                .bind("id", id)
                .map(this::mapClient)
                .one();
    }

    @Override
    public Mono<ApiClient> insertClient(ApiClient client) {
        return databaseClient.sql("""
                INSERT INTO api_clients(id,name,owner,tenant_id,status,version,created_at,updated_at)
                VALUES(:id,:name,:owner,:tenant,:status,1,:created,:updated)
                """)
                .bind("id", client.id())
                .bind("name", client.name())
                .bind("owner", client.owner())
                .bind("tenant", nullable(client.tenantId(), String.class))
                .bind("status", client.status().name())
                .bind("created", client.createdAt())
                .bind("updated", client.updatedAt())
                .fetch()
                .rowsUpdated()
                .then(findClient(client.id()));
    }

    @Override
    public Mono<ApiClient> updateClient(ApiClient client, long expectedVersion) {
        return databaseClient.sql("""
                UPDATE api_clients SET name=:name,owner=:owner,tenant_id=:tenant,status=:status,
                    version=version+1,updated_at=:updated
                WHERE id=:id AND version=:version
                """)
                .bind("id", client.id())
                .bind("name", client.name())
                .bind("owner", client.owner())
                .bind("tenant", nullable(client.tenantId(), String.class))
                .bind("status", client.status().name())
                .bind("updated", client.updatedAt())
                .bind("version", expectedVersion)
                .fetch()
                .rowsUpdated()
                .flatMap(count -> count == 1 ? findClient(client.id()) : Mono.empty());
    }

    @Override
    public Flux<ApiKeyRecord> findKeysByClient(UUID clientId) {
        return databaseClient.sql(KEY_COLUMNS + " WHERE client_id=:clientId ORDER BY created_at DESC")
                .bind("clientId", clientId)
                .map(this::mapKey)
                .all();
    }

    @Override
    public Mono<ApiKeyRecord> findKey(UUID id) {
        return databaseClient.sql(KEY_COLUMNS + " WHERE id=:id")
                .bind("id", id)
                .map(this::mapKey)
                .one();
    }

    @Override
    public Mono<ApiKeyRecord> findActiveKeyByPrefix(String prefix) {
        return databaseClient.sql(KEY_COLUMNS + " WHERE prefix=:prefix AND status='ACTIVE'")
                .bind("prefix", prefix)
                .map(this::mapKey)
                .one();
    }

    @Override
    public Mono<ApiKeyRecord> insertKey(ApiKeyRecord key) {
        return databaseClient.sql("""
                INSERT INTO api_keys(id,client_id,prefix,verifier,pepper_version,scopes,allowed_routes,status,
                    valid_from,expires_at,rotated_from,last_used_at,created_at)
                VALUES(:id,:clientId,:prefix,:verifier,:pepperVersion,:scopes,:routes,:status,
                    :validFrom,:expiresAt,:rotatedFrom,:lastUsedAt,:createdAt)
                """)
                .bind("id", key.id())
                .bind("clientId", key.clientId())
                .bind("prefix", key.prefix())
                .bind("verifier", key.verifier())
                .bind("pepperVersion", key.pepperVersion())
                .bind("scopes", encode(key.scopes()))
                .bind("routes", encode(key.allowedRoutes()))
                .bind("status", key.status().name())
                .bind("validFrom", key.validFrom())
                .bind("expiresAt", nullable(key.expiresAt(), Instant.class))
                .bind("rotatedFrom", nullable(key.rotatedFrom(), UUID.class))
                .bind("lastUsedAt", nullable(key.lastUsedAt(), Instant.class))
                .bind("createdAt", key.createdAt())
                .fetch()
                .rowsUpdated()
                .then(findKey(key.id()));
    }

    @Override
    public Mono<Boolean> revokeKey(UUID id) {
        return databaseClient.sql("UPDATE api_keys SET status='REVOKED' WHERE id=:id AND status='ACTIVE'")
                .bind("id", id)
                .fetch()
                .rowsUpdated()
                .map(count -> count == 1);
    }

    @Override
    public Mono<Void> touchLastUsed(UUID id) {
        return databaseClient.sql("UPDATE api_keys SET last_used_at=:now WHERE id=:id")
                .bind("id", id)
                .bind("now", Instant.now())
                .fetch()
                .rowsUpdated()
                .then();
    }

    private ApiClient mapClient(Readable row) {
        return new ApiClient(
                row.get("id", UUID.class),
                row.get("name", String.class),
                row.get("owner", String.class),
                row.get("tenant_id", String.class),
                ClientStatus.valueOf(row.get("status", String.class)),
                row.get("version", Long.class),
                instant(row, "created_at"),
                instant(row, "updated_at"));
    }

    private ApiKeyRecord mapKey(Readable row) {
        return new ApiKeyRecord(
                row.get("id", UUID.class),
                row.get("client_id", UUID.class),
                row.get("prefix", String.class),
                row.get("verifier", String.class),
                row.get("pepper_version", String.class),
                decode(row.get("scopes", String.class)),
                decode(row.get("allowed_routes", String.class)),
                KeyStatus.valueOf(row.get("status", String.class)),
                instant(row, "valid_from"),
                instant(row, "expires_at"),
                row.get("rotated_from", UUID.class),
                instant(row, "last_used_at"),
                instant(row, "created_at"));
    }

    private static <T> Parameter nullable(T value, Class<T> type) {
        return value == null ? Parameter.empty(type) : Parameter.from(value);
    }
}
