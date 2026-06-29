# Payment Service Configuration

**Version:** 1.0.0  
**Status:** Implemented configuration contract for the baseline memory service

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

## Application

| Variable | Type | Default | Required | Sensitive | Restart |
| --- | --- | --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | profile list | none | Yes | No | Yes |
| `SERVER_PORT` | integer | `8083` | No | No | Yes |
| `SERVICE_NAME` | string | `payment-service` | No | No | Yes |
| `SENTRA_ENVIRONMENT` | enum/string | active profile | Production-like | No | Yes |
| `SENTRA_INSTANCE_ID` | string | generated | No | No | Yes |
| `SHUTDOWN_TIMEOUT` | duration | `20s` | No | No | Yes |

Validation:

- `SERVER_PORT` is `1-65535`.
- `SERVICE_NAME` must be exactly `payment-service` in production-like profiles.
- `SHUTDOWN_TIMEOUT` is `5s-120s`.

## Repository And Seed

| Variable | Type | Default | Required | Sensitive | Restart |
| --- | --- | --- | --- | --- | --- |
| `PAYMENT_REPOSITORY_MODE` | enum | `memory` | Yes | No | Yes |
| `PAYMENT_SEED_ENABLED` | boolean | `false` | No | No | Yes |
| `PAYMENT_SEED_DATASET` | string | `default` | No | No | Yes |
| `PAYMENT_MOCK_DECLINE_REFERENCES` | escaped list | empty | Local/test only | No | Yes |

Only `memory` is valid in the baseline. Any future durable mode requires a
separate database, migration, pool, timeout, backup, and recovery specification.

Production-like startup fails if seed or mock-decline controls are enabled.

## Gateway Context

| Variable | Type | Default | Required | Sensitive | Restart |
| --- | --- | --- | --- | --- | --- |
| `GATEWAY_CONTEXT_REQUIRED` | boolean | `true` | Yes | No | Yes |
| `GATEWAY_ALLOWED_ROUTE_IDS` | escaped list | payment route IDs | Yes | No | Yes |
| `GATEWAY_ALLOWED_PEERS` | CIDR/IP/service list | empty | Production-like | No | Yes |
| `TRUSTED_HEADER_MAX_LENGTH` | integer | `255` | No | No | Yes |
| `REQUEST_ID_MAX_LENGTH` | integer | `128` | No | No | Yes |
| `CLIENT_ID_MAX_LENGTH` | integer | `120` | No | No | Yes |
| `KEY_ID_MAX_LENGTH` | integer | `120` | No | No | Yes |

Default route IDs:

```text
partner-payment-read,partner-payment-create,partner-refund-create
```

Production-like startup fails when:

- trusted context enforcement is disabled;
- allowed gateway peers are empty;
- allowed route IDs are blank, duplicated, wildcarded, or unknown;
- route IDs do not match the API contract.

## Signature Evidence

| Variable | Type | Default | Validation |
| --- | --- | --- | --- |
| `SIGNATURE_EVIDENCE_REQUIRED_FOR_MUTATIONS` | boolean | `true` | Must be true in all profiles |
| `SIGNATURE_VERIFIED_HEADER` | string | `X-Sentra-Signature-Verified` | nonblank trusted header name |
| `SIGNATURE_KEY_ID_HEADER` | string | `X-Sentra-Signature-Key-Id` | nonblank trusted header name |
| `NONCE_STATUS_HEADER` | string | `X-Sentra-Nonce-Status` | nonblank trusted header name |
| `NONCE_ACCEPTED_VALUE` | string | `accepted` | nonblank |

The service does not validate HMAC signatures. It validates the gateway's
bounded evidence that HMAC and replay checks already succeeded. Missing or false
evidence on signed routes is a `403`.

## Request And Domain Limits

| Variable | Type | Default | Validation |
| --- | --- | ---: | --- |
| `MAX_REQUEST_BODY_BYTES` | integer | `16384` | `1024-1048576` |
| `MAX_MERCHANT_REFERENCE_LENGTH` | integer | `128` | `8-255` |
| `MAX_DESCRIPTION_LENGTH` | integer | `255` | `0-1000` |
| `MAX_AMOUNT` | decimal string | `10000.00` | positive, two decimals |
| `CURRENCY_ALLOWED_VALUES` | escaped list | empty means any uppercase 3-letter code | bounded |
| `MAX_ERROR_DETAILS` | integer | `20` | `1-100` |

Money parsing must use decimal arithmetic. Floating-point parsing is prohibited.

## Idempotency

| Variable | Type | Default | Validation |
| --- | --- | ---: | --- |
| `IDEMPOTENCY_ENABLED` | boolean | `true` | Must be true |
| `IDEMPOTENCY_KEY_REQUIRED_FOR_MUTATIONS` | boolean | `true` | Must be true |
| `IDEMPOTENCY_KEY_MAX_LENGTH` | integer | `128` | `16-255` |
| `IDEMPOTENCY_RETENTION` | duration | `24h` | `1m-7d` |
| `IDEMPOTENCY_MAX_RECORDS` | integer | `10000` | `100-1000000` |
| `IDEMPOTENCY_CLEANUP_INTERVAL` | duration | `5m` | `10s-1h` |

Reaching safe idempotency capacity returns
`503 PAY_IDEMPOTENCY_CAPACITY_EXCEEDED`; live safety records must not be evicted
to permit unsafe duplicates.

## Management And OpenAPI

| Variable | Type | Default | Required | Sensitive | Restart |
| --- | --- | --- | --- | --- | --- |
| `MANAGEMENT_PORT` | integer | `8083` | No | No | Yes |
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
| `LOG_INCLUDE_PAYMENT_IDS` | boolean | `false` | false by default |
| `LOG_INCLUDE_CLIENT_IDS` | boolean | `false` | prohibited by default |

Always redact:

- plaintext API keys and authorization headers;
- signatures, canonical strings, body hashes, nonces, and HMAC key IDs where
  they identify secrets;
- idempotency keys and request fingerprints;
- complete request/response bodies;
- merchant references unless explicitly approved for local diagnostics;
- provider simulation internals.

## Container And Network

| Variable | Type | Default | Purpose |
| --- | --- | --- | --- |
| `SENTRA_SERVICES_NETWORK` | string | `sentra-gateway_services` | Shared internal application network |
| `PAYMENT_IMAGE` | image reference | `localhost/sentra/payment-service:1.0.0` | Compose image override |

Base Compose exposes container port `8083` only to internal networks. A local
Postman override may publish `127.0.0.1:8083`.

## `.env.example`

The implementation must commit `.env.example` with safe local example values and
must not commit `.env`.

Expected local example:

```dotenv
SPRING_PROFILES_ACTIVE=local
SERVER_PORT=8083
SENTRA_ENVIRONMENT=local
SENTRA_INSTANCE_ID=payment-service-local
SERVICE_NAME=payment-service
SHUTDOWN_TIMEOUT=20s

PAYMENT_REPOSITORY_MODE=memory
PAYMENT_SEED_ENABLED=true
PAYMENT_SEED_DATASET=default
PAYMENT_MOCK_DECLINE_REFERENCES=

GATEWAY_CONTEXT_REQUIRED=true
GATEWAY_ALLOWED_ROUTE_IDS=partner-payment-read,partner-payment-create,partner-refund-create
GATEWAY_ALLOWED_PEERS=gateway-service
TRUSTED_HEADER_MAX_LENGTH=255
REQUEST_ID_MAX_LENGTH=128
CLIENT_ID_MAX_LENGTH=120
KEY_ID_MAX_LENGTH=120

SIGNATURE_EVIDENCE_REQUIRED_FOR_MUTATIONS=true
SIGNATURE_VERIFIED_HEADER=X-Sentra-Signature-Verified
SIGNATURE_KEY_ID_HEADER=X-Sentra-Signature-Key-Id
NONCE_STATUS_HEADER=X-Sentra-Nonce-Status
NONCE_ACCEPTED_VALUE=accepted

MAX_REQUEST_BODY_BYTES=16384
MAX_MERCHANT_REFERENCE_LENGTH=128
MAX_DESCRIPTION_LENGTH=255
MAX_AMOUNT=10000.00
CURRENCY_ALLOWED_VALUES=
MAX_ERROR_DETAILS=20

IDEMPOTENCY_ENABLED=true
IDEMPOTENCY_KEY_REQUIRED_FOR_MUTATIONS=true
IDEMPOTENCY_KEY_MAX_LENGTH=128
IDEMPOTENCY_RETENTION=24h
IDEMPOTENCY_MAX_RECORDS=10000
IDEMPOTENCY_CLEANUP_INTERVAL=5m

MANAGEMENT_PORT=8083
MANAGEMENT_ALLOWED_CIDRS=
METRICS_ENABLED=true
OPENAPI_ENABLED=true
SWAGGER_UI_ENABLED=true

LOG_LEVEL_ROOT=INFO
LOG_FORMAT=text
LOG_INCLUDE_PAYMENT_IDS=false
LOG_INCLUDE_CLIENT_IDS=false

SENTRA_SERVICES_NETWORK=sentra-gateway_services
PAYMENT_IMAGE=localhost/sentra/payment-service:1.0.0
```

## Startup Validation

Startup fails when:

1. production-like profiles enable seed or mock failure controls;
2. trusted context is disabled outside local/test;
3. no gateway peer is configured in production-like deployment;
4. mutation signature evidence is not required;
5. mutation idempotency is disabled or optional;
6. route IDs differ from the documented catalog;
7. request, money, header, or idempotency limits are inconsistent;
8. Swagger UI is exposed unsafely;
9. logging defaults expose payment/client identifiers;
10. an unsupported repository mode is selected.

Invalid configuration is a startup error, not a warning.

## Restart Behavior

| Setting | Runtime refresh |
| --- | --- |
| Port, profile, repository mode | Restart required |
| Gateway peers/routes | Restart required |
| Signature evidence header names | Restart required |
| Limits and idempotency policy | Restart required |
| Management/OpenAPI exposure | Restart required |
| Log level | Runtime only through protected mechanism; otherwise restart |

The baseline service does not implement dynamic configuration refresh.
