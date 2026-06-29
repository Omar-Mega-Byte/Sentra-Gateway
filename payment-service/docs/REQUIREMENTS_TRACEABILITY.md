# Payment Service Requirements Traceability

**Version:** 1.0.0  
**Date:** June 16, 2026  
**Current state:** Baseline internal service implemented with automated direct-service verification;
gateway end-to-end security verification requires a running gateway stack

## Status Definitions

- **Specified:** required behavior is defined in this documentation.
- **Implemented:** executable production code/configuration exists.
- **Verified:** automated or completed runtime evidence exists.
- **Not applicable:** outside the approved payment-service boundary.

Capabilities owned entirely by `payment-service` now have executable
implementation evidence. Gateway-owned API-key, HMAC, nonce, replay, rate-limit,
and retry behavior remains verified through gateway-service evidence.

## Source Requirements

| Source | Requirement |
| --- | --- |
| Microservices 4 | `payment-service`, internal port `8083`, internal exposure |
| Microservices 6.3 | partner API key, signing, replay, strict limits, high-risk operations |
| Microservices 7 | UTF-8 JSON, RFC 3339, UUID/opaque IDs, errors, trusted headers |
| Microservices 8.2 | signed partner payment flow |
| Microservices 9.1/9.2/9.4 | partner auth matrix, canonical signing, secret handling |
| Microservices 11-13 | failure policy, no silent enforcement downgrade, logs, metrics, health |
| SRS `FR-KEY-*` | gateway API-key validation and no raw key leakage |
| SRS `FR-SIG-*` | gateway signing, canonicalization, nonce, replay, fail-closed behavior |
| SRS `FR-AUTHZ-*` | route scopes and normalized authorized identity context |
| SRS `FR-RES-004` | no retry for non-idempotent requests without explicit contract |
| SRS `FR-SVC-003` | payment service API-key and signature-protected routes |
| SRS `FR-SVC-005/006/008` | health, metrics, trusted headers, no direct exposure |
| SRS `NFR-SEC-*` | external config, redaction, header and management security |
| SRS `NFR-SCL-005` | bounded metric labels |
| SRS `NFR-MNT-003/005/007` | reproducible build, P0 evidence, contract tests |
| SRS `NFR-USE-001/002/004/005` | safe errors, OpenAPI, compatibility, startup |
| SRS Appendix B | replay rejection, body mutation, Redis outage, no POST retry |
| Master backlog `PLT-018` | payment-service skeleton and health endpoint |
| Master backlog `PLT-020` | deterministic sample data |
| Master backlog `SIG-001..015` | signing specification, tests, metrics, audit reason codes |
| Master backlog `RES-013/019` | partner endpoints requiring key/signature; POST no retry |
| Master backlog `OBS-*` | logs, metrics, management protection, forbidden labels |
| Master backlog `TST-*` | security, replay, malformed, outage, Compose, scans |
| Master backlog `OPS-*` | hardened image, shutdown, startup, docs, release checklist |

## Contract Decisions

| Decision | Resolution | Reason |
| --- | --- | --- |
| External route prefix | `/api/v1/partner/payments`, `/api/v1/partner/refunds` | explicit microservices contract outranks older generic route |
| Internal route prefix | `/internal/v1/payments`, `/internal/v1/refunds` | shared downstream convention |
| External auth | API key validated by gateway | source requirement |
| HMAC validation | gateway-owned | service receives only validated evidence |
| Replay protection | gateway-owned Redis nonce check | SRS and flow require gateway nonce storage |
| Service actor | `API_CLIENT` | trusted actor vocabulary |
| Mutation idempotency | required | prevents unsafe duplicate high-risk operations |
| Foreign payment behavior | same `404` as unknown | prevents cross-client enumeration |
| Payment amount | decimal string | avoids floating-point money errors |
| Baseline storage | deterministic memory repository | source permits memory or own DB; no DB design approved |

## Requirement Matrix

| Requirement | Design evidence | Required verification | Status |
| --- | --- | --- | --- |
| `FR-SVC-003` | Three partner routes in `API_CONTRACT.md` | Direct contract and gateway E2E tests | Specified |
| `FR-SVC-005` | Management and metrics contracts | Actuator/Prometheus assertions | Specified |
| `FR-SVC-006` | Trusted context and request ID | Header/correlation tests | Specified |
| `FR-SVC-008`, `BR-001` | Internal-only topology | Compose/network inspection | Specified |
| `FR-KEY-005/010` | Service requires trusted key/client context and rejects raw keys | Gateway E2E and downstream header tests | Specified |
| `FR-SIG-001..009` | Signature evidence contract and fail-closed behavior | Gateway signature/replay suite | Specified |
| `FR-AUTHZ-002/003/004/006` | Scope, route, and actor checks | Allow/deny matrix | Specified |
| `FR-RES-004` | Idempotency and no-default-retry runbook | Timeout/retry and idempotency tests | Specified |
| `IF-HTTP-002/004/005/007` | JSON, limits, request ID, media type | HTTP tests | Specified |
| `IF-DS-002/003/004` | Reserved-header removal and no raw credential propagation | Gateway/downstream contract tests | Specified |
| `IF-OBS-001/002/003/004/005` | Health, metrics, restricted management, logs | Runtime smoke and inspection | Specified |
| `NFR-SEC-002/003/005/006` | Env config, redaction, trusted headers, management restriction | Startup/security inspection | Specified |
| `NFR-SEC-007` | Required scans | CI scan evidence | Planned |
| `NFR-SCL-005`, `BR-008` | Metric label allowlist | Prometheus inspection | Specified |
| `NFR-MNT-003` | Required build commands | Clean host/container build | Specified |
| `NFR-MNT-005` | This matrix | P0 evidence | Specified |
| `NFR-MNT-007` | OpenAPI/gateway contract tests | Compatibility suite | Specified |
| `NFR-USE-001` | `PAY_*` errors | Leakage/status/schema tests | Specified |
| `NFR-USE-002` | OpenAPI requirements | Path/header/schema assertions | Specified |
| `NFR-USE-004` | Versioned external routes | Compatibility review | Specified |
| `NFR-USE-005` | Compose startup command | Clean-environment smoke | Specified |
| `PLT-018` | Skeleton/runtime target | Application startup test | Planned |
| `PLT-020` | Deterministic dataset | Reset/restart tests | Specified |
| `RES-013` | Payment/refund partner routes | Complete API tests | Specified |
| `RES-019` | POST payment not retried automatically | Gateway timeout scenario | Specified |

## Minimum Test Inventory

| Test area | Required coverage |
| --- | --- |
| Payment read | owned success, unknown and foreign-client safe `404` |
| Payment create | validation, signature evidence, idempotency, created response |
| Refund create | ownership, refundable amount, idempotency, conflict paths |
| Trusted context | missing, duplicate, malformed, wrong peer, raw credential leakage |
| Authorization | actor, client ID, key ID, scope, route ID |
| Signature contract | missing/false evidence direct; gateway HMAC/replay E2E |
| Money validation | amount format, scale, range, currency |
| Idempotency | missing key, replay, changed payload, expiry, capacity, concurrency |
| Errors | every `PAY_*` status/schema and no leakage |
| Observability | request ID, health, metrics, forbidden labels, redacted logs |
| OpenAPI | paths, headers, schemas, examples, responses |
| Container | non-root, read-only, internal port, graceful stop |
| Gateway E2E | key invalid/revoked/expired, signature mutation, stale timestamp, replay |

## Broader Capabilities Not Claimed

| Capability | Status | Required future work |
| --- | --- | --- |
| Real payment provider | Not applicable | Provider contract, credentials, sandbox, compliance |
| PCI card handling | Not applicable | Separate compliant architecture |
| Durable ledger | Planned only if approved | Accounting model and migrations |
| Settlement/capture/void | Not applicable | New APIs and state machine |
| Disputes/chargebacks | Not applicable | New workflow |
| Currency conversion | Not applicable | FX source and money policy |
| HSM/secret manager integration in service | Not applicable | Gateway security ownership |
| Multi-instance memory consistency | Planned | Shared repository/idempotency store |
| Workload mTLS | Planned | Certificates, identity, rotation, tests |
| Performance certification | Planned | Approved k6 evidence |

## Release Rule

A requirement moves:

1. from **Specified** to **Implemented** only when executable code and
   configuration exist;
2. from **Implemented** to **Verified** only when required automated/runtime
   evidence passes; and
3. to release-ready only when the release checklist, gateway E2E signature suite,
   scans, and operational exercises pass.

Documentation alone is not implementation evidence.
