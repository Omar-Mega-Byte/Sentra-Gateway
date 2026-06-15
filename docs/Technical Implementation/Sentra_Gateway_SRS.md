# Software Requirements Specification

# Sentra Gateway

**Enterprise API Security and Observability Gateway**  
**Version:** 1.0  
**Date:** June 14, 2026  
**Status:** Approved planning baseline, implementation not yet verified  
**Owner:** Omar Ahmed  
**Related documents:** [Technical Documentation](Sentra_Gateway_Technical_Documentation.md), [Microservices Documentation](Sentra_Gateway_Microservices_Documentation.md), [Master TODO](Sentra_Gateway_Master_TODO.md)

## 1. Introduction

### 1.1 Purpose

This Software Requirements Specification defines the required behavior, interfaces, data, quality attributes, constraints, and acceptance conditions for Sentra Gateway. It is written so requirements can be traced to architecture, implementation tasks, automated tests, and release evidence.

The keywords **shall**, **should**, and **may** mean mandatory, recommended, and optional respectively.

### 1.2 Product Scope

Sentra Gateway is a centralized reactive API gateway for microservice environments. It routes client requests while enforcing authentication, authorization, API-key lifecycle, request signing, replay prevention, IP policy, rate limiting, risk rules, audit logging, observability, and resilience.

The baseline demonstration environment contains:

- One Sentra Gateway service.
- User, order, payment, and notification mock services.
- PostgreSQL for durable policy and audit data.
- Redis for distributed fast-changing state.
- Prometheus and Grafana for operational visibility.

### 1.3 Intended Audience

Product owner, backend developers, security reviewers, QA engineers, operators, API client developers, project evaluators, and future maintainers.

### 1.4 Definitions

| Term | Definition |
| --- | --- |
| Route | Match conditions, target, security metadata, and resilience policy used to forward a request. |
| Subject | Authenticated user, API client, tenant, or other rate-limit identity. |
| Trusted header | Header created by the gateway after validation and accepted only on internal traffic. |
| Decision | Final allow, deny, throttle, fallback, or error outcome produced by the gateway. |
| Replay window | Period during which a signing nonce must remain unique. |
| Admin API | Protected `/api/v1/admin` interface for gateway configuration and audit access. |

## 2. Overall Description

### 2.1 Product Perspective

Sentra Gateway sits between untrusted clients and internal services. It is both a reverse proxy and policy enforcement point. It depends on an external JWT issuer/JWK endpoint, PostgreSQL, Redis, and downstream HTTP services.

### 2.2 Product Functions

- Dynamic routing and route administration.
- JWT and API-key authentication.
- Role/scope authorization.
- HMAC request-signature and nonce validation.
- Client-IP resolution and CIDR policy.
- Distributed rate limiting and temporary blocking.
- Explainable request-risk evaluation.
- Structured audit records and admin-action history.
- Metrics, health, logs, and optional traces.
- Timeout, retry, circuit-breaker, and fallback handling.
- OpenAPI documentation and controlled error responses.

### 2.3 User Classes

| Class | Capabilities |
| --- | --- |
| Anonymous client | Access public routes only. |
| Authenticated user | Access routes permitted by JWT roles/scopes. |
| Partner client | Access permitted routes with API key and optional required signature. |
| Route administrator | Manage routes and resilience metadata. |
| Security administrator | Manage clients, keys, permissions, IP, rate, and risk policies. |
| Auditor | Search/export audit evidence without mutating policy. |
| Operator | Read health, metrics, dashboards, and operational state. |

### 2.4 Operating Environment

- Java 25 target runtime.
- Compatible Spring Boot 4 and Spring Cloud Gateway release train, verified before implementation.
- Linux containers for production-like operation.
- PostgreSQL and Redis versions pinned by deployment configuration.
- Modern HTTP/1.1 clients; HTTP/2 support may be enabled at the edge.
- Docker Compose for the required local demonstration.

### 2.5 Constraints

- The gateway shall use a non-blocking request-processing model.
- Blocking database work shall not execute on Netty event-loop threads.
- Downstream services shall not be publicly exposed in the default deployment.
- Secrets shall not be committed or stored in plaintext.
- Dynamic route targets shall be validated against SSRF policy.
- Security decisions shall be deterministic and deny by default.

### 2.6 Assumptions and Dependencies

- Production traffic is protected by TLS.
- The trusted reverse proxy configuration is known.
- JWT issuer metadata and keys are available or validly cached.
- PostgreSQL provides durable policy storage.
- Redis is shared by gateway instances where distributed correctness is required.
- System clocks are synchronized within the configured skew.

## 3. External Interface Requirements

### 3.1 Client HTTP Interface

- `IF-HTTP-001` The system shall accept HTTP requests through the configured edge listener.
- `IF-HTTP-002` The system shall support JSON UTF-8 request and response bodies.
- `IF-HTTP-003` The system shall preserve valid query parameters and path semantics through documented rewrites.
- `IF-HTTP-004` The system shall enforce configurable request-line, header, and body limits.
- `IF-HTTP-005` The system shall generate or validate a request correlation identifier.
- `IF-HTTP-006` The system shall return `Content-Type: application/json` for gateway-owned JSON errors.
- `IF-HTTP-007` The system shall reject unsupported media types where a route requires JSON.

### 3.2 Administration Interface

The first version shall use `/api/v1/admin`.

| Resource | Minimum operations |
| --- | --- |
| Routes | list, get, create, update, enable, disable, delete, validate |
| API clients | list, get, create, update, disable |
| API keys | issue, list metadata, rotate, revoke |
| Permissions | list, replace/update by route |
| Rate limits | list, create, update, disable, delete |
| IP rules | list, create, update, disable, delete |
| Risk rules | list, create, update, disable, delete |
| Audit events | paginated search, get, controlled export |

- `IF-ADM-001` Admin operations shall require a validated administrator identity.
- `IF-ADM-002` Each operation shall require the least-privileged administrative authority.
- `IF-ADM-003` Collection responses shall use bounded pagination.
- `IF-ADM-004` Mutable resources shall expose a version for optimistic concurrency.
- `IF-ADM-005` Sensitive create/rotate operations shall support idempotency.
- `IF-ADM-006` Every successful and failed mutation shall create an admin-action audit record.
- `IF-ADM-007` The system shall publish an OpenAPI description for admin interfaces.

### 3.3 Identity Provider Interface

- `IF-IDP-001` The gateway shall support JWT verification using trusted JWK material.
- `IF-IDP-002` Network retrieval shall use bounded connection and response timeouts.
- `IF-IDP-003` JWK material shall be cached according to a documented refresh policy.
- `IF-IDP-004` Key rotation shall not require gateway restart.
- `IF-IDP-005` Failure to obtain an uncached verification key shall deny authentication.

### 3.4 PostgreSQL Interface

- `IF-DB-001` All schema changes shall be managed by versioned Flyway migrations.
- `IF-DB-002` Database credentials shall be externally injected.
- `IF-DB-003` Connections and queries shall have bounded pools and timeouts.
- `IF-DB-004` Policy mutations and their admin audit shall be transactionally consistent.
- `IF-DB-005` The application shall detect incompatible migration state at startup.

### 3.5 Redis Interface

- `IF-REDIS-001` Rate-limit operations shall be atomic.
- `IF-REDIS-002` Replay nonces shall be stored with atomic create-if-absent semantics.
- `IF-REDIS-003` Temporary state shall have an explicit TTL.
- `IF-REDIS-004` Redis keys shall not contain credentials, raw tokens, or sensitive PII.
- `IF-REDIS-005` Redis operations shall have bounded timeouts.
- `IF-REDIS-006` Outage behavior shall be explicit by control and route category.

### 3.6 Downstream Service Interface

- `IF-DS-001` Downstream targets shall use approved schemes and destinations.
- `IF-DS-002` The gateway shall remove inbound reserved trusted headers.
- `IF-DS-003` The gateway shall add only validated, bounded trusted headers.
- `IF-DS-004` Raw API keys and external signatures shall not be forwarded.
- `IF-DS-005` Downstream calls shall use route-specific timeout policy.
- `IF-DS-006` Automatic retries shall be limited to explicitly eligible operations.

### 3.7 Observability Interface

- `IF-OBS-001` The gateway shall expose a liveness endpoint.
- `IF-OBS-002` The gateway shall expose a readiness endpoint.
- `IF-OBS-003` The gateway shall expose Prometheus-format metrics.
- `IF-OBS-004` Management endpoints shall be network-restricted or authenticated.
- `IF-OBS-005` Logs shall be machine-parseable structured records.

## 4. Functional Requirements

### 4.1 Routing

- `FR-RTE-001 P0` The system shall route requests using enabled route definitions.
- `FR-RTE-002 P0` A route shall support a stable ID, path predicate, allowed methods, target URI, priority, enabled state, and metadata.
- `FR-RTE-003 P0` The system shall select routes deterministically when multiple definitions could match.
- `FR-RTE-004 P0` The system shall reject ambiguous or prohibited route definitions during administration.
- `FR-RTE-005 P0` Administrators shall create, inspect, update, enable, disable, and delete routes.
- `FR-RTE-006 P0` Committed route changes shall become active without application restart.
- `FR-RTE-007 P0` Invalid refreshed configuration shall not replace the last known valid route set.
- `FR-RTE-008 P0` Disabled routes shall not forward traffic.
- `FR-RTE-009 P0` Unmatched traffic shall receive a stable not-found response.
- `FR-RTE-010 P0` Admin and management endpoints shall not be shadowed by dynamic routes.
- `FR-RTE-011 P1` The system shall provide route validation/dry-run feedback.
- `FR-RTE-012 P1` Route changes shall propagate to all active gateway instances.

### 4.2 Request Context and Header Security

- `FR-CTX-001 P0` Every request shall have one gateway-approved request ID.
- `FR-CTX-002 P0` Malformed or oversized client request IDs shall be replaced.
- `FR-CTX-003 P0` All inbound `X-Sentra-*` headers shall be removed before policy evaluation.
- `FR-CTX-004 P0` The system shall resolve source IP using only configured trusted proxy hops.
- `FR-CTX-005 P0` The system shall normalize route/path metadata used in policy decisions.
- `FR-CTX-006 P0` Trusted identity headers shall be created only after successful validation.
- `FR-CTX-007 P1` Correlation and trace context shall be propagated to downstream services.

### 4.3 JWT Authentication

- `FR-JWT-001 P0` Protected user/admin routes shall validate JWT signatures.
- `FR-JWT-002 P0` Validation shall enforce approved algorithms.
- `FR-JWT-003 P0` Validation shall enforce issuer and audience.
- `FR-JWT-004 P0` Validation shall enforce expiration and not-before claims with bounded skew.
- `FR-JWT-005 P0` Required claims shall be present and correctly typed.
- `FR-JWT-006 P0` Roles and scopes shall be normalized from configured claims.
- `FR-JWT-007 P0` Invalid or absent credentials shall return a stable `401`.
- `FR-JWT-008 P0` Authentication failures shall not disclose key, token, or claim internals.
- `FR-JWT-009 P0` Authentication outcomes shall be auditable and measurable.
- `FR-JWT-010 P1` The key cache shall support issuer key rotation.

### 4.4 API-Key Authentication

- `FR-KEY-001 P0` Authorized administrators shall create API clients.
- `FR-KEY-002 P0` The system shall generate API keys using a cryptographically secure source.
- `FR-KEY-003 P0` Plaintext key material shall be returned only at issuance.
- `FR-KEY-004 P0` Persistent storage shall contain only a secure verifier and non-secret lookup prefix.
- `FR-KEY-005 P0` Validation shall enforce client status, key status, expiry, scopes, and route restrictions.
- `FR-KEY-006 P0` Administrators shall revoke keys with prompt enforcement across instances.
- `FR-KEY-007 P0` Administrators shall rotate keys with configurable overlap.
- `FR-KEY-008 P0` Key metadata shall record creation, expiry, revocation, and rotation lineage.
- `FR-KEY-009 P1` Last-used metadata shall be updated without imposing an unbounded write per request.
- `FR-KEY-010 P0` Raw keys shall never be logged, audited, cached as Redis key names, or returned by list APIs.

### 4.5 Authorization

- `FR-AUTHZ-001 P0` Routes shall declare accepted authentication mechanisms.
- `FR-AUTHZ-002 P0` Routes shall support required roles and scopes.
- `FR-AUTHZ-003 P0` Missing required permission shall return a stable `403`.
- `FR-AUTHZ-004 P0` Authorization shall default to deny for protected routes with incomplete policy.
- `FR-AUTHZ-005 P0` Administrative capabilities shall be separated by least-privileged authorities.
- `FR-AUTHZ-006 P0` Downstream services shall receive the normalized authorized identity context.

### 4.6 Request Signing and Replay Protection

- `FR-SIG-001 P0` Routes shall be able to require request signatures.
- `FR-SIG-002 P0` The signature shall cover method, normalized path, canonical query, body hash, timestamp, nonce, and key ID.
- `FR-SIG-003 P0` The canonicalization specification shall define encoding and duplicate-value behavior.
- `FR-SIG-004 P0` Signature comparison shall resist timing leakage where applicable.
- `FR-SIG-005 P0` Requests outside the permitted timestamp window shall be rejected.
- `FR-SIG-006 P0` A nonce shall be accepted at most once within the replay window for a key.
- `FR-SIG-007 P0` Replay-protected routes shall fail closed if nonce storage is unavailable.
- `FR-SIG-008 P0` Body buffering for signature verification shall honor a strict maximum.
- `FR-SIG-009 P0` Signature failures shall produce non-sensitive reason codes in audit data.
- `FR-SIG-010 P1` Client signing examples shall be published and tested against the implementation.

### 4.7 IP Policy

- `FR-IP-001 P0` Administrators shall manage exact IP and CIDR rules for IPv4 and IPv6.
- `FR-IP-002 P0` Rules shall support allow, block, and temporary block actions.
- `FR-IP-003 P0` Rules may be global or route-specific.
- `FR-IP-004 P0` Rule precedence shall be deterministic and documented.
- `FR-IP-005 P0` Expired or disabled rules shall not affect requests.
- `FR-IP-006 P0` Denied requests shall be audited with rule identity and safe reason.
- `FR-IP-007 P0` The system shall prevent untrusted forwarded headers from bypassing IP rules.
- `FR-IP-008 P1` Administration shall include a recovery mechanism for accidental lockout.

### 4.8 Rate Limiting

- `FR-RL-001 P0` The system shall enforce distributed rate limits through Redis.
- `FR-RL-002 P0` Policies shall support global, IP, user, API client, tenant, route, method, and composite subjects.
- `FR-RL-003 P0` Policies shall define capacity and refill behavior.
- `FR-RL-004 P0` Policy precedence shall select the most specific applicable policy.
- `FR-RL-005 P0` Rejected requests shall return `429` and retry metadata.
- `FR-RL-006 P0` Allowed responses shall expose configured limit metadata where enabled.
- `FR-RL-007 P0` Concurrent gateway instances shall not independently exceed one shared policy allowance.
- `FR-RL-008 P0` Each route category shall declare Redis-outage behavior.
- `FR-RL-009 P1` The implementation shall bound key cardinality and memory growth.
- `FR-RL-010 P0` Rate-limit decisions shall be measured and denial events audited.

### 4.9 Risk Rules

- `FR-RISK-001 P1` Administrators shall define enabled weighted risk rules.
- `FR-RISK-002 P1` Signals may include repeated authentication failure, invalid paths, burst patterns, signature failure, and prior blocks.
- `FR-RISK-003 P1` Evaluation shall produce an explainable score and contributing rule IDs.
- `FR-RISK-004 P1` Thresholds shall support observe, throttle, temporary block, and deny.
- `FR-RISK-005 P1` Scores and temporary effects shall decay or expire.
- `FR-RISK-006 P1` Rule evaluation shall have bounded time and memory cost.
- `FR-RISK-007 P1` Operators shall be able to investigate and override false positives with audit.

### 4.10 Audit

- `FR-AUD-001 P0` The system shall persist structured records for security and routing decisions selected by audit policy.
- `FR-AUD-002 P0` Every denial shall produce one final audit decision.
- `FR-AUD-003 P0` Events shall include timestamp, request ID, route, method, normalized path, actor type, decision, status, latency, and source IP.
- `FR-AUD-004 P0` Events shall exclude raw tokens, keys, signatures, passwords, and request bodies.
- `FR-AUD-005 P0` Administrators and auditors shall search by bounded time range, request ID, route, event type, decision, status, and subject reference.
- `FR-AUD-006 P0` Search results shall be paginated and authorization-filtered.
- `FR-AUD-007 P0` Administrative mutations shall record actor, action, target, result, and safe before/after change information.
- `FR-AUD-008 P1` Retention and archival shall be configurable.
- `FR-AUD-009 P1` Export shall be size-bounded, authorized, and itself audited.
- `FR-AUD-010 P0` Audit-sink failure behavior shall be explicit and alertable.

### 4.11 Resilience

- `FR-RES-001 P0` Routes shall support connection and response timeout configuration.
- `FR-RES-002 P0` Routes shall support circuit-breaker configuration.
- `FR-RES-003 P0` Routes may support bounded retries for eligible failures.
- `FR-RES-004 P0` Non-idempotent requests shall not be retried unless an explicit idempotency contract permits it.
- `FR-RES-005 P0` Retry backoff shall be bounded and include jitter.
- `FR-RES-006 P0` Routes may define a controlled fallback.
- `FR-RES-007 P0` Fallbacks shall not report fabricated success for security-sensitive mutations.
- `FR-RES-008 P0` Gateway-owned dependency failures shall return stable error codes.
- `FR-RES-009 P0` Circuit, retry, timeout, and fallback actions shall emit metrics.
- `FR-RES-010 P1` Concurrency/bulkhead controls shall protect constrained downstream services.

### 4.12 Errors

- `FR-ERR-001 P0` Gateway errors shall include timestamp, request ID, HTTP status, code, safe message, path, and route ID when known.
- `FR-ERR-002 P0` Error codes shall be stable within an API version.
- `FR-ERR-003 P0` Responses shall not expose stack traces, SQL, hosts, credentials, or cryptographic details.
- `FR-ERR-004 P0` Validation errors shall identify invalid fields without echoing secrets.
- `FR-ERR-005 P0` The same failure class shall have a consistent status and code across filters.

### 4.13 Demonstration Services

- `FR-SVC-001 P0` User service shall provide at least one public and one JWT-protected route.
- `FR-SVC-002 P0` Order service shall provide subject-scoped read and create routes.
- `FR-SVC-003 P0` Payment service shall provide API-key and signature-protected routes.
- `FR-SVC-004 P0` Notification service shall provide configurable timeout/failure scenarios.
- `FR-SVC-005 P0` Each service shall expose health and Prometheus metrics.
- `FR-SVC-006 P0` Each service shall echo request correlation and consume trusted identity headers.
- `FR-SVC-007 P0` Development failure controls shall be disabled in production-like profiles.
- `FR-SVC-008 P0` Direct external access to service application ports shall be disabled by default.

## 5. Data Requirements

### 5.1 Required Entities

| Entity | Minimum data |
| --- | --- |
| GatewayRoute | ID, predicates, methods, URI, order, metadata, enabled, version, audit fields |
| RoutePermission | Route, role/scope, rule type |
| ApiClient | ID, name, owner, status, tenant, audit fields |
| ApiKey | ID, client, prefix, verifier, status, expiry, rotation parent, last-used |
| RateLimitPolicy | subject, route/method scope, capacity, refill, priority, outage mode |
| IpRule | CIDR/address, action, route, priority, reason, expiry |
| RiskRule | signal/condition, weight, threshold action, enabled |
| AuditEvent | time, request/route, actor reference, decision, reason, status, latency |
| AdminActionLog | time, actor, action, target, result, change summary |

### 5.2 Data Integrity

- `DR-001 P0` Stable IDs shall not be reused after deletion.
- `DR-002 P0` Mutable policies shall use optimistic concurrency.
- `DR-003 P0` Invalid capacities, expiry ranges, URIs, CIDRs, and empty permission values shall be rejected.
- `DR-004 P0` Referential integrity shall prevent orphaned policy records.
- `DR-005 P0` API-key verifiers shall never be readable through an API.
- `DR-006 P1` Audit data shall support time-based partitioning or equivalent lifecycle management.
- `DR-007 P1` Retention deletion shall be observable and auditable.

## 6. Non-Functional Requirements

### 6.1 Security

- `NFR-SEC-001 P0` All production client traffic shall use TLS.
- `NFR-SEC-002 P0` Secrets and credentials shall be externally injected.
- `NFR-SEC-003 P0` Credentials and sensitive values shall be redacted from logs and audit.
- `NFR-SEC-004 P0` Route target validation shall mitigate SSRF.
- `NFR-SEC-005 P0` Reserved internal headers shall be sanitized.
- `NFR-SEC-006 P0` Management endpoints shall not be publicly exposed.
- `NFR-SEC-007 P0` Dependency, secret, static, and container scans shall run in CI.
- `NFR-SEC-008 P1` Release candidates shall be reviewed against OWASP API Security Top 10.
- `NFR-SEC-009 P0` Cryptographic algorithms and key sizes shall follow current approved organizational policy.
- `NFR-SEC-010 P0` Security controls shall fail according to documented deny/degraded behavior, never silently disable.

### 6.2 Performance

Targets apply in the approved performance environment and exclude downstream processing unless stated:

- `NFR-PERF-001 P1` At target average load, gateway-added p95 latency should be at most 25 ms for cached JWT, route, and policy decisions.
- `NFR-PERF-002 P1` Gateway-added p99 latency should be at most 60 ms under the same conditions.
- `NFR-PERF-003 P1` The gateway shall sustain the approved target throughput for 30 minutes with less than 0.1% gateway-caused 5xx errors.
- `NFR-PERF-004 P1` A two-hour soak shall not show unbounded heap, connection, Redis-key, or audit-queue growth.
- `NFR-PERF-005 P0` Request body and header limits shall prevent memory exhaustion from a single request.

Final throughput numbers must be established after hardware and route mix are defined.

### 6.3 Availability and Reliability

- `NFR-REL-001 P1` The production-like target availability shall be 99.9% monthly, excluding approved maintenance.
- `NFR-REL-002 P0` Multiple gateway instances shall operate without session affinity.
- `NFR-REL-003 P0` One instance failure shall not corrupt shared rate, nonce, route, or key state.
- `NFR-REL-004 P0` The gateway shall shut down gracefully and stop accepting traffic before termination.
- `NFR-REL-005 P0` Last known valid routes shall remain usable during temporary route-store failure where dependencies permit.
- `NFR-REL-006 P1` PostgreSQL backup recovery point objective shall be at most 24 hours for the demonstration and tightened for production.
- `NFR-REL-007 P1` Demonstrated recovery time objective shall be at most 60 minutes.

### 6.4 Scalability

- `NFR-SCL-001 P0` Gateway instances shall be horizontally scalable.
- `NFR-SCL-002 P0` Distributed rate limits and nonces shall preserve correctness across instances.
- `NFR-SCL-003 P1` Policy refresh shall converge across healthy instances within 10 seconds.
- `NFR-SCL-004 P1` Audit persistence shall support batching or asynchronous handling with bounded queues.
- `NFR-SCL-005 P0` Metrics shall avoid unbounded label cardinality.

### 6.5 Observability

- `NFR-OBS-001 P0` Every gateway response shall be correlatable to structured logs by request ID.
- `NFR-OBS-002 P0` Metrics shall cover traffic, latency, security decisions, dependencies, and runtime health.
- `NFR-OBS-003 P1` Dashboards shall show overview, security, routes, dependencies, and JVM/runtime views.
- `NFR-OBS-004 P1` Alerts shall cover availability, latency, errors, denial anomalies, Redis, PostgreSQL, circuits, and audit backlog.
- `NFR-OBS-005 P1` Every actionable alert shall link to a runbook.
- `NFR-OBS-006 P1` Distributed traces should use W3C trace context and a supported OpenTelemetry backend.

### 6.6 Maintainability and Testability

- `NFR-MNT-001 P0` Modules shall have explicit ownership and dependency boundaries.
- `NFR-MNT-002 P0` Filters shall delegate policy logic to unit-testable services.
- `NFR-MNT-003 P0` Build and test shall be reproducible from documented commands.
- `NFR-MNT-004 P0` Database schema shall be reproducible from migrations.
- `NFR-MNT-005 P0` Every P0 functional requirement shall have automated verification or a documented manual control.
- `NFR-MNT-006 P1` Architecture tests shall prevent prohibited module coupling.
- `NFR-MNT-007 P1` Public/admin contracts shall have compatibility or contract tests.

### 6.7 Usability and Compatibility

- `NFR-USE-001 P0` Error messages shall be actionable without exposing sensitive internals.
- `NFR-USE-002 P0` OpenAPI shall document authentication, errors, pagination, and examples.
- `NFR-USE-003 P1` Partner signing documentation shall include a working reference client.
- `NFR-USE-004 P0` API changes within `/api/v1` shall remain backward compatible unless a documented exception is approved.
- `NFR-USE-005 P1` Local startup shall require one documented command after prerequisites are installed.

## 7. Use Cases

### UC-01 Route a Public Request

**Actor:** Anonymous client  
**Precondition:** Enabled public route exists.  
**Flow:** Gateway assigns request ID, resolves IP, applies public controls/rate limit, forwards sanitized request, records metrics/audit as configured, and returns response.  
**Alternate:** Blocked IP produces `403`; excess rate produces `429`; no route produces `404`.

### UC-02 Access User Orders

**Actor:** JWT user  
**Precondition:** Valid issuer, key, claims, and `orders:read` scope.  
**Flow:** Gateway validates token, authorizes scope, rate-limits subject/route, adds trusted subject headers, forwards to order service, and returns the result.  
**Alternate:** Invalid token produces `401`; missing scope produces `403`.

### UC-03 Submit Signed Partner Payment

**Actor:** Partner API client  
**Precondition:** Active client/key with `payments:write`; route requires signing.  
**Flow:** Gateway validates key, canonical request, timestamp, signature, nonce, and rate limit; it forwards a sanitized request and records an audit decision.  
**Alternate:** Reused nonce, body mutation, stale timestamp, revoked key, or missing scope denies the request.

### UC-04 Rotate an API Key

**Actor:** Security administrator  
**Precondition:** Administrator has key-management authority.  
**Flow:** Admin requests rotation with idempotency key; gateway generates new material, stores verifier, creates overlap metadata, returns plaintext once, invalidates caches, and audits action.  
**Postcondition:** Old key remains valid only through approved overlap and can be revoked immediately.

### UC-05 Change a Route

**Actor:** Route administrator  
**Flow:** Admin submits versioned change; gateway validates conflicts and target safety, commits transaction, emits refresh, and instances adopt a validated route generation.  
**Alternate:** Version conflict returns `409`; unsafe URI or ambiguity returns validation error.

### UC-06 Investigate an Incident

**Actor:** Auditor/operator  
**Flow:** User searches a bounded time range by request ID/route/decision, correlates audit events with logs and dashboards, and exports only if authorized.  
**Security:** Search and export actions are audited.

## 8. Business Rules

- `BR-001` External clients must not directly reach downstream services.
- `BR-002` A protected route without a complete valid policy is unavailable rather than public.
- `BR-003` A plaintext API key is recoverable only at generation time.
- `BR-004` Payment mutations are not automatically retried.
- `BR-005` A replay nonce is unique within key identity and replay window.
- `BR-006` Dynamic routes cannot target prohibited schemes, metadata endpoints, loopback, or restricted networks unless explicitly approved.
- `BR-007` Administrative read and write privileges are separable.
- `BR-008` Audit and metrics must avoid high-cardinality or secret values.
- `BR-009` Development-only fault controls cannot run in production-like profiles.
- `BR-010` The current documentation describes intended behavior until implementation evidence is linked.

## 9. Verification and Acceptance

### 9.1 Verification Methods

| Method | Meaning |
| --- | --- |
| Unit test | Isolated policy or transformation behavior |
| Integration test | Real PostgreSQL, Redis, security, or route integration |
| Contract test | Stable API/header/error compatibility |
| End-to-end test | Client-to-gateway-to-service behavior |
| Security test | Abuse, bypass, malformed, or negative behavior |
| Performance test | Measured latency, throughput, saturation, and soak |
| Inspection | Configuration, documentation, migration, dashboard, or scan evidence |
| Exercise | Runbook, restore, rollback, or incident simulation |

### 9.2 Requirement Traceability Summary

| Requirement group | Primary module | Primary verification |
| --- | --- | --- |
| `FR-RTE`, `FR-CTX` | routing/common | Integration + end-to-end |
| `FR-JWT`, `FR-AUTHZ` | security.jwt/authorization | Security + integration |
| `FR-KEY` | security.apikey/admin | Integration + security |
| `FR-SIG` | security.signing | Unit + security + end-to-end |
| `FR-IP` | security.ip | Unit/property + security |
| `FR-RL` | ratelimit | Redis integration + concurrency |
| `FR-RISK` | security.risk | Unit + scenario tests |
| `FR-AUD` | audit/admin | DB integration + inspection |
| `FR-RES` | resilience | Failure injection |
| `FR-SVC` | mock services | Contract + end-to-end |
| `NFR-PERF` | whole system | k6 |
| `NFR-SEC` | whole system | Security tests + scans |
| `NFR-REL` | deployment | Resilience exercise |
| `NFR-OBS` | observability | Dashboard/alert smoke test |

### 9.3 System Acceptance Criteria

The release is accepted when:

1. All P0 requirements are implemented or explicitly waived with recorded risk.
2. Automated tests demonstrate public, JWT, API-key, signed, admin, and failure flows.
3. No critical/high unresolved vulnerability remains without formal acceptance.
4. A clean local environment starts and passes smoke tests.
5. Route and policy changes propagate correctly across at least two gateway instances.
6. Redis, PostgreSQL, JWK, and downstream outages produce documented behavior.
7. Performance evidence meets approved thresholds.
8. Dashboards, alerts, audit search, backup restore, rollback, and runbooks are demonstrated.
9. OpenAPI and all companion documents match the delivered system.

## 10. Risks and Open Issues

| ID | Issue | Required resolution |
| --- | --- | --- |
| OI-01 | Java 25/Spring Boot 4/Spring Cloud compatibility | Pin a verified release matrix before scaffolding. |
| OI-02 | Reactive persistence approach | Choose R2DBC or isolate blocking repositories. |
| OI-03 | Exact signature canonicalization | Freeze a protocol and golden test vectors. |
| OI-04 | Redis outage policy | Approve fail behavior per route category. |
| OI-05 | Audit durability target | Define compliance mode, buffering, and loss tolerance. |
| OI-06 | Performance throughput target | Define hardware, route mix, payloads, and concurrency. |
| OI-07 | Secret manager | Select production secret storage and rotation mechanism. |
| OI-08 | Production platform | Define Kubernetes, VM, or other deployment beyond Compose. |

## Appendix A: Baseline Error Codes

| Code | Status | Meaning |
| --- | ---:| --- |
| `GW_ROUTE_NOT_FOUND` | 404 | No enabled route matched. |
| `GW_REQUEST_INVALID` | 400 | Request failed structural validation. |
| `GW_BODY_TOO_LARGE` | 413 | Body exceeded policy. |
| `GW_AUTH_REQUIRED` | 401 | Required credentials absent. |
| `GW_TOKEN_INVALID` | 401 | JWT failed validation. |
| `GW_API_KEY_INVALID` | 401 | API key failed validation. |
| `GW_SIGNATURE_INVALID` | 401 | Signature validation failed. |
| `GW_REPLAY_DETECTED` | 403 | Nonce was already used. |
| `GW_PERMISSION_DENIED` | 403 | Role or scope missing. |
| `GW_IP_DENIED` | 403 | IP policy denied request. |
| `GW_RISK_DENIED` | 403 | Risk policy denied request. |
| `GW_RATE_LIMITED` | 429 | Rate allowance exhausted. |
| `GW_POLICY_CONFLICT` | 409 | Optimistic version or policy conflict. |
| `GW_DOWNSTREAM_TIMEOUT` | 504 | Downstream exceeded timeout. |
| `GW_DOWNSTREAM_UNAVAILABLE` | 503 | Downstream/circuit unavailable. |
| `GW_DEPENDENCY_UNAVAILABLE` | 503 | Required gateway dependency unavailable. |
| `GW_INTERNAL_ERROR` | 500 | Unexpected sanitized gateway failure. |

## Appendix B: Minimum Security Regression Matrix

| Test | Expected result |
| --- | --- |
| Spoof `X-Sentra-Subject` | Header is removed and cannot change identity. |
| Invalid JWT signature/algorithm | `401`, audit, no downstream call. |
| Wrong issuer/audience | `401`, no downstream call. |
| Expired/revoked API key | `401`, no downstream call. |
| Missing route scope | `403`, no downstream call. |
| Reuse signing nonce | Replay rejection. |
| Change signed body byte | Signature rejection. |
| Forge `X-Forwarded-For` from untrusted peer | Socket peer is used. |
| Add route to loopback/metadata address | Admin validation rejects route. |
| Exceed shared limit through two instances | Aggregate allowance remains bounded. |
| Redis outage on signed payment | Request denied. |
| POST payment downstream timeout | No automatic retry. |
| Search audit with unauthorized role | `403`. |
| Oversized header/body | Early bounded rejection. |
| Inspect logs after all cases | No credential or secret leakage. |
