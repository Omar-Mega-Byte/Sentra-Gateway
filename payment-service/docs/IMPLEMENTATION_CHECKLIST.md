# Payment Service Implementation Checklist

**Purpose:** Executable backlog for implementing the final specification  
**Current state:** Baseline internal service implementation is present. The list
below remains the release verification inventory; gateway E2E and security-scan
items require external tooling and a running gateway stack.

## Foundation

- [ ] Create Maven project using Java 25 and pinned Spring Boot 4.
- [ ] Commit Maven wrapper and toolchain enforcement.
- [ ] Add Spring MVC, validation, Actuator, Micrometer Prometheus, and springdoc.
- [ ] Add `.gitignore`, `.env.example`, and `.env` support through Podman.
- [ ] Create `PaymentServiceApplication`.
- [ ] Add `local`, `test`, and production-like configuration.
- [ ] Bind validated typed properties.
- [ ] Fail startup on prohibited production-like settings.

## Domain

- [ ] Implement immutable `Payment` and `Refund` domain types.
- [ ] Implement documented status enums.
- [ ] Keep client/key/signature/idempotency internals out of partner DTOs.
- [ ] Add strict payment and refund request DTOs.
- [ ] Validate merchant reference, amount, currency, and description.
- [ ] Use decimal money arithmetic only.
- [ ] Generate service-owned UUIDs and RFC 3339 timestamps.
- [ ] Seed deterministic payment/refund data.

## Repository

- [ ] Define repository interface around client-scoped lookups.
- [ ] Implement synchronized deterministic in-memory repository.
- [ ] Query reads by client ID rather than post-filtering global data.
- [ ] Return safe `404` for foreign-client records.
- [ ] Enforce merchant-reference uniqueness per client.
- [ ] Track refundable amount safely.
- [ ] Reset deterministic data for tests and local restart.
- [ ] Expose bounded repository health.

## Trusted Context

- [ ] Define consumed `X-Sentra-*` headers.
- [ ] Establish and return `X-Request-Id`.
- [ ] Reject duplicate security-critical headers.
- [ ] Reject raw external credential/signature headers downstream.
- [ ] Require approved gateway peer or workload identity.
- [ ] Require actor type `API_CLIENT`.
- [ ] Require client ID and key ID.
- [ ] Require exact route ID.
- [ ] Require operation scope.
- [ ] Require signature evidence for mutation routes.
- [ ] Contract-test signature evidence header names with gateway-service.

## HTTP APIs

- [ ] Implement `GET /internal/v1/payments/{id}`.
- [ ] Implement `POST /internal/v1/payments`.
- [ ] Implement `POST /internal/v1/refunds`.
- [ ] Enforce JSON media type and body limit.
- [ ] Reject unknown JSON fields.
- [ ] Return `201`, `Location`, no-store, and created representation.
- [ ] Return exact documented error statuses and codes.

## Idempotency

- [ ] Require one `Idempotency-Key` header for payment and refund mutations.
- [ ] Scope key by route and client ID.
- [ ] Compute fingerprint from validated canonical request fields.
- [ ] Atomically commit mutation and idempotency record.
- [ ] Return original response for same-key same-payload replay.
- [ ] Return `PAY_IDEMPOTENCY_CONFLICT` for changed payload.
- [ ] Guarantee one mutation under concurrent duplicates.
- [ ] Expire records after configured retention.
- [ ] Bound record count safely.
- [ ] Prevent keys/fingerprints from logs and metrics.

## Errors

- [ ] Implement stable `ApiError` and bounded details.
- [ ] Implement every documented `PAY_*` code.
- [ ] Map malformed JSON, validation, media type, and body limit consistently.
- [ ] Preserve request ID and trusted route ID.
- [ ] Prevent credential, cryptographic, body, repository, and host leakage.

## OpenAPI And Javadoc

- [ ] Configure OpenAPI title/version/internal server description.
- [ ] Annotate all three operations.
- [ ] Document trusted client and signature evidence headers.
- [ ] Document `Idempotency-Key`.
- [ ] Document all schemas, constraints, enums, examples, and responses.
- [ ] State that gateway validates API keys, HMAC signatures, and replay nonces.
- [ ] Add OpenAPI contract tests.
- [ ] Add Javadoc to important application, config, controller, service,
  repository, DTO, idempotency, and error classes.

## Observability

- [ ] Add request correlation filter.
- [ ] Emit bounded structured completion logs.
- [ ] Add finite operation/result metrics.
- [ ] Add repository and idempotency metrics.
- [ ] Add liveness/readiness groups.
- [ ] Add repository/idempotency health checks.
- [ ] Verify forbidden metric labels.
- [ ] Verify logs contain no keys, signatures, nonces, bodies, or idempotency data.

## Container And Podman

- [ ] Create multi-stage `Containerfile`.
- [ ] Run as fixed non-root UID.
- [ ] Support read-only root filesystem and bounded `/tmp`.
- [ ] Add `no-new-privileges`.
- [ ] Create base `compose.yaml` with no host application port.
- [ ] Attach base service to `sentra-gateway_services`.
- [ ] Add Compose readiness health check.
- [ ] Create local `compose.postman.yaml`.
- [ ] Bind local override only to `127.0.0.1:8083`.
- [ ] Add non-internal local test network for Podman host forwarding.
- [ ] Inspect runtime hardening and networks.
- [ ] Exercise graceful shutdown and restart.

## Automated Tests

- [ ] Unit-test money validation and normalization.
- [ ] Unit-test repository ownership and refundable amount.
- [ ] Unit-test trusted header parsing.
- [ ] Unit-test startup validation.
- [ ] Integration-test all HTTP success and denial paths.
- [ ] Test foreign-client reads and refunds.
- [ ] Test idempotency replay, conflict, expiry, capacity, and concurrency.
- [ ] Test malformed, unsupported, oversized, and unknown input.
- [ ] Test request ID, health, metrics, Swagger, and OpenAPI.
- [ ] Add gateway/downstream route/header compatibility fixtures.
- [ ] Run `clean verify` successfully on host and Linux container.

## Postman And Gateway E2E

- [ ] Create local Postman environment.
- [ ] Create Postman collection for all direct trusted-header scenarios.
- [ ] Run Newman with zero failed assertions.
- [ ] Restore/reset deterministic state after collection execution.
- [ ] Exercise external gateway API-key success and denial cases.
- [ ] Exercise HMAC body mutation, path/query normalization, timestamp skew, and replay.
- [ ] Verify Redis nonce outage fails closed.
- [ ] Verify plaintext API key and external signature are not forwarded.
- [ ] Verify POST payment timeout is not automatically retried.

## Security And Delivery

- [ ] Run dependency, secret, static, container, and IaC scans.
- [ ] Review applicable OWASP API Security Top 10 risks.
- [ ] Confirm production seed and Swagger controls fail closed.
- [ ] Confirm logs and metrics pass leakage review.
- [ ] Reconcile README, source docs, OpenAPI, and delivered behavior.
- [ ] Complete `OPERATIONS.md` release checklist with evidence.
- [ ] Promote traceability statuses only after evidence exists.

## Definition Of Done

The implementation is complete only when:

1. no temporary marker, unsupported endpoint, or unfinished branch remains;
2. every documented API and error is represented in OpenAPI;
3. every P0 requirement has automated or documented manual evidence;
4. all Maven, container, Newman, and gateway signature/replay tests pass;
5. the service starts healthy, stops gracefully, and is internally exposed only;
6. logs, errors, metrics, and DTOs pass leakage review; and
7. traceability and operations documentation match the final implementation.
