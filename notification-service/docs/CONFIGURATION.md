# Notification Service Configuration

**Version:** 1.0.0  
**Status:** Implemented configuration contract

## Precedence

Application configuration follows Spring Boot precedence:

1. command-line arguments;
2. environment variables;
3. profile-specific application configuration;
4. base application configuration;
5. code defaults.

`.env` is consumed by Podman Compose and by `scripts/run-local.ps1`. The local
runner preserves explicit process environment variables, then loads `.env`,
then applies local-safe defaults.

## Profiles

| Profile | Purpose | Required behavior |
| --- | --- | --- |
| `local` | Developer JVM/Podman and Postman | Seed data, OpenAPI, Swagger UI, fault controls allowed |
| `test` | Automated verification | Deterministic data and fault scenarios |
| `prod` | Production-like deployment | No seed data, no public Swagger, no fault controls |

## Application

| Variable | Type | Default | Required | Sensitive | Restart |
| --- | --- | --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | profile list | none | Yes | No | Yes |
| `SERVER_PORT` | integer | `8084` | No | No | Yes |
| `SERVICE_NAME` | string | `notification-service` | No | No | Yes |
| `SENTRA_ENVIRONMENT` | enum/string | active profile | Production-like | No | Yes |
| `SENTRA_INSTANCE_ID` | string | generated | No | No | Yes |
| `SHUTDOWN_TIMEOUT` | duration | `20s` | No | No | Yes |

## Repository And Seed

| Variable | Type | Default | Required | Sensitive | Restart |
| --- | --- | --- | --- | --- | --- |
| `NOTIFICATION_REPOSITORY_MODE` | enum | `memory` | Yes | No | Yes |
| `NOTIFICATION_SEED_ENABLED` | boolean | `false` | No | No | Yes |
| `NOTIFICATION_SEED_DATASET` | string | `default` | No | No | Yes |

Only `memory` is valid in the baseline. Production-like startup fails when seed
data is enabled.

## Gateway Context

| Variable | Type | Default | Required | Sensitive | Restart |
| --- | --- | --- | --- | --- | --- |
| `GATEWAY_CONTEXT_REQUIRED` | boolean | `true` | Yes | No | Yes |
| `GATEWAY_ALLOWED_ROUTE_IDS` | escaped list | notification route IDs | Yes | No | Yes |
| `GATEWAY_ALLOWED_PEERS` | CIDR/IP/service list | empty | Production-like | No | Yes |
| `TRUSTED_HEADER_MAX_LENGTH` | integer | `255` | No | No | Yes |
| `REQUEST_ID_MAX_LENGTH` | integer | `128` | No | No | Yes |
| `SUBJECT_MAX_LENGTH` | integer | `255` | No | No | Yes |
| `TENANT_ID_MAX_LENGTH` | integer | `128` | No | No | Yes |

Default route IDs:

```text
notifications-list,notification-preferences-update,admin-test-notification
```

Production-like startup fails when context enforcement is disabled, gateway
peers are empty, or route IDs are blank, duplicated, wildcarded, or unknown.

## Request And Domain Limits

| Variable | Type | Default | Validation |
| --- | --- | ---: | --- |
| `MAX_REQUEST_BODY_BYTES` | integer | `16384` | `1024-1048576` |
| `DEFAULT_PAGE_SIZE` | integer | `20` | `1-MAX_PAGE_SIZE` |
| `MAX_PAGE_SIZE` | integer | `100` | `1-500` |
| `MAX_PAGE_NUMBER` | integer | `10000` | `100-1000000` |
| `MAX_TITLE_LENGTH` | integer | `120` | `1-500` |
| `MAX_MESSAGE_LENGTH` | integer | `1000` | `1-5000` |
| `MAX_RECIPIENT_REFERENCE_LENGTH` | integer | `128` | `1-255` |
| `MAX_ERROR_DETAILS` | integer | `20` | `1-100` |

## Fault Controls

| Variable | Type | Default | Validation |
| --- | --- | ---: | --- |
| `FAULT_CONTROLS_ENABLED` | boolean | `false` | false outside local/test |
| `FAULT_ALLOW_DELAY` | boolean | `true` | local/test only |
| `FAULT_ALLOW_STATUS` | boolean | `true` | local/test only |
| `FAULT_ALLOW_MALFORMED` | boolean | `true` | local/test only |
| `FAULT_ALLOW_DISCONNECT` | boolean | `true` | local/test only |
| `FAULT_MAX_DELAY_MS` | integer | `5000` | `0-30000` |
| `FAULT_ALLOWED_STATUSES` | list | `500,502,503,504` | 4xx/5xx only |
| `FAULT_FAIL_ONCE_CACHE_SIZE` | integer | `1000` | `0-100000` |

Production-like startup fails if `FAULT_CONTROLS_ENABLED=true` or any fault
allow flag is true. Fault controls are for local/test resilience verification
only.

## Management And OpenAPI

| Variable | Type | Default | Required | Sensitive | Restart |
| --- | --- | --- | --- | --- | --- |
| `MANAGEMENT_PORT` | integer | `8084` | No | No | Yes |
| `MANAGEMENT_ALLOWED_CIDRS` | CIDR list | empty | Production-like | No | Yes |
| `METRICS_ENABLED` | boolean | `true` | Yes | No | Yes |
| `OPENAPI_ENABLED` | boolean | `false` | No | No | Yes |
| `SWAGGER_UI_ENABLED` | boolean | `false` | No | No | Yes |

Local/test may enable OpenAPI and Swagger UI. Production-like startup fails if
Swagger UI is enabled without protected-operations access.

Exposed Actuator endpoints are limited to:

```text
health,info,prometheus,metrics
```

## Logging

| Variable | Type | Default | Rule |
| --- | --- | --- | --- |
| `LOG_LEVEL_ROOT` | level | `INFO` | Production must not default to `DEBUG` or `TRACE` |
| `LOG_FORMAT` | enum | `json` in prod | Structured machine-readable output |
| `LOG_INCLUDE_NOTIFICATION_IDS` | boolean | `false` | false by default |
| `LOG_INCLUDE_OWNER_REFERENCES` | boolean | `false` | prohibited by default |

Always redact authorization headers, cookies, subjects, tenants, message bodies,
titles, recipient references, roles, scopes, and raw trusted headers.

## Container And Network

| Variable | Type | Default | Purpose |
| --- | --- | --- | --- |
| `SENTRA_SERVICES_NETWORK` | string | `sentra-gateway_services` | Shared internal application network |
| `NOTIFICATION_IMAGE` | image reference | `localhost/sentra/notification-service:1.0.0` | Compose image override |

Base Compose exposes container port `8084` only to internal networks. A local
Postman override may publish `127.0.0.1:8084`.

## `.env.example`

Expected local example:

```dotenv
SPRING_PROFILES_ACTIVE=local
SERVER_PORT=8084
SENTRA_ENVIRONMENT=local
SENTRA_INSTANCE_ID=notification-service-local
SERVICE_NAME=notification-service
SHUTDOWN_TIMEOUT=20s

NOTIFICATION_REPOSITORY_MODE=memory
NOTIFICATION_SEED_ENABLED=true
NOTIFICATION_SEED_DATASET=default

GATEWAY_CONTEXT_REQUIRED=true
GATEWAY_ALLOWED_ROUTE_IDS=notifications-list,notification-preferences-update,admin-test-notification
GATEWAY_ALLOWED_PEERS=gateway-service
TRUSTED_HEADER_MAX_LENGTH=255
REQUEST_ID_MAX_LENGTH=128
SUBJECT_MAX_LENGTH=255
TENANT_ID_MAX_LENGTH=128

MAX_REQUEST_BODY_BYTES=16384
DEFAULT_PAGE_SIZE=20
MAX_PAGE_SIZE=100
MAX_PAGE_NUMBER=10000
MAX_TITLE_LENGTH=120
MAX_MESSAGE_LENGTH=1000
MAX_RECIPIENT_REFERENCE_LENGTH=128
MAX_ERROR_DETAILS=20

FAULT_CONTROLS_ENABLED=true
FAULT_ALLOW_DELAY=true
FAULT_ALLOW_STATUS=true
FAULT_ALLOW_MALFORMED=true
FAULT_ALLOW_DISCONNECT=true
FAULT_MAX_DELAY_MS=5000
FAULT_ALLOWED_STATUSES=500,502,503,504
FAULT_FAIL_ONCE_CACHE_SIZE=1000

MANAGEMENT_PORT=8084
MANAGEMENT_ALLOWED_CIDRS=
METRICS_ENABLED=true
OPENAPI_ENABLED=true
SWAGGER_UI_ENABLED=true

LOG_LEVEL_ROOT=INFO
LOG_FORMAT=text
LOG_INCLUDE_NOTIFICATION_IDS=false
LOG_INCLUDE_OWNER_REFERENCES=false

SENTRA_SERVICES_NETWORK=sentra-gateway_services
NOTIFICATION_IMAGE=localhost/sentra/notification-service:1.0.0
```

## Startup Validation

Startup fails when:

1. production-like profiles enable seed data;
2. production-like profiles enable fault controls;
3. trusted gateway context is disabled outside local/test;
4. no gateway peer is configured in production-like deployment;
5. route IDs differ from the documented catalog;
6. request, page, message, or fault limits are inconsistent;
7. Swagger UI is exposed unsafely;
8. logging defaults expose notification or owner identifiers;
9. an unsupported repository mode is selected.

Invalid configuration is a startup error, not a warning.

## Restart Behavior

| Setting | Runtime refresh |
| --- | --- |
| Port, profile, repository mode | Restart required |
| Gateway peers/routes | Restart required |
| Fault-control policy | Restart required |
| Limits | Restart required |
| Management/OpenAPI exposure | Restart required |
| Log level | Runtime only through protected mechanism; otherwise restart |

The baseline service does not implement dynamic configuration refresh.
