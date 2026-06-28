# Order Service Implementation Checklist

**Purpose:** Executable backlog for implementing the final specification  
**Current state:** Implementation delivered; executable evidence is summarized below.

## Verification Evidence

- `.\mvnw.cmd -B -ntp clean verify`: 21 tests, zero failures.
- `.\mvnw.cmd -B -ntp javadoc:javadoc`: successful with strict doclint.
- Newman: 19 requests and 19 assertions, zero failures.
- Podman image: built successfully with the full Maven test gate in the build stage.
- Postman deployment: healthy, UID `10001`, read-only root, `no-new-privileges`,
  loopback-only port.
- Base deployment: healthy with no published host port.
- Graceful stop: completed inside the configured 20-second budget; restart healthy.

Unchecked boxes below remain a detailed audit inventory rather than an assertion
that the implementation is absent. Gateway JWT scenarios and organization-level
security scans require the surrounding platform and CI environment.

## Foundation

- [ ] Create Maven project using Java 25 and a pinned Spring Boot 4 release.
- [ ] Commit Maven wrapper and enforce supported Java/Maven versions.
- [ ] Add Spring MVC, validation, Actuator, Micrometer Prometheus, and springdoc.
- [ ] Add reproducible formatting and test configuration.
- [ ] Add `.gitignore`, `.env.example`, and local `.env` support.
- [ ] Create `OrderServiceApplication`.
- [ ] Add profile-specific `local`, `test`, and production-like configuration.
- [ ] Bind configuration into validated typed properties.
- [ ] Fail startup on every prohibited production configuration.

## Domain

- [ ] Implement immutable `Order` and `OrderItem` domain types.
- [ ] Implement `OrderStatus` with exactly the documented values.
- [ ] Keep owner subject and tenant out of user DTOs.
- [ ] Add separate user and administrator order response DTOs.
- [ ] Add strict create request DTO.
- [ ] Validate item count, SKU, quantity, duplicate SKU, and unknown fields.
- [ ] Generate canonical UUID order IDs.
- [ ] Set server-controlled status and RFC 3339 timestamps.

## Repository

- [ ] Define repository interface around owner-scoped queries.
- [ ] Implement synchronized deterministic in-memory repository.
- [ ] Query user collections by tenant and subject, not post-filtered global data.
- [ ] Implement ownership-safe single lookup.
- [ ] Implement admin paginated lookup.
- [ ] Enforce fixed `createdAt DESC, id DESC` ordering.
- [ ] Seed all four documented isolation cases.
- [ ] Reset deterministic data for tests and local restart.
- [ ] Expose bounded repository health.

## Trusted Context

- [ ] Define all consumed `X-Sentra-*` headers.
- [ ] Establish and return `X-Request-Id`.
- [ ] Reject duplicate security-critical headers.
- [ ] Enforce maximum header lengths.
- [ ] Parse gateway role/scope lists using an unambiguous codec.
- [ ] Require approved gateway socket peer or workload identity.
- [ ] Require actor type `USER`.
- [ ] Reject contradictory API-client identity on user routes.
- [ ] Require exact trusted route ID per operation.
- [ ] Require `orders:read`, `orders:write`, or `ORDER_ADMIN`.
- [ ] Add direct-bypass and spoofing tests.

## HTTP APIs

- [ ] Implement `GET /internal/v1/orders`.
- [ ] Implement `GET /internal/v1/orders/{id}`.
- [ ] Implement `POST /internal/v1/orders`.
- [ ] Implement `GET /internal/v1/admin/orders`.
- [ ] Implement bounded zero-based pagination.
- [ ] Reject unknown query parameters.
- [ ] Return safe `404` for unknown and foreign orders.
- [ ] Return `201`, `Location`, no-store, and created representation.
- [ ] Enforce JSON media type and body limit.
- [ ] Add exact cache and content-type headers.

## Idempotency

- [ ] Validate one `Idempotency-Key` header of 1-128 visible ASCII characters.
- [ ] Scope keys by route, normalized tenant partition, and subject.
- [ ] Canonicalize validated request fields and calculate SHA-256 fingerprint.
- [ ] Atomically commit order and idempotency record.
- [ ] Return the original `201`, body, and `Location` for same-payload replay.
- [ ] Return `Idempotency-Replayed` header.
- [ ] Return `409 ORD_IDEMPOTENCY_CONFLICT` for changed payload.
- [ ] Guarantee one order under concurrent duplicate requests.
- [ ] Expire records after configured retention.
- [ ] Bound record count without evicting live safety records.
- [ ] Return `ORD_IDEMPOTENCY_CAPACITY_EXCEEDED` when safe capacity is exhausted.
- [ ] Prevent key/fingerprint logging and metric labeling.

## Errors

- [ ] Implement stable `ApiError` and bounded detail DTO.
- [ ] Implement every documented `ORD_*` code.
- [ ] Map malformed JSON, validation, media type, and body limit consistently.
- [ ] Preserve request ID and trusted route ID in errors.
- [ ] Prevent stack trace, host, repository, owner, payload, and key leakage.
- [ ] Add exact schema and status assertions for every error class.

## OpenAPI And Javadoc

- [ ] Configure OpenAPI title/version/internal server description.
- [ ] Annotate all four operations.
- [ ] Document every trusted header and `Idempotency-Key`.
- [ ] Document pagination, DTOs, constraints, enums, examples, and responses.
- [ ] State that the gateway performs external JWT authentication.
- [ ] Add OpenAPI contract tests for every path/header/status/schema.
- [ ] Add Javadoc to application, configuration, controllers, services,
  repository contracts, domain types, DTOs, idempotency, and error components.
- [ ] Generate browsable and Javadoc JAR artifacts without warnings.

## Observability

- [ ] Add request correlation filter and bounded structured completion logs.
- [ ] Add finite operation/result metrics.
- [ ] Add repository and idempotency metrics.
- [ ] Add liveness and readiness groups.
- [ ] Add repository health indicator.
- [ ] Expose Prometheus only through protected/internal operations access.
- [ ] Verify no forbidden metric labels.
- [ ] Verify logs contain no subject, tenant, order, SKU, body, token, or key.

## Container And Podman

- [ ] Create multi-stage `Containerfile`.
- [ ] Run as fixed non-root UID.
- [ ] Support read-only root filesystem and bounded `/tmp`.
- [ ] Add `no-new-privileges`.
- [ ] Create base `compose.yaml` with no host application port.
- [ ] Attach base service to `sentra-gateway_services`.
- [ ] Add Compose readiness health check.
- [ ] Create local `compose.postman.yaml`.
- [ ] Bind local override only to `127.0.0.1:8082`.
- [ ] Add a non-internal local test network for Podman host forwarding.
- [ ] Validate rendered Compose configurations.
- [ ] Inspect runtime user, filesystem, security options, networks, and ports.
- [ ] Exercise graceful shutdown and restart.

## Automated Tests

- [ ] Unit-test order validation and normalization.
- [ ] Unit-test repository ordering and ownership.
- [ ] Unit-test pagination bounds.
- [ ] Unit-test trusted list codec and peer matcher.
- [ ] Unit-test startup validation.
- [ ] Integration-test all HTTP success and denial paths.
- [ ] Test exact foreign-subject and foreign-tenant `404` behavior.
- [ ] Test idempotency replay, conflict, expiry, capacity, and concurrency.
- [ ] Test request ID, health, metrics, Swagger, and OpenAPI.
- [ ] Test oversized, malformed, unsupported, and unknown input.
- [ ] Add gateway/downstream route/header compatibility fixtures.
- [ ] Add architecture tests for package boundaries where useful.
- [ ] Run `clean verify` successfully on host and Linux container.

## Postman And End-To-End

- [ ] Create local Postman environment.
- [ ] Create full Postman collection for all documented scenarios.
- [ ] Add Newman assertions for response status, shape, headers, and isolation.
- [ ] Restore/reset deterministic data after collection execution.
- [ ] Run the complete collection with zero failed requests/assertions.
- [ ] Exercise all four external gateway routes with JWTs.
- [ ] Verify invalid/expired JWTs never reach the service.
- [ ] Verify externally spoofed trusted headers are removed.
- [ ] Verify bearer JWT is not forwarded downstream.
- [ ] Verify gateway does not automatically retry create by default.

## Security And Delivery

- [ ] Run dependency, secret, static, container, and IaC scans.
- [ ] Review against applicable OWASP API Security Top 10 risks.
- [ ] Confirm base management endpoints are not public.
- [ ] Confirm production seed and Swagger controls fail closed.
- [ ] Add dashboard and alert assets or record their owning platform work.
- [ ] Reconcile README, source docs, OpenAPI, and delivered behavior.
- [ ] Complete `OPERATIONS.md` release checklist with evidence.
- [ ] Promote traceability statuses only after evidence exists.

## Definition Of Done

The implementation is complete only when:

1. no TODO, placeholder, unsupported endpoint, or unfinished branch remains;
2. every documented API and error is represented in OpenAPI;
3. every P0 requirement has automated or documented manual evidence;
4. all Maven, container, Newman, and gateway end-to-end tests pass;
5. the service starts healthy, stops gracefully, and is internally exposed only;
6. logs, errors, metrics, and DTOs pass leakage review; and
7. traceability and operations documentation match the final implementation.
