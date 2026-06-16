# Gateway Requirements Traceability

**Date:** June 15, 2026  
**Release:** `1.0.0-SNAPSHOT`

## Status Definitions

- **Implemented:** production code and executable configuration exist.
- **Verified:** automated test or completed runtime verification exists.
- **Planned:** specified by broader project documents but outside this release.

## Implementation Matrix

| Capability | Production evidence | Automated/runtime evidence | Status |
| --- | --- | --- | --- |
| Reactive gateway runtime | `GatewayApplication`, Gateway WebFlux dependency | Application context and HTTP integration tests | Verified |
| Dynamic routes | `RouteService`, `R2dbcRouteRepository`, `DatabaseRouteDefinitionLocator` | Route CRUD, validation, version conflict tests | Verified |
| SSRF controls | `RouteService.validateTarget` | Unsafe metadata target and allowlist tests | Verified baseline |
| Local admin authentication | `SecurityConfig.localSecurity` | Unauthorized/admin/operator matrix | Verified |
| JWT resource server | `SecurityConfig.jwtSecurity` | Context compilation; external issuer E2E not in local suite | Implemented |
| Admin role separation | `SecurityConfig.commonAuthorization` | Operator read and mutation-denied tests | Verified |
| API clients | `ApiClientAdminController`, `R2dbcApiClientRepository` | Create/read integration test | Verified |
| API-key issue/verify | `ApiKeyService` | One-time issue and verification tests | Verified |
| API-key redaction | `ApiKeyMetadata` | Metadata excludes plaintext/verifier | Verified |
| API-key rotate/revoke | `ApiKeyService` | Unit rotation/revocation tests and Postman workflow | Verified |
| Route role/scope enforcement | `GatewayPolicyGlobalFilter` | Service logic and route metadata integration | Implemented |
| HMAC signing | `RequestSigningService` | Canonical query, valid HMAC, stale timestamp tests | Verified |
| Replay prevention | `RedisReplayGuard` | Podman Redis runtime and signing flow infrastructure | Implemented |
| IP CIDR policy | `IpPolicyService`, `CidrMatcher` | IPv4/IPv6 unit tests and policy CRUD | Verified |
| Risk policy | `RiskService` | Policy CRUD and deterministic implementation | Verified baseline |
| Distributed rate limit | `RedisTokenBucket`, `RateLimitService` | Policy CRUD and Podman Redis startup | Implemented |
| Retry safety | `RouteService`, route locator `Retry` filter | Validation rejects unsafe retry methods | Verified |
| Circuit breakers | route locator `CircuitBreaker` filter | Context/filter construction and Podman startup | Implemented |
| Stable API errors | `ApiError`, both exception handlers | 400, 401, 403, 409 integration assertions | Verified |
| Request correlation | `RequestContextGlobalFilter` | HTTP responses and audit integration | Implemented |
| Audit persistence/search | `AuditService`, `R2dbcAuditRepository` | Bounded search and admin-action tests | Verified |
| Flyway schema | `V1__create_gateway_schema.sql` | Podman PostgreSQL startup | Runtime verified |
| OpenAPI/Swagger | `OpenApiConfig`, controller annotations | OpenAPI title and required path assertions | Verified |
| Health and Prometheus | Actuator configuration | Health integration and Podman checks | Verified |
| Non-root container | `Containerfile`, `compose.yaml` | Podman build/inspect | Runtime verified |
| Postman client workflow | `postman/` | Newman collection | Runtime verified |
| Javadocs | package/type comments, Maven Javadoc plugin | `mvnw clean verify`, `javadoc:javadoc` | Verified |

## Broader Project Requirements Not Claimed By This Release

| Capability | Status | Required future evidence |
| --- | --- | --- |
| JWT audience and algorithm allowlist customization | Planned | Issuer integration tests and explicit validator config |
| Trusted multi-proxy IP resolution | Planned | Forwarded-chain security/property tests |
| Standalone route-permission resources | Planned | Schema, APIs, authorization matrix |
| Persistent idempotent response replay | Planned | Durable idempotency table and duplicate/race tests |
| Audit export jobs | Planned | Bounded export API and authorization tests |
| Risk temporary-block workflow | Planned | Redis block service, expiry and recovery tests |
| Last-known-good multi-instance route reconciliation | Planned | Two-instance failure/convergence tests |
| Redis token-bucket concurrency certification | Planned | Multi-instance load test |
| Performance objectives | Planned | Reproducible load/soak report |
| Dashboards and alert rules | Planned | Deployed Prometheus/Grafana evidence |
| Backup/restore objectives | Planned | Timed restore exercise |

## Test Inventory

| Test class | Primary scope |
| --- | --- |
| `GatewayApplicationTest` | HTTP security, Swagger, routes, clients/keys, all policy types, audit |
| `RouteServiceTest` | Route target/security/retry validation |
| `ApiKeyServiceTest` | Issue, verify, revoke, rotate |
| `RequestSigningServiceTest` | Canonicalization and HMAC validation |
| `CidrMatcherTest` | IPv4/IPv6 CIDR matching |
| `TextListCodecTest` | Durable list encoding |

Surefire, JaCoCo, Javadocs, Podman, and Newman commands are documented in
`README.md` and `docs/OPERATIONS.md`.

## Release Rule

A capability can move from Implemented to Verified only when its evidence runs in
CI or a documented runtime environment. Planned platform requirements are listed
explicitly so this service does not claim behavior it does not provide.
