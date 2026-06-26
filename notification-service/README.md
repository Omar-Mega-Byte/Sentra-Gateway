# Sentra Notification Service

Internal notification demonstration service for Sentra Gateway. It proves
timeout, retry, circuit-breaker, fallback, and controlled degraded behavior for
downstream routes.

**Documentation version:** 1.0.0  
**Document date:** June 16, 2026  
**Implementation status:** Implemented and locally verified for the
notification-service boundary

## Service Boundary

- Logical name: `notification-service`
- Internal DNS name: `notification-service`
- Suggested internal port: `8084`
- External exposure: none; clients use `gateway-service`
- Authentication: bearer JWT is validated by the gateway
- Service authorization: trusted actor, subject, route, scope, and admin role checks
- Repository baseline: deterministic in-memory notification and preference data
- Primary purpose: resilience demonstration, not a real delivery provider

## API Summary

| Method | External path | Internal path | Required permission | Resilience policy |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/notifications` | `/internal/v1/notifications` | `notifications:read` | short timeout, one bounded retry |
| `POST` | `/api/v1/notifications/preferences` | `/internal/v1/preferences` | `notifications:write` | no automatic retry |
| `POST` | `/api/v1/admin/test-notification` | `/internal/v1/test` | role `NOTIFICATION_ADMIN` | circuit breaker, no retry |

Development-only delay/failure controls are allowed only in `local` and `test`.
Production-like profiles must fail startup if those controls are enabled.

## Documentation

- [Complete service design](docs/NOTIFICATION_SERVICE_DOCUMENTATION.md)
- [API contract](docs/API_CONTRACT.md)
- [Configuration catalog](docs/CONFIGURATION.md)
- [Operations and verification](docs/OPERATIONS.md)
- [Requirements traceability](docs/REQUIREMENTS_TRACEABILITY.md)
- [Implementation checklist](docs/IMPLEMENTATION_CHECKLIST.md)

## Source Resolution

The broad technical documentation contains an older generic route entry,
`POST /api/notifications/**`. The approved notification-service contract in this
directory uses the versioned routes from the microservices documentation:
`/api/v1/notifications`, `/api/v1/notifications/preferences`, and
`/api/v1/admin/test-notification`.

## Implementation Assets

This directory now contains the Spring Boot service implementation, Maven
wrapper, OpenAPI annotations, tests, Podman container assets, PowerShell scripts,
and direct-local Postman/Newman assets. Gateway end-to-end resilience scenarios
remain exercised through the gateway deployment because the gateway owns JWT,
retry, timeout, circuit, and fallback behavior.
