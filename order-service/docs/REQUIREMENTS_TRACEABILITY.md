# Order Service Requirements Traceability

**Version:** 1.0.0  
**Date:** June 15, 2026  
**Current state:** Implemented; service-local automated and Podman verification complete

## Status Definitions

- **Specified:** required behavior is defined in this documentation.
- **Implemented:** executable production code/configuration exists.
- **Verified:** automated or completed runtime evidence exists.
- **Planned:** required work has no implementation evidence.
- **Not applicable:** outside the approved order-service boundary.

The service-local implementation, Maven tests, Newman collection, image build,
container hardening, health, and graceful restart are verified. Matrix rows that
also require gateway JWT, shared-platform, scan, dashboard, or alert evidence
remain at their release-wide status until that external evidence is available.

## Source Requirements

| Source | Requirement |
| --- | --- |
| Microservices 4 | `order-service`, internal port `8082`, internal exposure |
| Microservices 6.2 | subject/tenant-scoped reads, create, admin role, idempotency |
| Microservices 7 | UTF-8 JSON, RFC 3339, UUIDs, pagination, errors, trusted headers |
| Microservices 11-13 | failure, resilience, logs, metrics, and health |
| Microservices 14-20 | deployment, configuration, tests, onboarding, definition of done |
| SRS `IF-HTTP-*` | JSON, limits, correlation, media-type behavior |
| SRS `IF-DS-*` | sanitized bounded trusted headers and route-specific resilience |
| SRS `IF-OBS-*` | health, Prometheus, restricted management, structured logs |
| SRS `FR-AUTHZ-*` | required roles/scopes and deny-by-default |
| SRS `FR-RES-004` | no retry for non-idempotent requests without explicit contract |
| SRS `FR-ERR-*` | stable safe error responses |
| SRS `FR-SVC-002` | subject-scoped order read and create routes |
| SRS `FR-SVC-005/006/008` | health, metrics, correlation, trusted headers, no direct exposure |
| SRS `NFR-SEC-*` | external config, redaction, header and management security |
| SRS `NFR-REL-004` | graceful shutdown |
| SRS `NFR-SCL-005` | bounded metric cardinality |
| SRS `NFR-OBS-*` | correlation, runtime visibility, dashboards, alerts |
| SRS `NFR-MNT-003/005/007` | reproducible build, P0 evidence, contract tests |
| SRS `NFR-USE-001/002/004/005` | safe errors, OpenAPI, compatibility, simple startup |
| SRS `BR-001/002/008/010` | internal-only, deny-by-default, redaction, evidence |
| Master TODO `ARC-005` | create order-service module |
| Master TODO `PLT-017` | skeleton and health endpoint |
| Master TODO `PLT-020` | deterministic sample data |
| Master TODO `RES-002/004` | explicit idempotency and retry-amplification control |
| Master TODO `RES-012` | read/create endpoints with role/scope examples |
| Master TODO `OBS-*` | logs, metrics, management protection, labels, smoke test |
| Master TODO `TST-*` | contract, security, malformed/oversized, Compose, scans |
| Master TODO `OPS-*` | hardened image, shutdown, startup, docs, release checklist |

## Contract Decisions

| Decision | Resolution | Reason |
| --- | --- | --- |
| External route prefix | `/api/v1/orders` | SRS and microservices contract outrank older generic example |
| Internal route prefix | `/internal/v1/orders` | Shared downstream convention |
| Ownership | `(tenantId, ownerSubject)` | Explicit tenant/subject scope |
| Foreign order behavior | Same `404` as unknown | Prevent identifier enumeration |
| Admin identity | actor `USER` plus `ORDER_ADMIN` | Trusted actor vocabulary has no separate ADMIN type |
| Pagination | zero-based page, max size 100, fixed order | Bounded and deterministic |
| Create pricing | absent | Pricing/payment are unrelated to authorization demonstration |
| Create status | service-controlled `CREATED` | Prevent client-controlled workflow state |
| Idempotency header | `Idempotency-Key` | Standard explicit request contract |
| Replay status | original `201` and representation | Stable repeat of original result |
| Unknown JSON fields | rejected | Security-focused strict baseline |
| Baseline storage | deterministic memory repository | Source permits memory or own DB; no DB design approved |

## Requirement Matrix

| Requirement | Design evidence | Required verification | Status |
| --- | --- | --- | --- |
| `FR-SVC-002` | Four routes in `API_CONTRACT.md` | HTTP and gateway end-to-end tests | Specified |
| `FR-SVC-005` | Management and metrics contracts | Actuator/Prometheus assertions | Specified |
| `FR-SVC-006` | Trusted context and response request ID | Header and correlation tests | Specified |
| `FR-SVC-008`, `BR-001` | Internal-only base topology | Compose/network inspection | Specified |
| `IF-HTTP-002` | UTF-8 JSON rules | Content-type/encoding tests | Specified |
| `IF-HTTP-004` | Header/body/domain limits | Boundary and oversized tests | Specified |
| `IF-HTTP-005` | Request-ID lifecycle | Response/log correlation | Specified |
| `IF-HTTP-007` | JSON-only create | `415` test | Specified |
| `IF-DS-002/003` | Trusted header contract | Duplicate/spoof/provenance tests | Specified |
| `IF-DS-005/006` | Route resilience and retry policy | Gateway contract/failure tests | Specified |
| `IF-OBS-001/002` | Liveness/readiness | Runtime HTTP tests | Specified |
| `IF-OBS-003` | Prometheus endpoint and domain metrics | Metrics assertions | Specified |
| `IF-OBS-004` | Restricted management topology | Network/config inspection | Specified |
| `IF-OBS-005` | Structured redacted logs | Log inspection | Specified |
| `FR-AUTHZ-002/003/004` | Actor/scope/role/route checks | Allow/deny matrix | Specified |
| `FR-RES-004` | Idempotency and no-default-retry contract | Replay/conflict/concurrency tests | Specified |
| `FR-ERR-001/002/003/004` | `ORD_*` error catalog | Exact schema/status/leakage tests | Specified |
| `NFR-SEC-002/003` | Environment catalog and redaction | Secret/log scan | Specified |
| `NFR-SEC-005` | Reserved-header/provenance rules | Direct-bypass and spoof tests | Specified |
| `NFR-SEC-006` | Management restriction | Deployment inspection | Specified |
| `NFR-SEC-007` | Required CI scans | Scan pipeline evidence | Planned |
| `NFR-REL-004` | Graceful shutdown runbook | Container stop exercise | Specified |
| `NFR-SCL-005`, `BR-008` | Metric label allowlist | Prometheus inspection | Specified |
| `NFR-OBS-001/002` | Logs and metrics catalog | Runtime smoke test | Specified |
| `NFR-OBS-003/004/005` | Suggested dashboards/alerts/runbooks | Provisioning and alert smoke | Planned |
| `NFR-MNT-003` | Required wrapper/build commands | Clean host and container build | Specified |
| `NFR-MNT-005` | This matrix | Evidence for every P0 row | Specified |
| `NFR-MNT-007` | OpenAPI/gateway contract tests | Compatibility suite | Specified |
| `NFR-USE-001` | Safe errors | Leakage assertions | Specified |
| `NFR-USE-002` | OpenAPI requirements | Schema/path/status assertions | Specified |
| `NFR-USE-004` | Versioned external contract | Compatibility review | Specified |
| `NFR-USE-005` | Compose startup command | Clean-environment smoke | Specified |
| `PLT-017` | Target skeleton/runtime layout | Application startup test | Planned |
| `PLT-020` | Deterministic dataset | Reset/restart tests | Specified |
| `RES-012` | Read/create/admin route design | Complete route test suite | Specified |

## Minimum Test Inventory

| Test area | Required coverage |
| --- | --- |
| User list | owner/tenant filtering, pagination, status filter, stable ordering |
| User single read | owned success, unknown/foreign subject/foreign tenant all safe |
| Create | validation, server-controlled fields, `201`, `Location`, no-store |
| Idempotency | no key, first key, replay, conflict, expiry, capacity, concurrency |
| Admin list | role allow/deny, filters, pagination, owner representation |
| Trusted context | missing, duplicate, malformed, contradictory, wrong peer |
| Authorization | actor, scope, role, and exact route ID |
| Errors | every `ORD_*` status/schema and no leakage |
| Limits | body, item count, SKU, quantity, page, header |
| Observability | request ID, logs, metrics, forbidden labels |
| Management | liveness, readiness, restricted metrics/OpenAPI |
| OpenAPI | paths, headers, schemas, examples, responses |
| Gateway contract | rewrites, header sanitation, JWT removal, retry policy |
| Container | build, non-root, read-only, internal port, graceful stop |

## Broader Capabilities Not Claimed

| Capability | Status | Required future work |
| --- | --- | --- |
| Durable database | Planned | Approved schema, migrations, credentials, tests, backup/restore |
| Multi-instance write consistency | Planned | Shared repository and idempotency uniqueness |
| Pricing/tax/discount | Not applicable | New domain requirements |
| Payment | Not applicable | Payment-service integration contract |
| Inventory | Not applicable | Inventory service and reservation workflow |
| Update/cancel/refund | Not applicable | New APIs, state machine, authorization, audit |
| Fulfillment/shipping | Not applicable | New service boundary |
| Event streaming | Planned only if approved | Event schema, broker, delivery semantics |
| Workload mTLS | Planned | Certificates, identity, rotation, tests |
| Performance certification | Planned | Approved workload and k6 evidence |

## Release Rule

A requirement moves from:

1. **Specified** to **Implemented** only when executable code and configuration
   exist;
2. **Implemented** to **Verified** only when the required automated/runtime
   evidence passes; and
3. **Verified** to release-ready only when the release checklist, gateway
   end-to-end tests, scans, and operational exercises pass.

Documentation alone is not implementation evidence.
