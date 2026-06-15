# Sentra Gateway Master TODO

**Document status:** Planning baseline  
**Version:** 1.0  
**Date:** June 14, 2026  
**Source:** [Sentra Gateway Technical Documentation](Sentra_Gateway_Technical_Documentation.md)  
**Important:** The current workspace contains design documents only. Every implementation item below is therefore unchecked until code or verifiable evidence exists.

## How to Use This Backlog

- `[ ]` means no implementation evidence has been verified.
- `[x]` means the item has implementation, automated tests, and required documentation.
- Priority is expressed as `P0` critical, `P1` high, `P2` normal, or `P3` optional.
- Each pull request should reference one or more task IDs.
- A feature is not complete until its positive tests, negative tests, metrics, audit behavior, and documentation are complete.
- Security-sensitive changes require threat review and explicit evidence that secrets are not logged.

## Program Exit Criteria

- [ ] `PGM-001 P0` All P0 requirements in the SRS are implemented and traceable to tests.
- [ ] `PGM-002 P0` External traffic can reach downstream services only through Sentra Gateway.
- [ ] `PGM-003 P0` Authentication, authorization, IP policy, signing, and rate-limit failures default to deny.
- [ ] `PGM-004 P0` PostgreSQL and Redis failure modes are documented and tested.
- [ ] `PGM-005 P0` No token, API key, signing secret, password, or full credential is logged.
- [ ] `PGM-006 P0` Docker Compose starts the complete local platform from a clean machine.
- [ ] `PGM-007 P0` Unit, integration, security, contract, resilience, and smoke suites pass in CI.
- [ ] `PGM-008 P1` Performance targets are measured with committed k6 scenarios and reports.
- [ ] `PGM-009 P1` Dashboards, alerts, runbooks, backup, and restore exercises are verified.
- [ ] `PGM-010 P1` OpenAPI, architecture, microservices, SRS, README, and operator docs agree.

## Phase 0: Decisions and Repository Foundation

- [ ] `ARC-001 P0` Create a multi-module Maven or Gradle repository rooted at `sentra-gateway/`.
- [ ] `ARC-002 P0` Record the selected build tool and version in an ADR.
- [ ] `ARC-003 P0` Verify Java 25 compatibility with the chosen Spring Boot and Spring Cloud release train.
- [ ] `ARC-004 P0` Pin all dependency and plugin versions through dependency management.
- [ ] `ARC-005 P0` Create modules for gateway, user, order, payment, and notification services.
- [ ] `ARC-006 P1` Create directories for observability, performance, deployment, scripts, and documentation.
- [ ] `ARC-007 P0` Define package root `com.omar.sentra.gateway`.
- [ ] `ARC-008 P0` Add `.gitignore`, `.editorconfig`, formatting rules, and line-ending policy.
- [ ] `ARC-009 P1` Add license, contribution guide, code of conduct, and security policy.
- [ ] `ARC-010 P0` Add a root README with prerequisites, build, test, and local-start commands.
- [ ] `ARC-011 P0` Add CI workflows for compile, test, static analysis, and artifact packaging.
- [ ] `ARC-012 P1` Add dependency update automation with controlled merge rules.
- [ ] `ARC-013 P0` Enable compiler warnings and fail on avoidable build warnings.
- [ ] `ARC-014 P1` Configure Spotless or equivalent deterministic formatting.
- [ ] `ARC-015 P1` Configure Checkstyle, PMD, SpotBugs, or justified equivalents.
- [ ] `ARC-016 P0` Add OWASP dependency scanning and secret scanning.
- [ ] `ARC-017 P0` Add software bill of materials generation.
- [ ] `ARC-018 P1` Add container image vulnerability scanning.
- [ ] `ARC-019 P1` Define semantic versioning and branch/release conventions.
- [ ] `ARC-020 P1` Create ADRs for reactive stack, persistence style, route storage, and cache consistency.
- [ ] `ARC-021 P0` Decide whether the gateway uses R2DBC or isolates blocking JPA work from event-loop threads.
- [ ] `ARC-022 P0` Define service ownership and code-owner rules.
- [ ] `ARC-023 P1` Establish common logging, error, clock, ID, and test utilities.
- [ ] `ARC-024 P1` Add architecture tests that prevent forbidden package dependencies.
- [ ] `ARC-025 P1` Add a reproducible developer bootstrap script.

## Phase 1: Local Platform and Service Skeletons

- [ ] `PLT-001 P0` Create `docker-compose.yml` for gateway, four mock services, PostgreSQL, Redis, Prometheus, and Grafana.
- [ ] `PLT-002 P0` Assign stable internal DNS names and non-conflicting ports.
- [ ] `PLT-003 P0` Keep downstream application ports off the public host interface by default.
- [ ] `PLT-004 P0` Add container health checks for every service.
- [ ] `PLT-005 P1` Add startup dependency conditions without assuming readiness from process start.
- [ ] `PLT-006 P1` Add named volumes for PostgreSQL and Grafana.
- [ ] `PLT-007 P1` Add an isolated internal network and explicit edge network.
- [ ] `PLT-008 P0` Inject credentials through environment or secret files, never committed values.
- [ ] `PLT-009 P1` Add resource limits and restart policies for production-like Compose.
- [ ] `PLT-010 P0` Add local, test, performance, and production-like configuration profiles.
- [ ] `PLT-011 P0` Validate required configuration at application startup.
- [ ] `PLT-012 P1` Publish a complete environment-variable catalog.
- [ ] `PLT-013 P1` Add `.env.example` containing placeholders only.
- [ ] `PLT-014 P0` Add gateway liveness and readiness health groups.
- [ ] `PLT-015 P1` Add dependency health indicators with bounded timeouts.
- [ ] `PLT-016 P0` Create mock user-service skeleton and health endpoint.
- [ ] `PLT-017 P0` Create mock order-service skeleton and health endpoint.
- [ ] `PLT-018 P0` Create mock payment-service skeleton and health endpoint.
- [ ] `PLT-019 P0` Create mock notification-service skeleton and health endpoint.
- [ ] `PLT-020 P1` Add deterministic sample data to all mock services.
- [ ] `PLT-021 P1` Add configurable delay and failure simulation to mock services.
- [ ] `PLT-022 P1` Add request-ID echoing to mock services.
- [ ] `PLT-023 P1` Add graceful shutdown and readiness withdrawal.
- [ ] `PLT-024 P0` Add a platform smoke test that waits for healthy services.
- [ ] `PLT-025 P1` Document Windows, Linux, and macOS local setup differences.

## Phase 2: Database and Persistent Policy Model

- [ ] `DB-001 P0` Configure PostgreSQL connectivity and connection-pool limits.
- [ ] `DB-002 P0` Configure Flyway and create an immutable migration convention.
- [ ] `DB-003 P0` Create `gateway_routes` with stable route ID, URI, predicates, priority, enabled state, and version.
- [ ] `DB-004 P0` Create `route_permissions` for roles and scopes.
- [ ] `DB-005 P0` Create `api_clients` with lifecycle and ownership metadata.
- [ ] `DB-006 P0` Create `api_keys` with prefix, strong hash, status, expiry, rotation lineage, and last-used time.
- [ ] `DB-007 P0` Create `api_scopes` and normalized key/route scope mappings.
- [ ] `DB-008 P0` Create `rate_limit_policies` with subject type, route, method, capacity, refill, and precedence.
- [ ] `DB-009 P0` Create `ip_rules` with CIDR, action, route scope, reason, expiry, and priority.
- [ ] `DB-010 P1` Create `risk_rules` with condition, weight, threshold action, and enabled state.
- [ ] `DB-011 P0` Create partition-ready `audit_events`.
- [ ] `DB-012 P0` Create append-only `admin_action_logs`.
- [ ] `DB-013 P1` Add optimistic-lock versions to mutable policy entities.
- [ ] `DB-014 P0` Add uniqueness and check constraints for invalid policy states.
- [ ] `DB-015 P0` Add indexes for active route lookup and route ordering.
- [ ] `DB-016 P0` Add indexes for API key prefix/status lookup.
- [ ] `DB-017 P1` Add audit indexes for time, request ID, route, event type, and subject.
- [ ] `DB-018 P1` Define audit partitioning and retention strategy.
- [ ] `DB-019 P0` Ensure migrations never insert production secrets.
- [ ] `DB-020 P1` Add idempotent local seed data in a development-only migration.
- [ ] `DB-021 P0` Add Testcontainers migration and repository tests.
- [ ] `DB-022 P1` Add schema documentation and entity relationship diagram.
- [ ] `DB-023 P1` Add backup, restore, and migration rollback runbooks.
- [ ] `DB-024 P0` Test startup against empty and previously migrated databases.
- [ ] `DB-025 P1` Define data deletion, pseudonymization, and retention jobs.

## Phase 3: Routing Core

- [ ] `RTE-001 P0` Implement persistent route loading into Spring Cloud Gateway.
- [ ] `RTE-002 P0` Validate route paths, methods, schemes, target hosts, and priorities.
- [ ] `RTE-003 P0` Reject unsafe target schemes and unapproved destination networks.
- [ ] `RTE-004 P0` Prevent admin and actuator paths from being shadowed by dynamic routes.
- [ ] `RTE-005 P0` Implement create, read, update, enable, disable, and delete route operations.
- [ ] `RTE-006 P0` Refresh route definitions after committed changes.
- [ ] `RTE-007 P1` Publish route-change events for multi-instance cache invalidation.
- [ ] `RTE-008 P0` Preserve the last known valid route set if refresh fails.
- [ ] `RTE-009 P1` Add route configuration dry-run validation.
- [ ] `RTE-010 P1` Add route conflict and ambiguity detection.
- [ ] `RTE-011 P0` Support method predicates and path predicates.
- [ ] `RTE-012 P1` Support controlled header, host, query, and weight predicates.
- [ ] `RTE-013 P0` Strip untrusted identity and internal control headers.
- [ ] `RTE-014 P0` Generate or validate `X-Request-Id`.
- [ ] `RTE-015 P0` Forward a bounded set of trusted identity headers after authentication.
- [ ] `RTE-016 P0` Define request and response header size limits.
- [ ] `RTE-017 P0` Define maximum request body size globally and per route.
- [ ] `RTE-018 P1` Support route metadata for auth, signing, rate limit, timeout, retry, and audit policy.
- [ ] `RTE-019 P0` Add controlled 404 behavior for unmatched routes.
- [ ] `RTE-020 P0` Add unit and integration tests for ordering, refresh, conflict, and header sanitation.

## Phase 4: Authentication and Authorization

- [ ] `AUTH-001 P0` Configure reactive OAuth2 resource-server JWT validation.
- [ ] `AUTH-002 P0` Validate signature, issuer, audience, expiration, not-before, and required claims.
- [ ] `AUTH-003 P0` Restrict accepted JWT algorithms.
- [ ] `AUTH-004 P0` Implement bounded JWK retrieval, caching, refresh, and rotation behavior.
- [ ] `AUTH-005 P0` Define clock-skew tolerance and test boundary conditions.
- [ ] `AUTH-006 P0` Normalize roles and scopes without trusting arbitrary claim shapes.
- [ ] `AUTH-007 P0` Implement public, user, partner, admin, and internal route categories.
- [ ] `AUTH-008 P0` Enforce route roles and scopes with deny-by-default semantics.
- [ ] `AUTH-009 P0` Return stable 401 and 403 problem codes without leaking token details.
- [ ] `AUTH-010 P0` Audit authentication and authorization failures.
- [ ] `AUTH-011 P1` Add metrics by route and outcome without subject-cardinality explosion.
- [ ] `AUTH-012 P0` Create API clients through an admin API.
- [ ] `AUTH-013 P0` Generate API keys using a cryptographically secure random source.
- [ ] `AUTH-014 P0` Display plaintext API key material exactly once.
- [ ] `AUTH-015 P0` Store only a strong password-style hash or keyed verifier.
- [ ] `AUTH-016 P0` Use a non-secret key prefix for indexed candidate lookup.
- [ ] `AUTH-017 P0` Compare key verifiers in constant-time where applicable.
- [ ] `AUTH-018 P0` Enforce key status, expiry, client status, scopes, and route restrictions.
- [ ] `AUTH-019 P0` Implement overlapping zero-downtime key rotation.
- [ ] `AUTH-020 P0` Implement immediate key revocation and cache invalidation.
- [ ] `AUTH-021 P1` Record last-used timestamp asynchronously or with bounded write frequency.
- [ ] `AUTH-022 P0` Protect all admin APIs with an explicit admin authority.
- [ ] `AUTH-023 P1` Support separation of route-admin, security-admin, auditor, and operator roles.
- [ ] `AUTH-024 P0` Add JWT and API-key positive, negative, expiry, rotation, and revocation tests.
- [ ] `AUTH-025 P0` Add tests proving spoofed trusted headers are replaced.

## Phase 5: Request Signing and Replay Protection

- [ ] `SIG-001 P0` Publish a canonical request format specification.
- [ ] `SIG-002 P0` Canonicalize HTTP method, normalized path, canonical query, body hash, timestamp, nonce, and key ID.
- [ ] `SIG-003 P0` Specify percent-encoding, duplicate-query, empty-body, and content-encoding behavior.
- [ ] `SIG-004 P0` Validate signatures with HMAC-SHA-256 or a documented approved alternative.
- [ ] `SIG-005 P0` Reject missing, malformed, stale, or future timestamps.
- [ ] `SIG-006 P0` Use Redis atomic set-if-absent with TTL for nonce replay protection.
- [ ] `SIG-007 P0` Scope nonce keys by client/key identity.
- [ ] `SIG-008 P0` Fail closed when signing is required and replay storage is unavailable.
- [ ] `SIG-009 P0` Apply route-specific signing requirements.
- [ ] `SIG-010 P0` Bound cached request bodies and reject oversized signed payloads.
- [ ] `SIG-011 P0` Avoid logging canonical strings, signatures, or signing secrets.
- [ ] `SIG-012 P1` Provide Java and curl-compatible signing examples.
- [ ] `SIG-013 P0` Test body mutation, path normalization, query order, clock skew, and replay.
- [ ] `SIG-014 P1` Add signature validation latency metrics.
- [ ] `SIG-015 P1` Audit signature failures with non-sensitive reason codes.

## Phase 6: IP Policy, Rate Limiting, and Risk

- [ ] `POL-001 P0` Define trusted proxy and client-IP resolution rules.
- [ ] `POL-002 P0` Ignore forwarded headers from untrusted peers.
- [ ] `POL-003 P0` Implement IPv4 and IPv6 exact-address and CIDR matching.
- [ ] `POL-004 P0` Define deterministic allow, block, temporary-block, and route-rule precedence.
- [ ] `POL-005 P0` Support rule expiry and disabled state.
- [ ] `POL-006 P0` Prevent accidental self-lockout from administrative access.
- [ ] `POL-007 P1` Add bulk import/export for reviewed IP rules.
- [ ] `POL-008 P0` Audit every denied IP decision.
- [ ] `POL-009 P0` Implement atomic Redis token-bucket logic.
- [ ] `POL-010 P0` Support subjects: global, IP, user, client, tenant, route, and composite.
- [ ] `POL-011 P0` Define policy precedence from most-specific to default.
- [ ] `POL-012 P0` Return `429`, `Retry-After`, limit, remaining, and reset metadata.
- [ ] `POL-013 P0` Define fail-open/fail-closed behavior per route if Redis is unavailable.
- [ ] `POL-014 P1` Add protection against unbounded Redis key cardinality.
- [ ] `POL-015 P1` Add TTL jitter where it prevents synchronized expiry.
- [ ] `POL-016 P0` Test concurrency across multiple gateway instances.
- [ ] `POL-017 P1` Define risk signals for invalid auth, repeated 404s, scanning, burst, and signature failures.
- [ ] `POL-018 P1` Implement explainable weighted risk scoring.
- [ ] `POL-019 P1` Support observe, challenge-ready, throttle, temporary-block, and deny actions.
- [ ] `POL-020 P1` Add decay windows and automatic temporary-block expiry.
- [ ] `POL-021 P1` Keep risk-rule evaluation bounded and deterministic.
- [ ] `POL-022 P1` Add false-positive review workflow and override audit.
- [ ] `POL-023 P0` Add policy CRUD APIs with validation and optimistic locking.
- [ ] `POL-024 P0` Add negative tests for bypass attempts and precedence errors.
- [ ] `POL-025 P1` Add policy decision metrics with bounded labels.

## Phase 7: Audit, Errors, and Administration

- [ ] `ADM-001 P0` Define one JSON error schema with timestamp, request ID, status, code, message, path, and route ID.
- [ ] `ADM-002 P0` Define stable error codes for every gateway rejection class.
- [ ] `ADM-003 P0` Never expose stack traces or dependency internals to clients.
- [ ] `ADM-004 P0` Ensure every denial creates one final audit decision.
- [ ] `ADM-005 P0` Define audit event types and reason-code taxonomy.
- [ ] `ADM-006 P0` Include request ID, route, method, normalized path, actor type, decision, status, latency, and source IP.
- [ ] `ADM-007 P0` Exclude credentials, request bodies, sensitive query values, and raw tokens.
- [ ] `ADM-008 P1` Buffer or asynchronously persist high-volume audit events with bounded memory.
- [ ] `ADM-009 P0` Define behavior if audit persistence is unavailable.
- [ ] `ADM-010 P1` Add audit retention, partition archival, and purge jobs.
- [ ] `ADM-011 P0` Implement paginated audit search with validated filters and maximum ranges.
- [ ] `ADM-012 P1` Implement audit export with authorization and size limits.
- [ ] `ADM-013 P0` Audit every administrative mutation before/after state safely.
- [ ] `ADM-014 P0` Require idempotency keys for sensitive create/rotate operations.
- [ ] `ADM-015 P1` Add ETags or version fields to prevent lost updates.
- [ ] `ADM-016 P0` Implement complete CRUD for routes, clients, keys, rate limits, IP rules, risk rules, and permissions.
- [ ] `ADM-017 P0` Validate SSRF-sensitive route targets and reserved networks.
- [ ] `ADM-018 P0` Add pagination, filtering, sorting, and bounded page sizes.
- [ ] `ADM-019 P0` Generate OpenAPI for admin APIs and shared errors.
- [ ] `ADM-020 P0` Add controller, validation, authorization, and audit integration tests.

## Phase 8: Resilience and Downstream Services

- [ ] `RES-001 P0` Define connect, response, and total timeout budgets per route.
- [ ] `RES-002 P0` Apply retries only to idempotent operations or explicitly idempotent requests.
- [ ] `RES-003 P0` Use bounded exponential backoff with jitter.
- [ ] `RES-004 P0` Prevent retry amplification across gateway and downstream services.
- [ ] `RES-005 P0` Configure route-specific circuit breakers.
- [ ] `RES-006 P0` Provide stable fallback responses for eligible routes.
- [ ] `RES-007 P1` Add bulkhead/concurrency limits for expensive downstreams.
- [ ] `RES-008 P0` Propagate remaining request deadlines where possible.
- [ ] `RES-009 P0` Distinguish gateway rejection, timeout, connection failure, and downstream response.
- [ ] `RES-010 P0` Add circuit, retry, timeout, and fallback metrics.
- [ ] `RES-011 P0` Build user-service endpoints for public profile and authenticated user scenarios.
- [ ] `RES-012 P0` Build order-service read/create endpoints with role/scope examples.
- [ ] `RES-013 P0` Build payment-service partner endpoints requiring key and signature.
- [ ] `RES-014 P0` Build notification-service endpoints suitable for timeout/retry scenarios.
- [ ] `RES-015 P1` Add admin-service behavior inside gateway admin APIs rather than a public mock.
- [ ] `RES-016 P1` Add configurable status, delay, malformed response, and disconnect simulation.
- [ ] `RES-017 P0` Add consumer/provider contracts for gateway-to-service calls.
- [ ] `RES-018 P0` Test circuit opening, half-open recovery, and fallback.
- [ ] `RES-019 P0` Test that POST payment calls are not automatically retried.
- [ ] `RES-020 P1` Document downstream service onboarding requirements.

## Phase 9: Observability

- [ ] `OBS-001 P0` Emit structured JSON application logs.
- [ ] `OBS-002 P0` Correlate logs by request ID, route ID, and trace ID.
- [ ] `OBS-003 P0` Redact authorization, cookies, API keys, signatures, and configured sensitive headers.
- [ ] `OBS-004 P0` Add HTTP request count, duration, in-flight, and response-size metrics.
- [ ] `OBS-005 P0` Add auth, authorization, signing, IP, risk, and rate-limit decision counters.
- [ ] `OBS-006 P0` Add downstream timeout, retry, circuit, and fallback metrics.
- [ ] `OBS-007 P0` Add PostgreSQL pool, Redis, JVM, Netty, and process metrics.
- [ ] `OBS-008 P0` Restrict Actuator exposure to the minimum required endpoints.
- [ ] `OBS-009 P0` Protect or network-isolate `/actuator/prometheus`.
- [ ] `OBS-010 P1` Add OpenTelemetry tracing and W3C trace-context propagation.
- [ ] `OBS-011 P1` Sanitize inbound trace headers at the trust boundary.
- [ ] `OBS-012 P0` Configure Prometheus scrape targets and retention.
- [ ] `OBS-013 P0` Create Grafana overview, security, routes, dependencies, and JVM dashboards.
- [ ] `OBS-014 P1` Provision dashboards and data sources from version-controlled files.
- [ ] `OBS-015 P0` Add alerts for availability, error ratio, latency, denial spikes, and dependency failures.
- [ ] `OBS-016 P1` Add circuit-open, Redis saturation, DB pool, and audit-backlog alerts.
- [ ] `OBS-017 P1` Link alerts to runbooks.
- [ ] `OBS-018 P0` Verify metric labels cannot contain user IDs, raw paths, keys, or unbounded values.
- [ ] `OBS-019 P1` Define SLI/SLO calculations and error-budget reporting.
- [ ] `OBS-020 P0` Add an observability smoke test that verifies logs, metrics, and dashboard data.

## Phase 10: Test and Security Assurance

- [ ] `TST-001 P0` Establish minimum unit coverage for policy and security logic.
- [ ] `TST-002 P0` Add WebTestClient tests for all gateway filters and errors.
- [ ] `TST-003 P0` Add PostgreSQL and Redis Testcontainers integration suites.
- [ ] `TST-004 P0` Add end-to-end Compose smoke tests.
- [ ] `TST-005 P0` Add route hot-refresh and multi-instance invalidation tests.
- [ ] `TST-006 P0` Add JWT invalid signature, issuer, audience, expiry, nbf, and algorithm tests.
- [ ] `TST-007 P0` Add API-key brute-force resistance, revocation, expiry, and rotation tests.
- [ ] `TST-008 P0` Add signature canonicalization and replay tests.
- [ ] `TST-009 P0` Add forwarded-header spoofing and trusted-proxy tests.
- [ ] `TST-010 P0` Add SSRF route-target validation tests.
- [ ] `TST-011 P0` Add request smuggling, oversized header, oversized body, and malformed URI tests.
- [ ] `TST-012 P0` Add SQL injection and unsafe filter-input tests for admin APIs.
- [ ] `TST-013 P0` Add authorization matrix tests for every admin role.
- [ ] `TST-014 P1` Add property-based tests for CIDR and canonicalization.
- [ ] `TST-015 P1` Add fuzz tests for parsers and security headers.
- [ ] `TST-016 P0` Add contract tests for error schema and downstream trusted headers.
- [ ] `TST-017 P0` Add Redis outage, PostgreSQL outage, JWK outage, and downstream outage tests.
- [ ] `TST-018 P1` Add network latency, packet-loss, and restart resilience scenarios.
- [ ] `TST-019 P0` Add k6 smoke, average-load, stress, spike, and soak scenarios.
- [ ] `TST-020 P0` Measure gateway-added latency separately from downstream latency.
- [ ] `TST-021 P1` Define p50, p95, p99, throughput, and error-rate thresholds.
- [ ] `TST-022 P1` Add memory, connection, Redis-key, and audit-volume soak checks.
- [ ] `TST-023 P0` Add SAST, dependency, container, IaC, and secret scans to CI.
- [ ] `TST-024 P1` Perform an OWASP API Security Top 10 review.
- [ ] `TST-025 P1` Record a release security assessment and accepted risks.

## Phase 11: Delivery, Operations, and Documentation

- [ ] `OPS-001 P0` Create multi-stage, non-root Dockerfiles with read-only-compatible filesystems.
- [ ] `OPS-002 P0` Pin base images and publish image digests and SBOMs.
- [ ] `OPS-003 P0` Add graceful termination, startup, liveness, and readiness behavior.
- [ ] `OPS-004 P0` Define horizontal scaling and shared-state assumptions.
- [ ] `OPS-005 P1` Document rolling deployment and route-cache compatibility.
- [ ] `OPS-006 P0` Automate database backup and verify restoration.
- [ ] `OPS-007 P1` Define Redis persistence expectations and disaster behavior.
- [ ] `OPS-008 P0` Write runbooks for gateway outage, DB outage, Redis outage, JWK outage, latency, denial spikes, and certificate expiry.
- [ ] `OPS-009 P1` Write key compromise and emergency revocation runbooks.
- [ ] `OPS-010 P1` Write route rollback and bad-policy recovery runbooks.
- [ ] `OPS-011 P0` Document TLS termination and trusted proxy configuration.
- [ ] `OPS-012 P1` Document certificate and secret rotation.
- [ ] `OPS-013 P0` Define production configuration validation and prohibited defaults.
- [ ] `OPS-014 P1` Add release notes and migration notes templates.
- [ ] `OPS-015 P0` Build a release checklist with rollback and verification steps.
- [ ] `OPS-016 P0` Reconcile README, technical document, SRS, microservices guide, and OpenAPI.
- [ ] `OPS-017 P1` Add architecture diagrams using version-controlled Mermaid.
- [ ] `OPS-018 P1` Add onboarding tutorials for user JWT and partner signed requests.
- [ ] `OPS-019 P1` Add admin API examples for route and key lifecycle.
- [ ] `OPS-020 P1` Add troubleshooting FAQ and known limitations.

## Release Gates

### MVP Gate

- [ ] Gateway routes to all four mock services.
- [ ] JWT, API key, route authorization, Redis rate limiting, and consistent errors work.
- [ ] PostgreSQL migrations and admin route/client APIs work.
- [ ] Audit records and Prometheus metrics are visible.
- [ ] Compose smoke tests pass from a clean environment.

### Security Gate

- [ ] Threat model has been reviewed.
- [ ] Secret and dependency scans pass.
- [ ] Authentication, signing, replay, proxy-header, SSRF, and authorization negative tests pass.
- [ ] Trusted headers cannot be supplied by external clients.
- [ ] Security-sensitive failures default to deny as documented.

### Production-Like Gate

- [ ] Multi-instance rate limiting and policy refresh are verified.
- [ ] SLO dashboards and alerts are provisioned.
- [ ] Backup and restore are exercised.
- [ ] Failure-injection and k6 reports meet approved thresholds.
- [ ] Runbooks and release rollback have been rehearsed.

## Deferred Enhancements

- [ ] `FUT-001 P3` Add Kubernetes manifests or Helm chart.
- [ ] `FUT-002 P3` Add service discovery integration.
- [ ] `FUT-003 P3` Add external policy engine evaluation.
- [ ] `FUT-004 P3` Add mTLS between gateway and downstream services.
- [ ] `FUT-005 P3` Add Web Application Firewall integration.
- [ ] `FUT-006 P3` Add adaptive anomaly detection after explainable rules are mature.
- [ ] `FUT-007 P3` Add multi-region policy distribution.
- [ ] `FUT-008 P3` Add tenant self-service API client administration.
- [ ] `FUT-009 P3` Add event-stream export of audit events.
- [ ] `FUT-010 P3` Add a web administration console.
