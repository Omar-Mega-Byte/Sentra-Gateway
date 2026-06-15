# User Service Requirements Traceability

**Date:** June 15, 2026  
**Target release:** `1.0.0-SNAPSHOT`  
**Current state:** Implemented; automated and container verification evidence recorded

## Status Definitions

- **Specified:** the final required behavior is documented.
- **Implemented:** production code and executable configuration exist.
- **Verified:** automated test or completed runtime verification exists.
- **Planned:** required by project documents but no implementation evidence exists.
- **Not applicable:** the requirement belongs to another service boundary.

The implementation evidence below refers to the source and executable checks in
this directory.

## Source Requirements

| Source | User-service requirement |
| --- | --- |
| Microservices documentation 6.1 | Public profile, authenticated `/me`, subject propagation, safe update |
| SRS `FR-SVC-001` | At least one public and one JWT-protected route |
| SRS `FR-SVC-005` | Health and Prometheus metrics |
| SRS `FR-SVC-006` | Echo request correlation and consume trusted identity headers |
| SRS `FR-SVC-007` | Development failure controls disabled outside local/test |
| SRS `FR-SVC-008` | No direct external access to application port |
| SRS `IF-DS-002/003` | Reserved headers sanitized by gateway; consume validated bounded values |
| SRS `NFR-SEC-001/002/003/005` | TLS boundary, external configuration, redaction, trusted-header security |
| SRS `NFR-OBS-001/002` | Correlatable responses and runtime metrics |
| SRS `NFR-MNT-003/005/007` | Reproducible build, P0 verification, contract testing |
| SRS `NFR-USE-001/002/004` | Safe errors, OpenAPI, compatible `/api/v1` contract |
| SRS `BR-001` | External clients do not directly reach downstream services |
| SRS `BR-008` | Audit/log/metric data avoids sensitive and high-cardinality values |
| SRS `BR-010` | Implementation claims must be linked to executable evidence |
| Master TODO `PLT-016` | Create user-service skeleton and health endpoint |
| Master TODO `PLT-020` | Deterministic sample data |
| Master TODO `RES-011` | Public profile and authenticated user scenarios |

## Implementation Matrix

| Capability | Required production evidence | Required verification evidence | Status |
| --- | --- | --- | --- |
| Reproducible Java service | Maven wrapper, `pom.xml`, application entry point | `clean verify` | Verified |
| Public profile endpoint | `ProfileController`, `PublicProfileResponse` | HTTP redaction tests | Verified |
| Authenticated self-read | `/me` controller and `TrustedContextResolver` | Trusted-header HTTP tests | Verified |
| Authenticated self-update | Validated PATCH service/repository path | HTTP, validation, and update tests | Verified |
| Subject-derived identity | `TrustedContextResolver` | Missing/duplicate/contradictory header tests | Verified |
| Actor-type enforcement | Trusted-context policy | Non-user actor denial test | Verified |
| Scope defense in depth | Escaped scope codec and required-scope check | Missing-scope denial tests | Verified |
| Public-field redaction | Dedicated allowlisted DTO | Exact four-field serialization test | Verified |
| Optimistic concurrency | Versioned model and synchronized atomic update | Success/stale/no-op tests | Verified |
| Deterministic sample data | `ProfileSeedData` | Repository reset tests | Verified |
| Stable errors | `ApiError`, `RestExceptionHandler` | Status/code/schema assertions | Verified |
| Request correlation | `RequestContextFilter` | Header/body correlation tests | Verified |
| Body/media limits | `RequestBodyLimitFilter` and controller consumes rule | 413 and 415 tests | Verified |
| Unknown-field rejection | Jackson strict deserialization | Unknown/immutable field tests | Verified |
| Liveness/readiness | Actuator probes and repository health indicator | Health HTTP tests | Verified |
| Prometheus metrics | Actuator/Micrometer configuration | Prometheus assertion | Verified |
| OpenAPI | Springdoc configuration and annotations | Required path/header/response assertions | Verified |
| Structured redacted logs | Normalized operation logging without profile values | Source review and runtime log inspection | Verified |
| Graceful shutdown | Spring graceful shutdown configuration | Container stop exercise | Verified |
| Non-root container | `Containerfile` user 10001 | Image inspect/runtime test | Verified |
| Internal-only exposure | Base Compose plus loopback-only test override | Compose/runtime inspection | Verified |
| Production startup validation | Typed validated properties and `StartupValidator` | Unsafe-config unit tests | Verified |
| Gateway contract | Shared documented header, route, scope, and error fixtures | Direct downstream contract tests | Verified |

Full JWT validation through gateway-service is not a user-service capability and
requires the separate identity-provider deployment.

## Requirement Matrix

| Requirement | Design evidence | Verification required | Status |
| --- | --- | --- | --- |
| `FR-SVC-001` | Three routes in `API_CONTRACT.md` | Public and trusted-context service E2E tests; JWT termination remains gateway scope | Verified |
| `FR-SVC-005` | Management contract | Health and Prometheus HTTP assertions | Verified |
| `FR-SVC-006` | Trusted headers and `X-Request-Id` contract | Contract and correlation assertions | Verified |
| `FR-SVC-007` | Profile rules in `CONFIGURATION.md` | Unsafe production-configuration tests | Verified |
| `FR-SVC-008` | Internal topology in operations guide | Base Compose and runtime network inspection | Verified |
| `IF-HTTP-002` | UTF-8 JSON common rules | Response content-type assertions | Verified |
| `IF-HTTP-004` | Request/field limit catalog | Boundary and oversized-body tests | Verified |
| `IF-HTTP-005` | Request ID lifecycle | Header/body correlation tests | Verified |
| `IF-HTTP-007` | PATCH media-type rule | Unsupported media-type test | Verified |
| `IF-DS-002/003` | Trusted-header contract | Duplicate, malformed, provenance, route, actor, and scope tests | Verified |
| `NFR-SEC-003` | Log and DTO redaction rules | Runtime log inspection and exact public serialization test | Verified |
| `NFR-SEC-005` | Gateway sanitation plus service provenance checks | Header-spoof and direct-bypass tests | Verified |
| `NFR-SCL-005` | Metric-label prohibition | Prometheus output assertions | Verified |
| `NFR-OBS-001` | Header/error request ID | Response correlation assertions and runtime log inspection | Verified |
| `NFR-OBS-002` | Monitoring catalog | Health and Prometheus assertions | Verified |
| `NFR-MNT-003` | Target Maven build gate | Host and Linux container `clean verify` | Verified |
| `NFR-MNT-005` | This matrix | P0 implementation and verification evidence above | Verified |
| `NFR-MNT-007` | Gateway contract requirement | Automated trusted-context compatibility tests | Verified |
| `NFR-USE-001` | Sanitized `USR_*` errors | Stable error-schema and leakage tests | Verified |
| `NFR-USE-002` | OpenAPI requirements | Generated OpenAPI path/schema assertions and live Swagger check | Verified |
| `NFR-USE-004` | Versioned external paths | Route catalog compatibility review and internal route tests | Verified |
| `BR-001` | Internal-only port policy | Base Compose inspection; local-only Postman override | Verified |
| `BR-008` | Sensitive/high-cardinality prohibitions | Runtime log and Prometheus inspection | Verified |
| `BR-010` | Evidence-linked implementation status | Maven, Newman, OpenAPI, and Podman evidence recorded | Verified |

## Minimum Test Inventory

| Verified test evidence | Primary scope |
| --- | --- |
| `UserServiceApplicationTest` | Public/private/update APIs, trusted context, errors, body/media limits, OpenAPI, health, and metrics |
| `InMemoryProfileRepositoryTest` | Lookup, uniqueness, atomic version increment, conflict, and reset |
| `ProfileValidatorTest` | Field normalization and documented validation boundaries |
| `StartupValidatorTest` | Production fail-closed configuration rules |
| `EscapedListCodecTest` | Gateway-compatible role/scope decoding |
| `NetworkMatcherTest` | Trusted peer CIDR, address, and service-name matching |
| Newman collection | Twelve live readiness, OpenAPI, success, rejection, update, conflict, and restore assertions |
| Podman runtime checks | Health, non-root user, read-only filesystem, security options, networking, and graceful shutdown |

## Broader Capabilities Not Claimed

| Capability | Status | Required future evidence |
| --- | --- | --- |
| Durable production database | Planned | Schema/migrations, repository tests, backup/restore |
| Multi-instance write consistency | Planned | Shared store and concurrency tests |
| Registration and account lifecycle | Not applicable to current scope | New requirements and APIs |
| Password/MFA/credential management | Not applicable | Identity-service design and security review |
| Administrative user management | Planned only if approved | Roles, APIs, audit, tests |
| Avatar upload/storage | Planned only if approved | Content security and storage design |
| Email verification | Planned only if approved | Token and notification workflow |
| Workload mTLS | Planned | Certificates, rotation, identity tests |
| Dashboards and alerts | Planned | Provisioned assets and smoke tests |
| Performance/SLO certification | Planned | Reproducible load and soak report |
| Production backup/restore | Planned | Timed restore exercise |

## Release Rule

A capability moves:

1. from Planned to Implemented only when application code and executable
   configuration exist;
2. from Implemented to Verified only when CI or documented runtime evidence
   executes successfully; and
3. to release-ready only when the operations checklist and gateway end-to-end
   contract both pass.

Documentation alone is not implementation evidence.
