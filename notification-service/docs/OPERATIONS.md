# Notification Service Operations Guide

**Version:** 1.0.0  
**Status:** Implemented notification-service runbook  
**Internal service port:** `8084`

## Current State

`notification-service` contains the Maven build, Spring Boot implementation,
tests, Containerfile, Podman Compose files, Postman/Newman collection, local
environment template, and helper scripts. The service is internally scoped; use
the Postman override only for direct local testing.

## Prerequisites

- Java 25
- Maven wrapper committed by the service
- Podman with Compose support
- Node.js/npm for Newman
- running `gateway-service` for JWT and resilience end-to-end tests
- shared internal network `sentra-gateway_services`

## Required Files

```text
notification-service/
  .env.example
  .gitignore
  Containerfile
  compose.yaml
  compose.postman.yaml
  mvnw
  mvnw.cmd
  pom.xml
  postman/
    Sentra_Notification_Service.postman_collection.json
    Sentra_Notification_Service_Local.postman_environment.json
  src/
```

## Build Gate

```powershell
.\mvnw.cmd -B -ntp clean verify
.\mvnw.cmd -B -ntp javadoc:javadoc
```

Expected artifacts:

```text
target/notification-service-1.0.0-SNAPSHOT.jar
target/notification-service-1.0.0-SNAPSHOT-javadoc.jar
target/reports/apidocs/index.html
target/site/jacoco/index.html
```

## Local JVM

```powershell
Copy-Item .env.example .env
.\scripts\run-local.ps1
```

`run-local.ps1` preserves explicit process environment variables, then loads
`.env`, then applies local-safe defaults.

## Podman Base Deployment

Create the shared internal network only when absent:

```powershell
podman network create --internal sentra-gateway_services
```

Run without publishing the application port:

```powershell
Copy-Item .env.example .env
podman compose --env-file .env config
podman compose --env-file .env up --build -d
podman compose ps
```

The base `compose.yaml` must use `expose: 8084`, attach to the internal services
network, run non-root, use a read-only root filesystem, set `no-new-privileges`,
mount bounded `/tmp`, and define a readiness health check.

## Local Postman Deployment

```powershell
podman compose --env-file .env `
  -f compose.yaml `
  -f compose.postman.yaml `
  up --build -d
```

The override must publish `127.0.0.1:8084 -> 8084` and add a non-internal local
test network for Podman host forwarding.

## Health And Documentation

```text
http://localhost:8084/actuator/health/liveness
http://localhost:8084/actuator/health/readiness
http://localhost:8084/actuator/prometheus
http://localhost:8084/v3/api-docs
http://localhost:8084/swagger-ui.html
```

## Direct Local Smoke Tests

List notifications:

```powershell
$readHeaders = @{
  "X-Sentra-Request-Id" = "local-notifications-read-001"
  "X-Sentra-Subject" = "sentra-user-omar"
  "X-Sentra-Actor-Type" = "USER"
  "X-Sentra-Tenant-Id" = "tenant-demo"
  "X-Sentra-Scopes" = "notifications:read"
  "X-Sentra-Route-Id" = "notifications-list"
}

Invoke-RestMethod `
  -Uri "http://localhost:8084/internal/v1/notifications?page=0&size=20" `
  -Headers $readHeaders
```

Update preferences:

```powershell
$writeHeaders = @{
  "X-Sentra-Request-Id" = "local-preferences-update-001"
  "X-Sentra-Subject" = "sentra-user-omar"
  "X-Sentra-Actor-Type" = "USER"
  "X-Sentra-Tenant-Id" = "tenant-demo"
  "X-Sentra-Scopes" = "notifications:write"
  "X-Sentra-Route-Id" = "notification-preferences-update"
}

$body = @{
  emailEnabled = $true
  smsEnabled = $false
  pushEnabled = $true
  webhookEnabled = $false
  version = 2
} | ConvertTo-Json

Invoke-WebRequest `
  -Method Post `
  -Uri "http://localhost:8084/internal/v1/preferences" `
  -Headers $writeHeaders `
  -ContentType "application/json" `
  -Body $body
```

Admin test:

```powershell
$adminHeaders = @{
  "X-Sentra-Request-Id" = "local-admin-test-001"
  "X-Sentra-Subject" = "sentra-admin"
  "X-Sentra-Actor-Type" = "USER"
  "X-Sentra-Roles" = "NOTIFICATION_ADMIN"
  "X-Sentra-Route-Id" = "admin-test-notification"
}

$testBody = @{
  scenario = "SUCCESS"
  channel = "EMAIL"
  recipientReference = "local-recipient"
  message = "Gateway resilience smoke"
} | ConvertTo-Json

Invoke-WebRequest `
  -Method Post `
  -Uri "http://localhost:8084/internal/v1/test" `
  -Headers $adminHeaders `
  -ContentType "application/json" `
  -Body $testBody
```

## Postman And Newman

Required collection assets:

```text
postman/Sentra_Notification_Service.postman_collection.json
postman/Sentra_Notification_Service_Local.postman_environment.json
```

Required collection flow:

1. readiness is `UP`;
2. OpenAPI contains all three operations;
3. owned notification list succeeds;
4. subject/tenant isolation is preserved;
5. missing trusted context returns `401`;
6. wrong actor, scope, role, or route returns `403`;
7. preference update succeeds;
8. stale preference version returns `409`;
9. unsupported media, malformed, unknown field, and oversized bodies are rejected;
10. admin test success returns accepted result;
11. local/test delay, failure, malformed, and disconnect scenarios are available;
12. deterministic data is restored after restart.

CLI:

```powershell
.\scripts\newman.ps1
```

## Gateway End-To-End Verification

External routes:

```text
GET  /api/v1/notifications
POST /api/v1/notifications/preferences
POST /api/v1/admin/test-notification
```

Required scenarios:

- valid JWT user with `notifications:read`;
- missing/invalid/expired JWT never reaches service;
- missing `notifications:read` or `notifications:write`;
- valid preference mutation is not automatically retried;
- read route transient failure receives one bounded retry when route policy permits;
- slow read route produces gateway timeout or fallback per policy;
- repeated admin test failures open circuit;
- half-open recovery closes circuit after success;
- fallback content is gateway-owned and never reports mutation success falsely.

## Security Inspection

```powershell
podman inspect sentra-notification_notification-service_1 --format `
  "user={{.Config.User}} readOnly={{.HostConfig.ReadonlyRootfs}} security={{json .HostConfig.SecurityOpt}} health={{.State.Health.Status}} ports={{json .NetworkSettings.Ports}}"
```

Expected: non-root user, read-only filesystem, `no-new-privileges`, healthy
state, no host port in base deployment, loopback-only port in local override.

## Graceful Shutdown

```powershell
podman stop --time 20 sentra-notification_notification-service_1
podman logs --tail 100 sentra-notification_notification-service_1
podman start sentra-notification_notification-service_1
podman healthcheck run sentra-notification_notification-service_1
```

Expected logs show graceful shutdown completion before timeout and healthy
restart.

## Monitoring

```powershell
$metrics = Invoke-WebRequest `
  -UseBasicParsing `
  http://localhost:8084/actuator/prometheus

$metrics.Content | Select-String "sentra_notification_"
```

Metric labels must not contain subjects, tenants, notification IDs, request IDs,
IPs, raw paths, titles, message bodies, tokens, roles, or scopes.

Suggested alerts:

- readiness unavailable;
- elevated `5xx`;
- p95 latency above approved threshold;
- fault scenario spike outside test windows;
- gateway circuit-open state;
- retry/fallback spike;
- Prometheus scrape failure.

## Failure Behavior

| Failure | Required behavior |
| --- | --- |
| Missing trusted context | `401 NTF_TRUSTED_CONTEXT_REQUIRED` |
| Wrong actor/scope/role/route | stable `403` |
| Fault controls in production-like profile | startup failure |
| Invalid list query | `400 NTF_REQUEST_INVALID` |
| Stale preference version | `409 NTF_VERSION_CONFLICT` |
| Oversized body | `413` before domain work |
| Unsupported media type | `415` |
| Repository unavailable | `503`, readiness down where unsafe |
| Read timeout | gateway `504` or fallback per route |
| Admin repeated failure | gateway circuit opens |

## Runtime Data

Memory mode is non-durable:

- restart restores deterministic seed data;
- preferences reset;
- fault counters reset;
- horizontal replicas do not share writes.

## Release Checklist

- [x] `clean verify` passes.
- [x] Javadoc and JaCoCo reports are generated.
- [x] OpenAPI matches `API_CONTRACT.md`.
- [x] Postman/Newman assertions pass.
- [x] Production-like startup rejects fault controls.
- [x] Base deployment publishes no host port.
- [x] Container hardening inspection passes.
- [x] Graceful stop and healthy restart pass.
- [x] Logs and metrics contain no forbidden values.
- [x] Documentation and traceability match delivered behavior.
- External release gate: gateway retry/timeout/circuit/fallback scenarios pass.
- External release gate: gateway preference mutation no-retry behavior is verified.
- External release gate: dependency, secret, static, container, and IaC scans pass.
