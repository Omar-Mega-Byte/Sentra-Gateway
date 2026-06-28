# Order Service Configuration

**Version:** 1.0.0  
**Status:** Target configuration contract; implementation not yet verified

## Precedence

Configuration precedence follows Spring Boot:

1. command-line arguments;
2. environment variables;
3. profile-specific application configuration;
4. base application configuration;
5. code defaults.

`.env` is consumed by Podman Compose. A direct JVM launch does not automatically
load it.

## Profiles

| Profile | Purpose | Required behavior |
| --- | --- | --- |
| `local` | Developer JVM/Podman and Postman | Seed data, OpenAPI, Swagger UI |
| `test` | Automated verification | Deterministic clock/data where needed |
| `prod` | Production-like deployment | No seed data, no public Swagger, strict provenance |

Unknown profiles must not silently activate local behavior.

## Environment Catalog

### Application

| Variable | Type | Default | Required | Sensitive | Restart |
| --- | --- | --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | profile list | none | Yes | No | Yes |
| `SERVER_PORT` | integer | `8082` | No | No | Yes |
| `SERVICE_NAME` | string | `order-service` | No | No | Yes |
| `SENTRA_ENVIRONMENT` | enum/string | active profile | Production-like | No | Yes |
| `SENTRA_INSTANCE_ID` | string | generated | No | No | Yes |
| `SHUTDOWN_TIMEOUT` | duration | `20s` | No | No | Yes |

Validation:

- `SERVER_PORT` is `1-65535`.
- `SERVICE_NAME` must be exactly `order-service` in production-like profiles.
- `SHUTDOWN_TIMEOUT` is `5s-120s`.

### Repository And Seed

| Variable | Type | Default | Required | Sensitive | Restart |
| --- | --- | --- | --- | --- | --- |
| `ORDER_REPOSITORY_MODE` | enum | `memory` | Yes | No | Yes |
| `ORDER_SEED_ENABLED` | boolean | `false` | No | No | Yes |
| `ORDER_SEED_DATASET` | string | `default` | No | No | Yes |

Supported baseline mode is `memory`. Any future `jdbc` mode requires separately
documented URL, credential, migration, pool, timeout, backup, and recovery
configuration before it is considered valid.

Rules:

- `ORDER_SEED_ENABLED=true` is permitted only in `local` and `test`.
- Unknown repository modes fail startup.
- Seed initialization is deterministic and idempotent.

### Gateway Context

| Variable | Type | Default | Required | Sensitive | Restart |
| --- | --- | --- | --- | --- | --- |
| `GATEWAY_CONTEXT_REQUIRED` | boolean | `true` | Yes | No | Yes |
| `GATEWAY_ALLOWED_ROUTE_IDS` | escaped list | four order route IDs | Yes | No | Yes |
| `GATEWAY_ALLOWED_PEERS` | list of CIDR/IP/service names | empty | Production-like | No | Yes |
| `TRUSTED_HEADER_MAX_LENGTH` | integer | `255` | No | No | Yes |
| `REQUEST_ID_MAX_LENGTH` | integer | `128` | No | No | Yes |
| `SUBJECT_MAX_LENGTH` | integer | `255` | No | No | Yes |
| `TENANT_ID_MAX_LENGTH` | integer | `128` | No | No | Yes |

Default route IDs:

```text
orders-list,orders-get,orders-create,admin-orders-list
```

Production-like startup fails when:

- `GATEWAY_CONTEXT_REQUIRED=false`;
- `GATEWAY_ALLOWED_PEERS` is empty;
- an allowed route ID is blank, duplicated, or unknown;
- wildcard peer or route values are configured.

Peer matching is based on the actual socket peer or a reviewed workload identity,
not `X-Forwarded-For`.

### Request And Domain Limits

| Variable | Type | Default | Validation |
| --- | --- | ---: | --- |
| `MAX_REQUEST_BODY_BYTES` | integer | `32768` | `1024-1048576` |
| `MAX_PAGE_SIZE` | integer | `100` | `1-500` |
| `DEFAULT_PAGE_SIZE` | integer | `20` | `1-MAX_PAGE_SIZE` |
| `MAX_PAGE_NUMBER` | integer | `10000` | `100-1000000` |
| `MAX_ITEMS_PER_ORDER` | integer | `50` | `1-500` |
| `MAX_SKU_LENGTH` | integer | `64` | `8-255` |
| `MAX_ITEM_QUANTITY` | integer | `100` | `1-100000` |
| `MAX_ERROR_DETAILS` | integer | `20` | `1-100` |

Startup fails if defaults exceed maxima or limits are internally inconsistent.
Runtime validation must occur before allocation grows with attacker-controlled
input.

### Idempotency

| Variable | Type | Default | Validation |
| --- | --- | ---: | --- |
| `IDEMPOTENCY_ENABLED` | boolean | `true` | Must be true if create retries are enabled upstream |
| `IDEMPOTENCY_KEY_MAX_LENGTH` | integer | `128` | `16-255` |
| `IDEMPOTENCY_RETENTION` | duration | `24h` | `1m-7d` |
| `IDEMPOTENCY_MAX_RECORDS` | integer | `10000` | `100-1000000` |
| `IDEMPOTENCY_CLEANUP_INTERVAL` | duration | `5m` | `10s-1h` |

The in-memory repository must bound retained records by both expiry and maximum
count. Reaching the safe maximum must reject new keyed creates with
`503 ORD_IDEMPOTENCY_CAPACITY_EXCEEDED`; it must not evict a live record and
permit an unsafe duplicate.

Idempotency configuration is not secret, but keys and fingerprints are sensitive
runtime values and must not be logged.

### Management And OpenAPI

| Variable | Type | Default | Required | Sensitive | Restart |
| --- | --- | --- | --- | --- | --- |
| `MANAGEMENT_PORT` | integer | `8082` | No | No | Yes |
| `MANAGEMENT_ALLOWED_CIDRS` | CIDR list | empty | Production-like | No | Yes |
| `METRICS_ENABLED` | boolean | `true` | Yes | No | Yes |
| `OPENAPI_ENABLED` | boolean | `false` | No | No | Yes |
| `SWAGGER_UI_ENABLED` | boolean | `false` | No | No | Yes |

Local/test may enable OpenAPI and Swagger UI. Production-like startup fails if
Swagger UI is enabled without an explicit protected-operations exception.

Exposed Actuator endpoints are limited to:

```text
health,info,prometheus,metrics
```

Environment, config properties, heap dump, thread dump, loggers, mappings, and
shutdown endpoints are not exposed by default.

### Logging

| Variable | Type | Default | Rule |
| --- | --- | --- | --- |
| `LOG_LEVEL_ROOT` | level | `INFO` | Production must not default to `DEBUG` or `TRACE` |
| `LOG_FORMAT` | enum | `json` in prod | Structured machine-readable output |
| `LOG_INCLUDE_ORDER_IDS` | boolean | `false` | Must remain false by default |
| `LOG_INCLUDE_OWNER_REFERENCES` | boolean | `false` | Prohibited in production by default |

Regardless of level, logs must redact:

- authorization and cookie headers;
- JWTs and credentials;
- trusted subject and tenant values unless irreversibly transformed under an
  approved diagnostic policy;
- `Idempotency-Key` and payload fingerprint;
- complete order bodies and SKU arrays;
- sensitive query values.

### Container And Network

| Variable | Type | Default | Purpose |
| --- | --- | --- | --- |
| `SENTRA_SERVICES_NETWORK` | string | `sentra-gateway_services` | Shared internal application network |
| `ORDER_IMAGE` | image reference | `localhost/sentra/order-service:1.0.0` | Compose image override |

The base Compose file exposes port `8082` only to container networks. A local
Postman override may publish `127.0.0.1:8082`.

## `.env.example`

The implementation must provide a committed `.env.example` with every supported
key and safe local placeholders. It must contain no passwords, tokens, private
keys, production hosts, or personal data.

An uncommitted `.env` may contain local values. `.gitignore` must exclude `.env`
while retaining `.env.example`.

Expected local example:

```dotenv
SPRING_PROFILES_ACTIVE=local
SERVER_PORT=8082
SENTRA_ENVIRONMENT=local
SENTRA_INSTANCE_ID=order-service-local
SERVICE_NAME=order-service
SHUTDOWN_TIMEOUT=20s

ORDER_REPOSITORY_MODE=memory
ORDER_SEED_ENABLED=true
ORDER_SEED_DATASET=default

GATEWAY_CONTEXT_REQUIRED=true
GATEWAY_ALLOWED_ROUTE_IDS=orders-list,orders-get,orders-create,admin-orders-list
GATEWAY_ALLOWED_PEERS=gateway-service
TRUSTED_HEADER_MAX_LENGTH=255
REQUEST_ID_MAX_LENGTH=128
SUBJECT_MAX_LENGTH=255
TENANT_ID_MAX_LENGTH=128

MAX_REQUEST_BODY_BYTES=32768
MAX_PAGE_SIZE=100
DEFAULT_PAGE_SIZE=20
MAX_PAGE_NUMBER=10000
MAX_ITEMS_PER_ORDER=50
MAX_SKU_LENGTH=64
MAX_ITEM_QUANTITY=100
MAX_ERROR_DETAILS=20

IDEMPOTENCY_ENABLED=true
IDEMPOTENCY_KEY_MAX_LENGTH=128
IDEMPOTENCY_RETENTION=24h
IDEMPOTENCY_MAX_RECORDS=10000
IDEMPOTENCY_CLEANUP_INTERVAL=5m

MANAGEMENT_PORT=8082
MANAGEMENT_ALLOWED_CIDRS=
METRICS_ENABLED=true
OPENAPI_ENABLED=true
SWAGGER_UI_ENABLED=true

LOG_LEVEL_ROOT=INFO
LOG_FORMAT=text
LOG_INCLUDE_ORDER_IDS=false
LOG_INCLUDE_OWNER_REFERENCES=false

SENTRA_SERVICES_NETWORK=sentra-gateway_services
ORDER_IMAGE=localhost/sentra/order-service:1.0.0
```

## Startup Validation

The application must fail before accepting traffic when:

1. production-like profiles enable seed data;
2. gateway context enforcement is disabled outside local/test;
3. no approved gateway peer is configured in production-like deployment;
4. route IDs differ from the documented operation catalog;
5. Swagger UI is exposed unsafely;
6. management CIDRs are absent while management shares an exposed listener;
7. request, page, item, or idempotency limits are inconsistent;
8. an unsupported repository mode is selected;
9. logging defaults expose owner or order identifiers;
10. an upstream create retry policy is enabled while idempotency is disabled.

Invalid configuration is a startup error, not a warning.

## Restart Behavior

| Setting | Runtime refresh |
| --- | --- |
| Port, profile, repository mode | Restart required |
| Gateway peers/routes | Restart required |
| Limits and idempotency policy | Restart required |
| Management/OpenAPI exposure | Restart required |
| Log level | Runtime change only through a protected mechanism; otherwise restart |

The baseline service does not implement dynamic configuration refresh.
