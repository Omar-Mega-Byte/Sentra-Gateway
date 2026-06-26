# Gateway Configuration

**Status:** Implemented configuration reference  
**Sources:** `application.yml`, `application-local.yml`, `SentraProperties`

## Precedence

Spring Boot applies command-line arguments, environment variables, profile
configuration, base configuration, and code defaults in normal precedence order.
Compose loads `.env` and passes values into the service container.

`.env` is ignored by Git. `.env.example` contains the complete local template.

## Profiles

| Profile | Behavior |
| --- | --- |
| `local` | HTTP Basic admin users, public Swagger, local-friendly defaults |
| `test` | H2 database, no Flyway, deterministic local authentication |
| other/JWT | OAuth2 Resource Server, protected Swagger and metrics |

Production must not use `local`.

## Application

| Variable | Default | Meaning |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | none | Active Spring profile |
| `SERVER_PORT` | `8080` | HTTP port |
| `SENTRA_ENVIRONMENT` | `local` | API-key prefix and metric environment |
| `SENTRA_INSTANCE_ID` | random UUID | Instance identity in audit events |

## PostgreSQL And Flyway

| Variable | Default |
| --- | --- |
| `DB_HOST` | `localhost` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `sentra_gateway` |
| `DB_USERNAME` | `sentra` |
| `DB_PASSWORD` | `sentra_local_password` |
| `DB_POOL_MIN_SIZE` | `2` |
| `DB_POOL_MAX_SIZE` | `20` |
| `DB_CONNECT_TIMEOUT` | `2s` |
| `FLYWAY_ENABLED` | `true` |

Flyway uses JDBC during startup. Runtime repositories use pooled R2DBC. Compose
also uses `POSTGRES_DB`, `POSTGRES_USER`, and `POSTGRES_PASSWORD`; these values must
match `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD`.

## Redis

| Variable | Default |
| --- | --- |
| `REDIS_HOST` | `localhost` |
| `REDIS_PORT` | `6379` |
| `REDIS_PASSWORD` | `sentra_local_redis` |
| `REDIS_CONNECT_TIMEOUT` | `1s` |
| `REDIS_COMMAND_TIMEOUT` | `500ms` |

Redis stores replay claims and token-bucket state. It is not the durable source of
routes, policies, clients, keys, or audit records.

## Authentication

| Variable | Default | Notes |
| --- | --- | --- |
| `JWT_ISSUER_URI` | empty | Required when local auth is disabled |
| `LOCAL_ADMIN_USERNAME` | `admin` | Local profile only |
| `LOCAL_ADMIN_PASSWORD` | `sentra-admin` | Local profile only |
| `LOCAL_OPERATOR_USERNAME` | `operator` | Local profile only |
| `LOCAL_OPERATOR_PASSWORD` | `sentra-operator` | Local profile only |

JWT `scope`/`scp` values map to `SCOPE_*`. The `roles` claim maps to `ROLE_*`.
Issuer, signature, timestamps, and standard resource-server validation are handled
by Spring Security.

## API Keys And Signing

| Variable | Default |
| --- | --- |
| `API_KEY_PEPPER` | none in base config |
| `SIGNING_TIMESTAMP_SKEW` | `5m` |
| `SIGNING_NONCE_TTL` | `10m` |

`API_KEY_PEPPER` must contain at least 32 characters. The local profile supplies a
development fallback, but Compose explicitly passes the `.env` value. Production
must inject a high-entropy secret.

## Route Safety

| Variable | Default |
| --- | --- |
| `ROUTE_ALLOWED_SCHEMES` | `http,https` |
| `ROUTE_ALLOWED_SERVICE_HOSTS` | empty |
| `ROUTE_REFRESH_INTERVAL` | `30s` |

`ROUTE_ALLOWED_SERVICE_HOSTS` is a comma-separated host allowlist. Compose uses:

```text
user-service,order-service,payment-service,notification-service,host.containers.internal
```

Route changes publish a Spring Cloud Gateway refresh event immediately. The
refresh interval is retained as configuration for reconciliation work; no
independent scheduled reconciliation task is currently required by the runtime.

## Request Limits

| Variable | Default |
| --- | --- |
| `REQUEST_ID_MAX_LENGTH` | `128` |
| `MAX_REQUEST_HEADER_BYTES` | `32768` |
| `MAX_REQUEST_BODY_BYTES` | `10485760` |
| `MAX_SIGNED_BODY_BYTES` | `1048576` |

The signed-body limit is enforced before signature verification. The global body
and header values are validated configuration ceilings and should also be applied
at the ingress/proxy layer.

## Audit

| Variable | Default |
| --- | --- |
| `AUDIT_SEARCH_MAX_RANGE` | `31d` |

Audit search rejects inverted ranges, ranges over the configured maximum, negative
pages, and page sizes outside 1 through 100.

## Management And Swagger

Fixed endpoints:

- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/prometheus`
- `/actuator/metrics`
- `/v3/api-docs`
- `/swagger-ui.html`

Health is public. Swagger is public only in local mode. Prometheus, metrics, and
protected Swagger require operator or super-admin roles in JWT mode. Network-level
ingress policy must provide an additional production boundary.

## Complete `.env.example`

```dotenv
SPRING_PROFILES_ACTIVE=local
SENTRA_ENVIRONMENT=local
SERVER_PORT=8080
POSTGRES_DB=sentra_gateway
POSTGRES_USER=sentra
POSTGRES_PASSWORD=change-me-consistently
DB_HOST=postgres
DB_PORT=5432
DB_NAME=sentra_gateway
DB_USERNAME=sentra
DB_PASSWORD=change-me-consistently
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=change-me
API_KEY_PEPPER=replace-with-at-least-32-random-bytes
LOCAL_ADMIN_USERNAME=admin
LOCAL_ADMIN_PASSWORD=change-me
LOCAL_OPERATOR_USERNAME=operator
LOCAL_OPERATOR_PASSWORD=change-me
ROUTE_ALLOWED_SERVICE_HOSTS=user-service,order-service,payment-service,notification-service
```

## Secret Rules

- Never commit `.env`.
- Never store plaintext API keys; only one-time responses contain them.
- Never expose PostgreSQL, Redis, or downstream ports publicly.
- Do not enable Spring Security debug logging in production.
- Rotate the API-key pepper only through a versioned migration strategy; existing
  records currently use verifier version `v1`.
- Replace every sample password before any shared deployment.
