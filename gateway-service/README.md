# Sentra Gateway Service

Reactive API security gateway and administration service for the Sentra platform.

## Status

Implemented and executable as of June 15, 2026.

- Java 25
- Spring Boot 4.0.7
- Spring Cloud 2025.1.2
- Spring Cloud Gateway WebFlux 5.0.2
- springdoc-openapi 3.0.3
- PostgreSQL 17
- Redis 8
- Podman 5+

The service provides database-backed dynamic routes, local or JWT administration
authentication, partner API keys, HMAC request signing, replay protection,
IP/risk/rate policies, audit persistence, Prometheus metrics, Swagger UI, Flyway
migrations, and non-root container packaging.

## Quick Start With Podman

Requirements:

- Podman 5 or newer with a running Podman machine
- PowerShell

From `gateway-service`:

```powershell
podman compose --env-file .env up --build -d
podman compose ps
```

Wait until `gateway-service` is healthy, then open:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Readiness: <http://localhost:8080/actuator/health/readiness>
- Liveness: <http://localhost:8080/actuator/health/liveness>

Local administration credentials come from `.env`:

```text
admin / sentra-admin
operator / sentra-operator
```

The admin account can mutate routes, clients, keys, and policies. The operator
account has read-only route access and audit access.

Stop the stack:

```powershell
podman compose down
```

Delete local PostgreSQL and Redis data as well:

```powershell
podman compose down -v
```

## Build And Test

The repository includes Maven Wrapper 3.9.12.

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd javadoc:javadoc
```

Generated artifacts:

- Application JAR: `target/gateway-service-1.0.0-SNAPSHOT.jar`
- Javadoc JAR: `target/gateway-service-1.0.0-SNAPSHOT-javadoc.jar`
- Javadoc site: `target/reports/apidocs/index.html`
- JaCoCo report: `target/site/jacoco/index.html`
- Surefire reports: `target/surefire-reports/`

The test profile uses an in-memory H2 database in PostgreSQL compatibility mode.
Podman smoke tests provide the PostgreSQL, Redis, Flyway, image, and network
verification.

## Run From Maven

Start PostgreSQL and Redis first, or use the complete Podman stack. Environment
variables in `.env` are consumed by Compose; Spring itself does not automatically
load `.env`.

For a direct Maven run, set dependency hosts to `localhost`:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:DB_HOST = "localhost"
$env:DB_NAME = "sentra_gateway"
$env:DB_USERNAME = "sentra"
$env:DB_PASSWORD = "sentra_local_password"
$env:REDIS_HOST = "localhost"
$env:REDIS_PASSWORD = "sentra_local_redis"
$env:API_KEY_PEPPER = "replace-with-at-least-32-random-bytes"
.\mvnw.cmd spring-boot:run
```

## Postman

Import both files:

- `postman/Sentra_Gateway.postman_collection.json`
- `postman/Sentra_Gateway_Local.postman_environment.json`

Select **Sentra Gateway Local**, start the Podman stack, and run the collection in
folder order. The collection generates unique route, client, and policy IDs,
captures API-key values, verifies response contracts, and removes temporary routes
and policies.

Run the same collection without the Postman desktop application:

```powershell
npx --yes newman run postman/Sentra_Gateway.postman_collection.json `
  -e postman/Sentra_Gateway_Local.postman_environment.json
```

## Administrative APIs

Base path: `/api/v1/admin`

| Area | Endpoints |
| --- | --- |
| Routes | `GET/POST /routes`, `POST /routes/validate`, `GET/PUT/DELETE /routes/{id}`, `POST /routes/{id}/enable`, `POST /routes/{id}/disable`, `GET /routes/generation` |
| API clients | `GET/POST /api-clients`, `GET/PUT /api-clients/{id}`, `POST /api-clients/{id}/disable` |
| API keys | `GET/POST /api-clients/{id}/keys`, `POST /api-keys/{id}/rotate`, `POST /api-keys/{id}/revoke` |
| Rate limits | `GET/POST /rate-limits`, `GET/PUT/DELETE /rate-limits/{id}` |
| IP rules | `GET/POST /ip-rules`, `GET/PUT/DELETE /ip-rules/{id}` |
| Risk rules | `GET/POST /risk-rules`, `GET/PUT/DELETE /risk-rules/{id}` |
| Audit | `GET /audit-events`, `GET /audit-events/{id}`, `GET /admin-actions` |

Swagger is generated from the implemented controllers and schemas.

## Route Runtime

Enabled records in `gateway_routes` become Spring Cloud Gateway routes. Route
metadata controls:

- category: `PUBLIC`, `USER`, `PARTNER`, `ADMIN`, or `INTERNAL`
- accepted HTTP methods and path patterns
- destination URI and prefix stripping
- JWT roles/scopes or partner API-key scopes
- optional HMAC signing and Redis replay prevention
- selected IP, risk, and distributed rate-limit policies
- connect/response timeouts
- bounded retries for `GET`, `HEAD`, and `OPTIONS`
- Resilience4j circuit breaker name

Route targets are restricted to configured schemes and allowlisted service hosts.
Reserved administration, Actuator, and Swagger paths cannot be registered as
dynamic routes.

## Authentication

The `local` profile enables HTTP Basic authentication for development.

Outside local mode, the service uses OAuth2 Resource Server JWT validation through
`JWT_ISSUER_URI`. Standard `scope`/`scp` claims become `SCOPE_*` authorities. The
`roles` claim becomes `ROLE_*` authorities.

Administrative role boundaries:

- `GATEWAY_ROUTE_ADMIN`: route reads and mutations
- `GATEWAY_SECURITY_ADMIN`: API clients, keys, and policies
- `GATEWAY_AUDITOR`: audit reads
- `GATEWAY_OPERATOR`: route reads, metrics, and protected documentation
- `GATEWAY_SUPER_ADMIN`: all administration

Swagger is public only in the local profile. Health probes are public. Metrics
require operator or super-admin authentication.

## API Keys And Signing

API keys use:

```text
sgw_<environment>_<12-hex-prefix>_<base64url-secret>
```

Only an HMAC verifier is persisted. Plaintext is returned once during issue or
rotation. Metadata endpoints exclude both plaintext and verifier fields. Rotation
creates a successor and revokes the predecessor.

Signed partner routes require:

- `X-API-Key`
- `X-Sentra-Key-Id`
- `X-Sentra-Timestamp`
- `X-Sentra-Nonce`
- `X-Sentra-Signature`

The signature is base64url HMAC-SHA-256 over method, normalized path, canonical
query, exact body SHA-256, timestamp, nonce, and key ID. Redis atomically claims
each nonce.

## Configuration

Copy `.env.example` to `.env` for a new environment and replace every placeholder.
`.env` is intentionally ignored by Git.

Important variables:

| Variable | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local` for Basic auth, otherwise JWT |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | PostgreSQL target |
| `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL credentials |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Redis target |
| `API_KEY_PEPPER` | Server-side API-key verifier secret, at least 32 characters |
| `JWT_ISSUER_URI` | JWT issuer outside local mode |
| `ROUTE_ALLOWED_SERVICE_HOSTS` | Comma-separated route target allowlist |
| `SIGNING_TIMESTAMP_SKEW` | Accepted signing clock skew, default `5m` |
| `SIGNING_NONCE_TTL` | Replay nonce lifetime, default `10m` |

See [Configuration](docs/CONFIGURATION.md) for every implemented setting.

## Documentation

- [Service Documentation](docs/GATEWAY_SERVICE_DOCUMENTATION.md)
- [API Contract](docs/API_CONTRACT.md)
- [Configuration](docs/CONFIGURATION.md)
- [Operations](docs/OPERATIONS.md)
- [Requirements Traceability](docs/REQUIREMENTS_TRACEABILITY.md)

## Production Notes

- Do not use the local profile or `{noop}` Basic passwords in production.
- Inject secrets through a secret manager or platform secret mechanism.
- Keep PostgreSQL, Redis, downstream services, metrics, and administration paths
  off the public network.
- Terminate TLS at an approved ingress or add service-side TLS.
- Configure a real JWT issuer before disabling local authentication.
- Run at least two gateway replicas only after validating shared Redis rate and
  replay behavior and database connection budgets.
