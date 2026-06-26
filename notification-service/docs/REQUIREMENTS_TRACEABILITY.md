# Notification Service Requirements Traceability

**Version:** 1.0.0  
**Date:** June 16, 2026  
**Current state:** Notification-service boundary implemented with local
automated verification; gateway end-to-end and scan evidence are external
release-environment gates

## Status Definitions

- **Specified:** required behavior is defined in this documentation.
- **Implemented:** executable production code/configuration exists.
- **Verified:** automated or completed runtime evidence exists.
- **Not applicable:** outside the approved notification-service boundary.
- **External release gate:** evidence must be produced from a gateway deployment
  or organization scanning pipeline outside this directory.

Capabilities implemented in this directory are represented by code, tests,
OpenAPI annotations, Podman assets, and Postman/Newman assets. Gateway-owned
resilience behavior is verified from the gateway deployment, not by direct
notification-service tests.

## Source Requirements

| Source | Requirement |
| --- | --- |
| Microservices 4 | `notification-service`, internal port `8084`, internal exposure |
| Microservices 6.4 | notification read, preferences mutation, admin test route, fault controls |
| Microservices 7 | UTF-8 JSON, RFC 3339, UUID/opaque IDs, errors, trusted headers |
| Microservices 11-13 | failure policy, resilience, logs, metrics, health |
| SRS `FR-RES-*` | timeout, retry, circuit, fallback, no unsafe mutation retry |
| SRS `FR-SVC-004` | configurable timeout/failure scenarios |
| SRS `FR-SVC-005/006/007/008` | health, metrics, trusted headers, dev controls disabled, no direct exposure |
| SRS `NFR-SEC-*` | config, redaction, header and management security |
| SRS `NFR-SCL-005` | bounded metric labels |
| SRS `NFR-MNT-003/005/007` | reproducible build, P0 evidence, contract tests |
| SRS `NFR-USE-001/002/004/005` | safe errors, OpenAPI, compatibility, startup |
| SRS `BR-001/008/009/010` | internal-only, redaction, no prod fault controls, evidence |
| Master backlog `PLT-019` | notification-service skeleton and health endpoint |
| Master backlog `PLT-020/021/022/023` | seed data, delay/failure simulation, request ID echo, graceful shutdown |
| Master backlog `RES-014/016/018` | timeout/retry routes, fault simulation, circuit/fallback tests |
| Master backlog `OBS-*` | logs, metrics, management protection, forbidden labels |
| Master backlog `TST-*` | security, malformed/oversized, outage, latency, restart, Compose, scans |
| Master backlog `OPS-*` | hardened image, shutdown, startup, docs, release checklist |

## Contract Decisions

| Decision | Resolution | Reason |
| --- | --- | --- |
| External route prefix | `/api/v1/notifications`, `/api/v1/notifications/preferences`, `/api/v1/admin/test-notification` | explicit microservices contract outranks older generic route |
| Internal route prefix | `/internal/v1/notifications`, `/internal/v1/preferences`, `/internal/v1/test` | shared downstream convention |
| Admin role | `NOTIFICATION_ADMIN` | least-privileged admin test capability |
| User actor | `USER` | gateway trusted actor vocabulary |
| Preference mutation | no automatic retry, optimistic version | prevent unsafe duplicate/lost update |
| Fault controls | local/test only | SRS `FR-SVC-007` and `BR-009` |
| Fallback owner | gateway | resilience policy is gateway-owned |
| Baseline storage | deterministic memory repository | source permits mock service state |

## Requirement Matrix

| Requirement | Design evidence | Required verification | Status |
| --- | --- | --- | --- |
| `FR-SVC-004` | Three routes and fault controls in `API_CONTRACT.md` | Direct service and Newman tests; gateway resilience is external | Verified / External release gate |
| `FR-SVC-005` | Management and metrics contracts | Actuator/Prometheus assertions | Verified |
| `FR-SVC-006` | Trusted context and request ID | Header/correlation tests | Verified |
| `FR-SVC-007`, `BR-009` | production fail-closed fault controls | Startup validation tests | Verified |
| `FR-SVC-008`, `BR-001` | internal-only topology | Compose/network inspection | Verified |
| `FR-RES-001..010` | route resilience intent and operations guide | Gateway timeout/retry/circuit/fallback suite | External release gate |
| `IF-HTTP-002/004/005/007` | JSON, limits, request ID, media type | HTTP tests | Verified |
| `IF-DS-002/003` | trusted header contract | Duplicate/spoof/provenance tests | Verified |
| `IF-OBS-001..005` | health, metrics, management, logs | Runtime smoke and inspection | Verified |
| `NFR-SEC-002/003/005/006` | env config, redaction, trusted headers, management restriction | Startup/security inspection | Verified |
| `NFR-SEC-007` | required scans | CI scan evidence | External release gate |
| `NFR-SCL-005`, `BR-008` | metric label allowlist | Prometheus inspection | Verified |
| `NFR-MNT-003` | required build commands | Clean host/container build | Verified |
| `NFR-MNT-005` | this matrix | P0 evidence | Verified |
| `NFR-MNT-007` | OpenAPI/gateway contract tests | OpenAPI assertions; gateway compatibility is external | Verified / External release gate |
| `NFR-USE-001` | `NTF_*` errors | leakage/status/schema tests | Verified |
| `NFR-USE-002` | OpenAPI requirements | path/header/schema assertions | Verified |
| `NFR-USE-004` | versioned external routes | compatibility review | External release gate |
| `NFR-USE-005` | Compose startup command | clean-environment smoke | Verified |
| `PLT-019` | skeleton/runtime target | application startup test | Verified |
| `PLT-020/021/022/023` | seed, faults, request ID, graceful shutdown | reset/fault/shutdown tests | Verified |
| `RES-014` | timeout/retry-suitable endpoints | gateway route tests | External release gate |
| `RES-016` | configurable delay/failure/malformed/disconnect | local/test fault tests | Verified |
| `RES-018` | circuit/fallback tests | gateway resilience E2E | External release gate |

## Minimum Test Inventory

| Test area | Required coverage |
| --- | --- |
| Notification list | owned success, tenant/subject isolation, filters, pagination |
| Preferences | success, validation, version conflict, no automatic retry |
| Admin test | role allow/deny, success, delay, failure, malformed, disconnect |
| Fault controls | local/test enabled, production-like startup rejection |
| Trusted context | missing, duplicate, malformed, wrong peer, wrong route |
| Authorization | actor, scope, role |
| Errors | every `NTF_*` status/schema and no leakage |
| Observability | request ID, health, metrics, forbidden labels, redacted logs |
| OpenAPI | paths, headers, schemas, examples, responses |
| Container | non-root, read-only, internal port, graceful stop |
| Gateway resilience | timeout, retry, circuit open, half-open, fallback |

## Broader Capabilities Not Claimed

| Capability | Status | Out-of-scope action if later approved |
| --- | --- | --- |
| Real notification delivery | Not applicable | Provider contracts and credentials |
| Durable queue/broker | Not claimed | Queue semantics and retry scheduler |
| Templates/localization | Not applicable | Template model and rendering rules |
| Device/token management | Not applicable | User-device service contract |
| Production provider failover | Not applicable | Provider abstraction and SLO design |
| Workload mTLS | External release gate | Certificates, identity, rotation, tests |
| Performance certification | External release gate | Approved k6 evidence |

## Release Rule

A requirement moves:

1. from **Specified** to **Implemented** only when executable code and
   configuration exist;
2. from **Implemented** to **Verified** only when required automated/runtime
   evidence passes; and
3. to release-ready only when the notification-service release checklist and
   any external gateway/scanner gates required by the broader release pass.

Documentation alone is not implementation evidence.
