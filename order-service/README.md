# Sentra Order Service

Internal order demonstration service for Sentra Gateway. It proves subject-scoped
and tenant-scoped reads, protected order creation, administrator visibility, and
an explicit idempotency contract for a non-idempotent operation.

**Documentation version:** 1.0.0  
**Document date:** June 15, 2026  
**Implementation status:** Implemented with automated host verification

## Service Boundary

- Logical name: `order-service`
- Internal DNS name: `order-service`
- Suggested internal port: `8082`
- External exposure: none; clients use `gateway-service`
- Authentication: bearer JWT is validated by the gateway
- Service authorization: trusted actor, subject, tenant, route, scope, and role checks
- Repository baseline: deterministic in-memory storage for the demonstration
- Production persistence: intentionally undecided and outside the current scope

The service never validates an external JWT and never accepts a client-selected
owner. It derives ownership exclusively from gateway-created `X-Sentra-*`
headers received over an approved internal network path.

## API Summary

| Method | External path | Internal path | Required permission |
| --- | --- | --- | --- |
| `GET` | `/api/v1/orders` | `/internal/v1/orders` | scope `orders:read` |
| `GET` | `/api/v1/orders/{id}` | `/internal/v1/orders/{id}` | scope `orders:read` |
| `POST` | `/api/v1/orders` | `/internal/v1/orders` | scope `orders:write` |
| `GET` | `/api/v1/admin/orders` | `/internal/v1/admin/orders` | role `ORDER_ADMIN` |

User routes return only orders owned by the trusted subject in the trusted
tenant context. Looking up another user's or another tenant's order returns the
same `404` as an unknown order.

## Documentation

- [Complete service design](docs/ORDER_SERVICE_DOCUMENTATION.md)
- [API contract](docs/API_CONTRACT.md)
- [Configuration catalog](docs/CONFIGURATION.md)
- [Operations and verification](docs/OPERATIONS.md)
- [Requirements traceability](docs/REQUIREMENTS_TRACEABILITY.md)
- [Implementation checklist](docs/IMPLEMENTATION_CHECKLIST.md)

## Source Resolution

The project documents contain one older generic route entry,
`POST /api/orders/**`. The approved order-service contract in this directory
uses the more specific and versioned routes from the SRS and microservices
documentation: `/api/v1/orders` and `/api/v1/admin/orders`.

## Completion Rule

Documentation is not implementation evidence. The service becomes complete only
after the implementation checklist is satisfied, automated tests pass, the
container is verified, and the gateway-to-service contract is exercised.

## Implemented Baseline

The directory now includes:

- Java 25 and Spring Boot 4 implementation of all four internal APIs;
- deterministic in-memory ownership-scoped storage;
- atomic bounded idempotency with replay, conflict, expiry, and concurrency tests;
- stable errors, OpenAPI, Swagger UI, health, Prometheus, and Javadoc;
- Maven wrapper, Podman `Containerfile`, base Compose, and Postman override;
- Postman environment and Newman collection.

Run the host verification gate:

```powershell
.\mvnw.cmd -B -ntp clean verify
.\mvnw.cmd -B -ntp javadoc:javadoc
```

Run locally for Postman:

```powershell
Copy-Item .env.example .env
podman compose --env-file .env -f compose.yaml -f compose.postman.yaml up --build -d
npx --yes newman run postman/Sentra_Order_Service.postman_collection.json `
  -e postman/Sentra_Order_Service_Local.postman_environment.json --reporters cli
```
