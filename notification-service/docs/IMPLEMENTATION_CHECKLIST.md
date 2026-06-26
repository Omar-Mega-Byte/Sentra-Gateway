# Notification Service Implementation Checklist

**Purpose:** Executable backlog for implementing the final specification  
**Current state:** Notification-service implementation, tests, OpenAPI,
Podman assets, Postman assets, and scripts are complete in this directory.
Items requiring a live gateway deployment or organization scan tooling are
listed separately as external release gates.

## Foundation

- [x] Create Maven project using Java 25 and pinned Spring Boot 4.
- [x] Commit Maven wrapper and toolchain enforcement.
- [x] Add Spring MVC, validation, Actuator, Micrometer Prometheus, and springdoc.
- [x] Add `.gitignore`, `.env.example`, and `.env` support through Podman.
- [x] Create `NotificationServiceApplication`.
- [x] Add `local`, `test`, and production-like configuration.
- [x] Bind validated typed properties.
- [x] Fail startup on prohibited production-like settings.

## Domain

- [x] Implement immutable `Notification` and `NotificationPreferences`.
- [x] Implement documented channel and status enums.
- [x] Keep subject/tenant/provider/fault internals out of user DTOs.
- [x] Add strict list, preference update, and admin test DTOs.
- [x] Validate page, size, channel, status, title, message, and recipient reference.
- [x] Generate deterministic seed data.
- [x] Implement optimistic preference versioning.

## Repository

- [x] Define repository interface around subject/tenant scoped reads.
- [x] Implement synchronized deterministic in-memory repository.
- [x] Query user collections by tenant and subject.
- [x] Implement preference lookup/update by tenant and subject.
- [x] Enforce fixed `createdAt DESC, id DESC` ordering.
- [x] Reset deterministic data for tests and local restart.
- [x] Expose bounded repository health.

## Trusted Context

- [x] Define consumed `X-Sentra-*` headers.
- [x] Establish and return `X-Request-Id`.
- [x] Reject duplicate security-critical headers.
- [x] Require approved gateway peer or workload identity.
- [x] Require actor type `USER`.
- [x] Require subject for user routes.
- [x] Require exact route ID.
- [x] Require `notifications:read`, `notifications:write`, or `NOTIFICATION_ADMIN`.
- [x] Add direct-bypass and spoofing tests.

## HTTP APIs

- [x] Implement `GET /internal/v1/notifications`.
- [x] Implement `POST /internal/v1/preferences`.
- [x] Implement `POST /internal/v1/test`.
- [x] Enforce JSON media type and body limit.
- [x] Reject unknown JSON/query fields.
- [x] Return exact cache, content-type, and request ID headers.
- [x] Return documented `NTF_*` errors.

## Fault Simulation

- [x] Implement local/test-only delay control.
- [x] Implement local/test-only status override.
- [x] Implement local/test-only malformed response.
- [x] Implement local/test-only disconnect simulation where feasible.
- [x] Bound delay/status values.
- [x] Emit finite fault metrics.
- [x] Fail production-like startup if controls are enabled.
- [x] Ensure fault controls cannot expose sensitive data.

## Errors

- [x] Implement stable `ApiError` and bounded details.
- [x] Implement every documented `NTF_*` code.
- [x] Map malformed JSON, validation, media type, and body limit consistently.
- [x] Preserve request ID and route ID.
- [x] Prevent subject, tenant, body, provider, host, and stack leakage.

## OpenAPI And Javadoc

- [x] Configure OpenAPI title/version/internal server description.
- [x] Annotate all three operations.
- [x] Document trusted headers and local/test fault controls.
- [x] Document all schemas, constraints, enums, examples, and responses.
- [x] State that gateway owns external JWT, retry, timeout, circuit, and fallback behavior.
- [x] Add OpenAPI contract tests.
- [x] Add Javadoc to important application, config, controller, service,
  repository, DTO, fault, and error classes.

## Observability

- [x] Add request correlation filter.
- [x] Emit bounded structured completion logs.
- [x] Add finite operation/result/fault metrics.
- [x] Add repository and preference metrics.
- [x] Add liveness/readiness groups.
- [x] Add repository health check.
- [x] Verify forbidden metric labels.
- [x] Verify logs contain no subject, tenant, body, title, token, role, or scope.

## Container And Podman

- [x] Create multi-stage `Containerfile`.
- [x] Run as fixed non-root UID.
- [x] Support read-only root filesystem and bounded `/tmp`.
- [x] Add `no-new-privileges`.
- [x] Create base `compose.yaml` with no host application port.
- [x] Attach base service to `sentra-gateway_services`.
- [x] Add Compose readiness health check.
- [x] Create local `compose.postman.yaml`.
- [x] Bind local override only to `127.0.0.1:8084`.
- [x] Add non-internal local test network for Podman host forwarding.
- [x] Inspect runtime hardening and networks.
- [x] Exercise graceful shutdown and restart.

## Automated Tests

- [x] Unit-test notification filtering and preference validation.
- [x] Unit-test fault control bounds and profile restrictions.
- [x] Unit-test trusted header parsing.
- [x] Unit-test startup validation.
- [x] Integration-test all HTTP success and denial paths.
- [x] Test preference version conflict and no-retry contract.
- [x] Test local/test delay, status, malformed, and disconnect scenarios.
- [x] Test production-like fault startup rejection.
- [x] Test request ID, health, metrics, Swagger, and OpenAPI.
- [x] Run `clean verify` successfully on host and run Linux container build/start smoke.

## Postman And Gateway E2E

- [x] Create local Postman environment.
- [x] Create Postman collection for direct trusted-header scenarios.
- [x] Run Newman with zero failed assertions.
- [x] Restore deterministic state after collection execution.
- External release gate: exercise external gateway JWT success and denial cases.
- External release gate: verify read retry on eligible transient failure.
- External release gate: verify read timeout/fallback behavior.
- External release gate: verify preference mutation is not retried.
- External release gate: verify circuit open and half-open recovery through admin test route.

## Security And Delivery

- External release gate: run dependency, secret, static, container, and IaC scans.
- External release gate: review applicable OWASP API Security Top 10 risks.
- [x] Confirm production seed, Swagger, and fault controls fail closed.
- [x] Confirm logs and metrics pass leakage review.
- [x] Reconcile README, source docs, OpenAPI, and delivered behavior.
- [x] Complete `OPERATIONS.md` notification-service release checklist with evidence.
- [x] Promote traceability statuses only after evidence exists.

## Definition Of Done

The implementation is complete only when:

1. no unfinished marker, unsupported endpoint, or incomplete branch remains;
2. every documented API and error is represented in OpenAPI;
3. every P0 requirement has automated or documented manual evidence;
4. all Maven, container, Newman, and in-boundary tests pass;
5. the service starts healthy, stops gracefully, and is internally exposed only;
6. logs, errors, metrics, and DTOs pass leakage review; and
7. traceability and operations documentation match the final implementation.

