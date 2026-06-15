# User Service Operations Guide

**Status:** Executable runbook  
**Target runtime:** Java 25, Spring Boot, internal port `8081`

## Current State

`user-service` contains a Maven build, Java source, automated tests, typed
configuration, Swagger/OpenAPI, Podman definitions, Javadocs, and Postman assets.
The release baseline uses deterministic in-memory state and does not claim
durable persistence.

## Build Gate

Run from `user-service`:

```powershell
.\mvnw.cmd clean verify
```

The build gate shall compile, test, package, generate coverage, and validate
OpenAPI/contract tests.

Javadocs command:

```powershell
.\mvnw.cmd javadoc:javadoc
```

## Local Startup

Target local command:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
.\mvnw.cmd spring-boot:run
```

Local direct access is a developer convenience only. End-to-end verification must
send external profile requests through `gateway-service`.

## Container Deployment

The Compose topology:

- attach `user-service` to an internal service network;
- avoid publishing port `8081` by default;
- let `gateway-service` reach `http://user-service:8081`;
- restrict management scraping to the operations network;
- run the container as non-root; and
- use a read-only root filesystem where supported.

The base topology requires the gateway services network and does not publish the
application port:

```powershell
podman network inspect sentra-gateway_services
podman compose --env-file .env config
podman compose --env-file .env up --build -d
podman compose ps
```

Create the shared internal network when gateway-service has not created it:

```powershell
podman network create --internal sentra-gateway_services
```

For direct Postman testing, add the local test network and publish port 8081 on
loopback only:

```powershell
podman compose --env-file .env `
  -f compose.yaml `
  -f compose.postman.yaml `
  up --build -d
```

## Health Verification

From an approved internal network path:

```powershell
Invoke-RestMethod http://user-service:8081/actuator/health/liveness
Invoke-RestMethod http://user-service:8081/actuator/health/readiness
```

Expected status is `UP`.

Liveness reports whether the process can operate. It must not fail solely because
an optional external dependency is unavailable. Readiness reports whether the
instance can safely serve the documented profile operations.

For the in-memory baseline, readiness depends on application initialization and
repository availability. It does not depend on gateway reachability.

## API Smoke Verification

Public request through the gateway:

```powershell
Invoke-RestMethod `
  http://localhost:8080/api/v1/public/users/7aa99db8-a943-4b63-b4b7-79f769ef9f87
```

Authenticated self-read:

```powershell
$headers = @{ Authorization = "Bearer <valid-profile-read-token>" }
Invoke-RestMethod `
  http://localhost:8080/api/v1/users/me `
  -Headers $headers
```

Authenticated update:

```powershell
$headers = @{
  Authorization = "Bearer <valid-profile-write-token>"
  "Content-Type" = "application/json"
}
$body = @{
  displayName = "Omar H."
  version = 1
} | ConvertTo-Json

Invoke-RestMethod `
  http://localhost:8080/api/v1/users/me `
  -Method Patch `
  -Headers $headers `
  -Body $body
```

The deterministic local profile is:

```text
Profile ID: 7aa99db8-a943-4b63-b4b7-79f769ef9f87
Subject: sentra-user-omar
Initial version: 3
```

Direct internal testing supplies trusted headers and does not use bearer tokens.
External JWT testing remains a gateway/identity-environment workflow.

## Postman And Newman

Import:

- `postman/Sentra_User_Service.postman_collection.json`
- `postman/Sentra_User_Service_Local.postman_environment.json`

Run from the CLI:

```powershell
npx --yes newman run `
  postman/Sentra_User_Service.postman_collection.json `
  -e postman/Sentra_User_Service_Local.postman_environment.json
```

## Security Smoke Verification

The release smoke suite shall prove:

1. a public response contains only the four public fields;
2. `/me` without a JWT is rejected by the gateway;
3. a JWT without `profile:read` cannot read `/me`;
4. a JWT without `profile:write` cannot update `/me`;
5. a forged external `X-Sentra-Subject` cannot select another profile;
6. a direct protected internal call without trusted provenance is rejected;
7. actor type `API_CLIENT` cannot access `/me`;
8. an old profile version returns `409`;
9. an unknown or immutable JSON field returns `400`;
10. an oversized body returns `413`;
11. every response has a correlatable request ID; and
12. logs contain no token, email, subject, or profile body.

## OpenAPI Verification

From an approved local or operations path:

```powershell
Invoke-RestMethod http://user-service:8081/v3/api-docs
```

The specification must contain:

- `GET /internal/v1/users/{id}/public`;
- `GET /internal/v1/users/me`;
- `PATCH /internal/v1/users/me`;
- trusted header parameters;
- all success and error schemas; and
- documented validation constraints.

## Runtime Data

In-memory mode owns:

- deterministic profile records;
- subject-to-profile lookup state; and
- optimistic versions.

All state is instance-local and disappears on restart. Multiple instances do not
share updates in this mode. Therefore, state-changing demonstrations shall use one
user-service instance unless a shared durable repository is implemented.

Do not treat in-memory mode as a production persistence design.

## Graceful Shutdown

The target shutdown sequence is:

1. mark the instance not ready;
2. stop routing new traffic;
3. allow in-flight requests to finish within `SHUTDOWN_TIMEOUT`;
4. stop the HTTP server and repository resources; and
5. terminate without accepting new work.

An in-memory repository has no durability guarantee during shutdown.

## Monitoring

Prometheus endpoint:

```text
/actuator/prometheus
```

Minimum dashboard signals:

- request rate by normalized route and method;
- p50/p95/p99 request duration;
- status-class and service-error counts;
- public lookup hit/not-found counts;
- profile update success/conflict counts;
- readiness and restart count;
- JVM memory, threads, CPU, and garbage collection; and
- repository dependency health if a durable store is introduced.

Never use profile ID, subject, email, request ID, source IP, or raw path as a
metric label.

## Failure Behavior

### Missing Gateway Context

- protected `/me` operations return `401 USR_TRUSTED_CONTEXT_REQUIRED`;
- no profile lookup or mutation occurs;
- the event is logged without sensitive header values.

### Invalid Actor Or Scope

- invalid actor type returns `403 USR_ACTOR_NOT_ALLOWED`;
- missing expected scope returns `403 USR_SCOPE_REQUIRED`;
- no mutation occurs.

### Repository Failure

- reads and writes return `503 USR_DEPENDENCY_UNAVAILABLE`;
- readiness becomes `DOWN` when the required repository cannot serve requests;
- no fabricated profile response is returned.

### Optimistic Conflict

- update returns `409 USR_VERSION_CONFLICT`;
- the stored record remains unchanged;
- clients retrieve the current profile before retrying.

### Gateway Or Network Failure

- the service does not attempt to authenticate external clients directly;
- the gateway applies its configured timeout/circuit behavior;
- no fallback may fabricate a successful profile mutation.

## Incident Checks

For elevated `401`/`403`:

1. verify gateway route category and required scope;
2. verify trusted headers are created after inbound-header sanitation;
3. verify route ID allowlists match deployed route IDs;
4. verify the request is arriving from the approved network path; and
5. inspect sanitized logs by request ID.

For elevated `409`:

1. check whether clients reuse stale profile representations;
2. confirm each successful update increments once;
3. confirm gateway retries are disabled for `PATCH`; and
4. inspect concurrent update patterns without logging profile values.

For public-data exposure:

1. disable the public gateway route;
2. preserve request IDs and deployment evidence;
3. verify the public DTO field allowlist;
4. inspect OpenAPI and serialization changes;
5. rotate affected data or credentials if needed; and
6. add a regression test before restoration.

## Backup And Restore

No backup exists for in-memory mode.

Before enabling durable storage, document and verify:

- backup scope and encryption;
- recovery point and recovery time objectives;
- migration compatibility;
- restore into an isolated environment;
- subject and email uniqueness after restore;
- profile version continuity; and
- post-restore gateway smoke tests.

## Release Checklist

1. `.\mvnw.cmd clean verify` passes.
2. Public DTO redaction tests pass.
3. Trusted-header spoofing and bypass tests pass.
4. JWT scope end-to-end cases pass through the gateway.
5. Optimistic update and no-retry behavior pass.
6. Error and request-ID contracts match the docs.
7. OpenAPI contains all paths, schemas, and responses.
8. Liveness, readiness, metrics, and Prometheus checks pass.
9. The application port is not externally published.
10. The image runs as non-root with no embedded secrets.
11. Production-like startup rejects unsafe configuration.
12. Logs and metrics contain no sensitive or high-cardinality values.
13. `REQUIREMENTS_TRACEABILITY.md` links every verified claim to evidence.
14. Rollback and, if applicable, data migration compatibility are demonstrated.
