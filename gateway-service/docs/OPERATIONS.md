# Gateway Operations Guide

**Status:** Executable local/container operating guide  
**Container engine:** Podman 5+

## Build Gate

From `gateway-service`:

```powershell
.\mvnw.cmd clean verify
```

This compiles Java 25 sources, runs unit and HTTP integration tests, packages the
Spring Boot JAR, creates a Javadoc JAR, and generates JaCoCo coverage.

Generate the browsable Javadocs explicitly:

```powershell
.\mvnw.cmd javadoc:javadoc
```

## Podman Deployment

Validate Compose:

```powershell
podman compose --env-file .env config
```

Build and start:

```powershell
podman compose --env-file .env up --build -d
podman compose ps
```

The stack contains:

- `postgres`: PostgreSQL 17 with persistent volume
- `redis`: Redis 8 with password and append-only persistence
- `gateway-service`: non-root Java 25 runtime with a read-only root filesystem

The database and Redis networks are internal. Only gateway port `8080` is
published.

Inspect logs:

```powershell
podman compose logs gateway-service
podman compose logs postgres
podman compose logs redis
```

Stop:

```powershell
podman compose down
```

Destroy local data:

```powershell
podman compose down -v
```

## Health Verification

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health/liveness
Invoke-RestMethod http://localhost:8080/actuator/health/readiness
```

Expected status is `UP`. Readiness includes configured dependency health
contributors. The container health check calls the readiness endpoint with
`curl`.

## API Smoke Verification

```powershell
$credential = [Convert]::ToBase64String(
  [Text.Encoding]::ASCII.GetBytes("admin:sentra-admin")
)
$headers = @{ Authorization = "Basic $credential" }

Invoke-RestMethod http://localhost:8080/v3/api-docs
Invoke-RestMethod http://localhost:8080/api/v1/admin/routes -Headers $headers
Invoke-RestMethod http://localhost:8080/api/v1/admin/admin-actions -Headers $headers
```

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

## Postman And Newman

Import:

- `postman/Sentra_Gateway.postman_collection.json`
- `postman/Sentra_Gateway_Local.postman_environment.json`

CLI execution:

```powershell
npx --yes newman run postman/Sentra_Gateway.postman_collection.json `
  -e postman/Sentra_Gateway_Local.postman_environment.json
```

The run checks health, OpenAPI, authentication enforcement, route lifecycle,
client/key issue and rotation, policy creation, audit reads, and cleanup.

## Flyway

`V1__create_gateway_schema.sql` runs automatically before the application becomes
ready. Never edit a migration after releasing it. Add a new ordered migration for
schema changes.

Verify migration state:

```powershell
podman exec sentra-gateway-postgres-1 `
  psql -U sentra -d sentra_gateway -c "select version, description, success from flyway_schema_history order by installed_rank;"
```

Container names may differ by Compose provider; obtain the exact name with
`podman ps`.

## Runtime Data

PostgreSQL owns:

- dynamic routes
- API clients and verifier-only API keys
- rate, IP, and risk policies
- request audit events
- administrative action logs

Redis owns:

- `sentra:nonce:*` replay claims
- `sentra:rl:*` distributed token buckets

Deleting Redis state can permit nonce reuse inside the original nonce lifetime and
resets current rate-limit buckets. Treat Redis data loss as a security event for
signed partner traffic.

## Graceful Shutdown

Spring graceful shutdown is enabled with a 20-second phase timeout. During an
orchestrated shutdown:

1. stop routing new traffic to the instance;
2. allow in-flight requests to finish;
3. let Spring close Netty, R2DBC, and Redis resources;
4. preserve PostgreSQL and Redis volumes.

## Monitoring

Prometheus endpoint:

```text
/actuator/prometheus
```

Key metric families include JVM/process metrics, HTTP server requests, Spring Cloud
Gateway route requests, R2DBC pool metrics where available, Redis command
observations, and Resilience4j circuit metrics.

Do not use request IDs, API keys, source IPs, subjects, or raw paths as metric
labels.

## Failure Behavior

### PostgreSQL

- startup fails when Flyway or initial database connectivity fails;
- admin reads/writes and uncached API-key lookups fail safely;
- already materialized Spring routes can continue only for the lifetime of the
  running route generation;
- restart only after PostgreSQL and migration state are healthy.

### Redis

- signed requests fail when nonce claims cannot be stored;
- rate policies with `DENY` fail closed;
- rate policies with `ALLOW` explicitly continue;
- rate policy value `LOCAL_FALLBACK` currently fails closed.

### JWT Issuer

- Spring Security uses issuer metadata/JWK discovery;
- known cached keys follow framework cache behavior;
- unknown or invalid tokens are rejected;
- never bypass issuer or signature validation during an outage.

### Downstream

- finite connect and response timeouts are attached to each route;
- configured retries apply only to safe methods;
- configured circuit breakers isolate failing route groups;
- failures are normalized into the gateway error contract where raised by gateway
  policy code.

## Backup And Restore

Back up the PostgreSQL volume/database. Redis persistence is useful operationally
but is not a replacement for PostgreSQL backup.

After restore, verify:

1. Flyway history;
2. route count and route generation;
3. active/disabled client and key status;
4. policy counts;
5. audit time continuity;
6. Postman collection success.

## Release Checklist

1. `.\mvnw.cmd clean verify` passes.
2. Javadoc and JaCoCo sites are generated.
3. `.env.example` contains no real secrets.
4. `podman compose config` succeeds.
5. Container image builds.
6. PostgreSQL and Redis become healthy.
7. Gateway readiness becomes `UP`.
8. Swagger loads and contains all admin paths.
9. Newman collection passes without failed assertions.
10. Container runs as UID `10001` with a read-only root filesystem.
11. Production profile has a real JWT issuer and rotated secrets.
12. Rollback remains compatible with applied Flyway migrations.
