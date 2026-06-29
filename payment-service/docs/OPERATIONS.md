# Payment Service Operations Guide

**Version:** 1.0.0  
**Status:** Implemented runbook for the baseline memory service  
**Internal service port:** `8083`

## Current State

`payment-service` now contains a Maven build, Spring Boot implementation,
container image definition, Compose files, and Postman collection. Direct service
tests exercise the trusted-header contract. Gateway API-key, HMAC, nonce, replay,
and no-retry scenarios still require a running `gateway-service` deployment.

## Prerequisites

- Java 25
- Maven wrapper committed by the service
- Podman with Compose support
- Node.js/npm for Newman
- running `gateway-service` for API-key, signature, replay, and route tests
- shared internal network `sentra-gateway_services`

## Required Files

```text
payment-service/
  .env.example
  .gitignore
  Containerfile
  compose.yaml
  compose.postman.yaml
  mvnw
  mvnw.cmd
  pom.xml
  postman/
    Sentra_Payment_Service.postman_collection.json
    Sentra_Payment_Service_Local.postman_environment.json
  src/
```

## Build Gate

Required commands:

```powershell
.\mvnw.cmd -B -ntp clean verify
.\mvnw.cmd -B -ntp javadoc:javadoc
```

Expected artifacts:

```text
target/payment-service-1.0.0-SNAPSHOT.jar
target/payment-service-1.0.0-SNAPSHOT-javadoc.jar
target/reports/apidocs/index.html
target/site/jacoco/index.html
```

The build fails for compilation errors, test failures, Javadoc errors, or
toolchain violations.

## Local JVM

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:PAYMENT_SEED_ENABLED = "true"
$env:OPENAPI_ENABLED = "true"
$env:SWAGGER_UI_ENABLED = "true"
.\mvnw.cmd spring-boot:run
```

Local direct calls simulate trusted headers only for service development. They
do not prove API-key, HMAC, nonce, or rate-limit gateway behavior.

## Podman Base Deployment

Create the shared internal network only when the gateway stack has not already
created it:

```powershell
podman network create --internal sentra-gateway_services
```

Build and run without publishing the application port:

```powershell
Copy-Item .env.example .env
podman compose --env-file .env config
podman compose --env-file .env up --build -d
podman compose ps
```

The base `compose.yaml` must:

- use `expose: 8083`, not a host `ports` mapping;
- attach to `sentra-gateway_services`;
- run non-root;
- use a read-only root filesystem;
- set `no-new-privileges`;
- mount bounded `/tmp` as `tmpfs`;
- define a readiness health check.

## Local Postman Deployment

```powershell
podman compose --env-file .env `
  -f compose.yaml `
  -f compose.postman.yaml `
  up --build -d
```

The override must publish:

```text
127.0.0.1:8083 -> container 8083
```

With Podman, a container attached only to internal networks cannot receive host
port forwarding. The local override therefore also needs a non-internal test
network. This network must not appear in the base deployment.

## Health And Documentation

```text
http://localhost:8083/actuator/health/liveness
http://localhost:8083/actuator/health/readiness
http://localhost:8083/actuator/prometheus
http://localhost:8083/v3/api-docs
http://localhost:8083/swagger-ui.html
```

Verification:

```powershell
Invoke-RestMethod http://localhost:8083/actuator/health/liveness
Invoke-RestMethod http://localhost:8083/actuator/health/readiness
Invoke-WebRequest http://localhost:8083/v3/api-docs
Invoke-WebRequest http://localhost:8083/swagger-ui/index.html
```

Expected health status is `UP`.

## Direct Local Smoke Tests

These calls simulate headers created by the gateway. They are not partner-facing
examples.

Read payment:

```powershell
$readHeaders = @{
  "X-Sentra-Request-Id" = "local-payment-read-001"
  "X-Sentra-Actor-Type" = "API_CLIENT"
  "X-Sentra-Client-Id" = "partner-acme"
  "X-Sentra-Key-Id" = "key-acme-active"
  "X-Sentra-Scopes" = "payments:read"
  "X-Sentra-Route-Id" = "partner-payment-read"
}

Invoke-RestMethod `
  -Uri "http://localhost:8083/internal/v1/payments/40000000-0000-4000-8000-000000000001" `
  -Headers $readHeaders
```

Create payment:

```powershell
$writeHeaders = @{
  "X-Sentra-Request-Id" = "local-payment-create-001"
  "X-Sentra-Actor-Type" = "API_CLIENT"
  "X-Sentra-Client-Id" = "partner-acme"
  "X-Sentra-Key-Id" = "key-acme-active"
  "X-Sentra-Scopes" = "payments:write"
  "X-Sentra-Route-Id" = "partner-payment-create"
  "X-Sentra-Signature-Verified" = "true"
  "X-Sentra-Signature-Key-Id" = "sig-key-acme-active"
  "X-Sentra-Nonce-Status" = "accepted"
  "Idempotency-Key" = "local-payment-create-001"
}

$body = @{
  merchantReference = "acme-order-local-001"
  amount = "125.50"
  currency = "USD"
  description = "Local payment-service smoke"
} | ConvertTo-Json

Invoke-WebRequest `
  -Method Post `
  -Uri "http://localhost:8083/internal/v1/payments" `
  -Headers $writeHeaders `
  -ContentType "application/json" `
  -Body $body
```

Create refund:

```powershell
$refundHeaders = @{
  "X-Sentra-Request-Id" = "local-refund-create-001"
  "X-Sentra-Actor-Type" = "API_CLIENT"
  "X-Sentra-Client-Id" = "partner-acme"
  "X-Sentra-Key-Id" = "key-acme-active"
  "X-Sentra-Scopes" = "refunds:write"
  "X-Sentra-Route-Id" = "partner-refund-create"
  "X-Sentra-Signature-Verified" = "true"
  "X-Sentra-Signature-Key-Id" = "sig-key-acme-active"
  "X-Sentra-Nonce-Status" = "accepted"
  "Idempotency-Key" = "local-refund-create-001"
}

$refundBody = @{
  paymentId = "40000000-0000-4000-8000-000000000001"
  merchantReference = "acme-refund-local-001"
  amount = "25.00"
} | ConvertTo-Json

Invoke-WebRequest `
  -Method Post `
  -Uri "http://localhost:8083/internal/v1/refunds" `
  -Headers $refundHeaders `
  -ContentType "application/json" `
  -Body $refundBody
```

Replay the exact create/refund request with the same idempotency key. Expected:

- `201`;
- same body and `Location`;
- `Idempotency-Replayed: true`;
- one stored mutation.

Change the body and reuse the same key. Expected:

```text
409 PAY_IDEMPOTENCY_CONFLICT
```

## Postman And Newman

Required collection assets:

```text
postman/Sentra_Payment_Service.postman_collection.json
postman/Sentra_Payment_Service_Local.postman_environment.json
```

Required collection flow:

1. readiness is `UP`;
2. OpenAPI contains all three operations;
3. owned payment read succeeds;
4. foreign-client payment read returns `404`;
5. missing trusted context returns `401`;
6. wrong actor, scope, route, or signature evidence returns `403`;
7. create payment returns `201`;
8. payment idempotency replay succeeds;
9. payment idempotency conflict returns `409`;
10. create refund returns `201`;
11. refund idempotency replay succeeds;
12. invalid amount, currency, unknown field, unsupported media type, and
    oversized body are rejected;
13. deterministic data is restored after restart.

CLI:

```powershell
npx --yes newman run `
  postman/Sentra_Payment_Service.postman_collection.json `
  -e postman/Sentra_Payment_Service_Local.postman_environment.json `
  --reporters cli
```

## Gateway End-To-End Verification

Direct trusted-header tests do not prove API-key or signature behavior. A release
candidate must also call the external gateway routes:

```text
GET  /api/v1/partner/payments/{id}
POST /api/v1/partner/payments
POST /api/v1/partner/refunds
```

Required gateway scenarios:

- valid active API key with required scope;
- missing API key;
- invalid API key;
- revoked or expired key;
- missing route scope;
- valid signed payment create;
- body mutation after signing;
- path/query canonicalization mismatch;
- stale timestamp;
- future timestamp;
- reused nonce;
- Redis nonce outage on signed route fails closed;
- plaintext API key and external signature are not forwarded;
- POST payment timeout is not automatically retried;
- gateway audit records contain non-sensitive reason codes.

## Security Inspection

Container inspection:

```powershell
podman inspect sentra-payment_payment-service_1 --format `
  "user={{.Config.User}} readOnly={{.HostConfig.ReadonlyRootfs}} security={{json .HostConfig.SecurityOpt}} health={{.State.Health.Status}} ports={{json .NetworkSettings.Ports}}"
```

Expected:

- numeric non-root user;
- `readOnly=true`;
- `no-new-privileges`;
- `health=healthy`;
- no host port in base deployment;
- loopback-only port in local override.

Base Compose inspection:

```powershell
podman compose --env-file .env -f compose.yaml config
```

The rendered base configuration must not contain a `ports` publication.

## Graceful Shutdown

```powershell
podman stop --time 20 sentra-payment_payment-service_1
podman logs --tail 100 sentra-payment_payment-service_1
podman start sentra-payment_payment-service_1
podman healthcheck run sentra-payment_payment-service_1
```

Expected logs show graceful shutdown completion before timeout and healthy
restart.

## Monitoring

Verify metrics:

```powershell
$metrics = Invoke-WebRequest `
  -UseBasicParsing `
  http://localhost:8083/actuator/prometheus

$metrics.Content | Select-String "sentra_payment_"
```

Metric labels must not contain client IDs, key IDs, payment IDs, refund IDs,
merchant references, request IDs, IPs, nonces, idempotency keys, signatures, raw
paths, or amounts.

Suggested alerts:

- readiness unavailable;
- elevated `5xx`;
- p95 latency above approved threshold;
- repository or idempotency failures;
- signature-context denial spike;
- idempotency conflict spike;
- Prometheus scrape failure.

## Failure Behavior

| Failure | Required behavior |
| --- | --- |
| Missing trusted context | `401 PAY_TRUSTED_CONTEXT_REQUIRED` |
| Wrong actor/scope/route | stable `403` |
| Missing signature evidence on mutation | `403 PAY_SIGNATURE_CONTEXT_REQUIRED` |
| Foreign-client payment | indistinguishable `404` |
| Invalid payment/refund body | stable `400` |
| Missing mutation idempotency key | `400 PAY_IDEMPOTENCY_KEY_REQUIRED` |
| Idempotency payload conflict | `409`, no new mutation |
| Refund over remaining amount | `409 PAY_REFUND_NOT_ALLOWED` |
| Oversized body | `413` before domain work |
| Repository unavailable | `503`, readiness down where unsafe |
| Signed route nonce storage unavailable at gateway | gateway denial, no downstream call |
| POST payment downstream timeout | no automatic retry by default |

The service must never silently weaken signature-evidence, trusted-context, or
idempotency enforcement.

## Incident Checks

### Suspected Credential Leakage

1. Stop log export if active.
2. Search logs for authorization, key, signature, nonce, and idempotency fields.
3. Correlate by request ID without exposing raw values.
4. Rotate affected gateway API/signing keys through gateway procedures.
5. Record evidence and remediation before resuming normal traffic.

### Duplicate Payment Or Refund

1. Identify idempotency key presence and route.
2. Check whether gateway retried a mutation.
3. Verify service atomic idempotency record and mutation commit.
4. Check repository restart/reset boundaries in memory mode.
5. Do not delete records before secure incident capture.

### Replay Or Signature Denial Spike

1. Confirm denials happen at the gateway before downstream calls.
2. Check partner clock skew and signing canonicalization.
3. Verify Redis nonce availability.
4. Review recent key rotation and overlap windows.
5. Do not disable replay or signature policy as a quick recovery.

## Runtime Data

Memory mode is non-durable:

- restart restores deterministic seed data;
- created payments, refunds, and idempotency records are lost;
- horizontal replicas do not share writes;
- backup and restore are not applicable.

Production durability requires a separate approved repository design.

## Release Checklist

- [ ] `clean verify` passes.
- [ ] Javadoc and JaCoCo reports are generated.
- [ ] OpenAPI matches `API_CONTRACT.md`.
- [ ] Postman/Newman assertions pass.
- [ ] Gateway API-key/signature/replay allow and deny scenarios pass.
- [ ] POST timeout no-retry behavior is verified.
- [ ] Foreign client reads return `404`.
- [ ] Concurrent idempotency test creates one mutation.
- [ ] Base deployment publishes no host port.
- [ ] Container hardening inspection passes.
- [ ] Graceful stop and healthy restart pass.
- [ ] Logs and metrics contain no forbidden values.
- [ ] Security scans pass.
- [ ] Documentation and traceability match delivered behavior.
