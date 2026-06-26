# User Service Configuration

**Status:** Implemented in typed Spring configuration and environment-backed YAML  
**Target service:** `user-service` version `1.0.0`

## Precedence

The target Spring Boot implementation uses normal precedence: command-line
arguments, environment variables, profile-specific configuration, base
configuration, then code defaults.

Secrets and environment-specific values are injected. `.env` must not be
committed. `.env.example` contains placeholders only.

## Profiles

| Profile | Behavior |
| --- | --- |
| `local` | Deterministic in-memory profiles, local OpenAPI, direct developer access allowed only on loopback |
| `test` | Deterministic isolated repository and test-friendly management access |
| `prod` or production-like | Gateway-only application traffic, protected management endpoints, no development controls |

Production-like profiles shall fail startup when gateway provenance enforcement or
required repository configuration is incomplete.

## Application

| Variable | Default | Validation | Meaning |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | none | Required outside tests | Runtime profile |
| `SERVER_PORT` | `8081` | 1-65535 | Internal HTTP port |
| `SENTRA_ENVIRONMENT` | `local` | Nonblank token | Metric/log environment |
| `SENTRA_INSTANCE_ID` | random UUID | Nonblank | Instance identity |
| `SERVICE_NAME` | `user-service` | Fixed in production | Log and metric service name |
| `SHUTDOWN_TIMEOUT` | `20s` | Positive duration | Graceful shutdown budget |

## Repository

| Variable | Default | Validation | Meaning |
| --- | --- | --- | --- |
| `PROFILE_REPOSITORY_MODE` | `memory` | `memory` or approved durable mode | Profile storage |
| `PROFILE_SEED_ENABLED` | `true` in local/test | Boolean | Load deterministic sample profiles |
| `PROFILE_EMAIL_UNIQUE` | `true` | Boolean | Enforce normalized email uniqueness |

The initial demonstration contract requires only `memory`. Any durable mode must
add its complete connection, migration, pool, timeout, backup, and health
configuration before release.

## Trusted Gateway Context

| Variable | Default | Validation | Meaning |
| --- | --- | --- | --- |
| `GATEWAY_CONTEXT_REQUIRED` | `true` | Must be `true` in production-like profiles | Require trusted headers |
| `GATEWAY_ALLOWED_ROUTE_IDS` | `user-public-profile,user-profile-read,user-profile-update` | Nonempty list in production | Accepted route IDs |
| `GATEWAY_ALLOWED_PEERS` | empty | CIDR/IP list | Optional immediate-peer allowlist |
| `TRUSTED_HEADER_MAX_LENGTH` | `255` | 32-4096 | Generic trusted-header ceiling |
| `REQUEST_ID_MAX_LENGTH` | `128` | 16-512 | Correlation ID ceiling |

An IP allowlist alone is not workload identity. Production deployments should
also isolate the service network and use platform workload authentication or mTLS
where available.

When `GATEWAY_CONTEXT_REQUIRED=false`, only local/test use is permitted. This
setting must not cause the service to invent an authenticated subject.

## Request And Field Limits

| Variable | Default | Validation |
| --- | --- | --- |
| `MAX_REQUEST_BODY_BYTES` | `16384` | 1024-1048576 |
| `MAX_DISPLAY_NAME_LENGTH` | `100` | 1-200 |
| `MAX_BIO_LENGTH` | `500` | 0-5000 |
| `MAX_AVATAR_URL_LENGTH` | `2048` | 128-8192 |
| `MAX_EMAIL_LENGTH` | `254` | 64-320 |
| `MAX_LOCALE_LENGTH` | `35` | 2-64 |
| `MAX_TIMEZONE_LENGTH` | `64` | 16-128 |
| `MAX_ERROR_DETAILS` | `20` | 1-100 |

The gateway and ingress should enforce equal or stricter body/header ceilings.

## Public Profile Policy

| Variable | Default | Meaning |
| --- | --- | --- |
| `PUBLIC_PROFILE_ENABLED` | `true` | Enable the documented public lookup |
| `PUBLIC_PROFILE_CACHE_CONTROL` | `public, max-age=60` | Response cache policy |
| `PUBLIC_PROFILE_NOT_FOUND_CACHE_CONTROL` | `no-store` | Avoid durable negative caching |

Public fields are compile-time DTO allowlists, not configuration. Environment
configuration must never turn private fields public.

## Management And OpenAPI

| Variable | Default | Meaning |
| --- | --- | --- |
| `MANAGEMENT_PORT` | same as `SERVER_PORT` | Management HTTP port |
| `MANAGEMENT_ALLOWED_CIDRS` | empty | Operations-network allowlist |
| `OPENAPI_ENABLED` | `true` in local/test | Publish `/v3/api-docs` |
| `SWAGGER_UI_ENABLED` | `true` in local/test | Publish Swagger UI |
| `METRICS_ENABLED` | `true` | Publish Micrometer metrics |

Production should use a separate management port or network policy. Liveness and
readiness may be available to the orchestrator; detailed health, metrics, and docs
must be restricted.

## Logging

| Variable | Default | Meaning |
| --- | --- | --- |
| `LOG_LEVEL_ROOT` | `INFO` | Root log level |
| `LOG_FORMAT` | `json` outside local | Structured log format |
| `LOG_INCLUDE_PROFILE_IDS` | `false` | Must remain false in production |

Authorization, cookies, tokens, trusted subjects, emails, request/response bodies,
and raw profile values are prohibited from logs regardless of log level.

## `.env.example`

The complete maintained template is `../.env.example`. The local working file
`../.env` is ignored by Git and contains no credentials.

`GATEWAY_ALLOWED_PEERS` accepts IP literals, CIDRs, and service names. Service
names are resolved once at startup and converted to immutable exact-address
matches.

## Startup Validation

Startup shall fail when:

- the active profile is missing in a non-test deployment;
- a production-like profile disables required gateway context;
- allowed route IDs are empty in a production-like profile;
- a numeric or duration limit is outside its documented range;
- repository mode is unsupported;
- deterministic seed data is enabled contrary to an approved production policy;
- detailed management endpoints are publicly exposed by configuration; or
- required durable-store configuration is incomplete.

These checks are implemented by `UserServiceProperties` and `StartupValidator`.
Only `memory` repository mode is accepted in release 1.0.0.

## Restart Behavior

All variables in this version are read at startup. Changes require an instance
restart. Profile updates are runtime data and do not require restart.

With the in-memory repository, every restart resets state to seed data when
seeding is enabled or to an empty repository when it is disabled.

## Secret Rules

- Do not add gateway JWT keys, API keys, or signing secrets to this service.
- Do not place real email addresses or credentials in seed data.
- Do not commit `.env`.
- Do not log injected secrets or complete environment dumps.
- Use secret files or a secret manager if a durable repository later requires
  credentials.
- Rotate secrets through documented overlap and rollback procedures.
