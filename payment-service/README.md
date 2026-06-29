# Sentra Payment Service

Internal partner payment demonstration service for Sentra Gateway. It proves
API-key partner routing, gateway-validated request signing, replay-protection
evidence, high-risk mutation handling, strict limits, and non-retry behavior.

**Documentation version:** 1.0.0  
**Document date:** June 16, 2026  
**Implementation status:** Implemented with Maven tests, OpenAPI, Podman assets,
and Postman/Newman assets. Gateway API-key, HMAC, nonce, and replay scenarios
still require a running `gateway-service` stack for external end-to-end evidence.

## Service Boundary

- Logical name: `payment-service`
- Internal DNS name: `payment-service`
- Suggested internal port: `8083`
- External exposure: none; partners call `gateway-service`
- External authentication: API key at gateway
- External signing: HMAC request signature at gateway
- Service authorization: trusted actor, client ID, key ID, route, scope, and signature-validation context
- Repository baseline: deterministic in-memory payment/refund records for the demonstration

The service never receives plaintext API keys, external signatures, HMAC secrets,
or raw partner credentials. It accepts only gateway-created `X-Sentra-*` trusted
headers over an approved internal network path.

## API Summary

| Method | External path | Internal path | Required permission | Signing |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/partner/payments/{id}` | `/internal/v1/payments/{id}` | `payments:read` | policy-controlled |
| `POST` | `/api/v1/partner/payments` | `/internal/v1/payments` | `payments:write` | required |
| `POST` | `/api/v1/partner/refunds` | `/internal/v1/refunds` | `refunds:write` | required |

Create and refund operations are high-risk mutations. They are never
automatically retried unless the gateway and service preserve the documented
idempotency contract.

## Documentation

- [Complete service design](docs/PAYMENT_SERVICE_DOCUMENTATION.md)
- [API contract](docs/API_CONTRACT.md)
- [Configuration catalog](docs/CONFIGURATION.md)
- [Operations and verification](docs/OPERATIONS.md)
- [Requirements traceability](docs/REQUIREMENTS_TRACEABILITY.md)
- [Implementation checklist](docs/IMPLEMENTATION_CHECKLIST.md)

## Run And Test

Service scripts:

```powershell
.\scripts\test.ps1
.\scripts\podman-up.ps1 -Postman -ForceRecreate
.\scripts\postman-newman.ps1
.\scripts\podman-down.ps1 -Postman
```

Equivalent direct commands:

```powershell
.\mvnw.cmd -B -ntp clean verify
.\mvnw.cmd -B -ntp javadoc:javadoc
```

Local JVM:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:PAYMENT_SEED_ENABLED = "true"
$env:OPENAPI_ENABLED = "true"
$env:SWAGGER_UI_ENABLED = "true"
.\mvnw.cmd spring-boot:run
```

Podman for local Postman testing:

```powershell
Copy-Item .env.example .env
podman network create --internal sentra-gateway_services
podman compose --env-file .env -f compose.yaml -f compose.postman.yaml up --build -d
npx --yes newman run postman/Sentra_Payment_Service.postman_collection.json -e postman/Sentra_Payment_Service_Local.postman_environment.json --reporters cli
```

## Source Resolution

The broad technical documentation contains one older generic route entry,
`POST /api/payments/**`. The approved payment-service contract in this directory
uses the more specific and versioned partner routes from the SRS and
microservices documentation: `/api/v1/partner/payments` and
`/api/v1/partner/refunds`.

## Completion Rule

Documentation is not implementation evidence. The service is release-ready only
after Maven tests, Javadoc, OpenAPI, Podman, Postman/Newman, gateway
end-to-end signature scenarios, and operational checks pass in the target
environment.
