# Notification Service API Contract

**Version:** 1.0.0  
**Target internal base URL:** `http://notification-service:8084`  
**External base URL:** gateway deployment URL  
**Content type:** `application/json; charset=UTF-8`  
**Implementation status:** Implemented in this service with automated
notification-service tests and generated OpenAPI

## Routing And Authentication

External callers use the gateway. Internal paths are not public APIs.

| External request | Internal request | Gateway auth | Service permission | Route ID |
| --- | --- | --- | --- | --- |
| `GET /api/v1/notifications` | `GET /internal/v1/notifications` | Bearer JWT | `notifications:read` | `notifications-list` |
| `POST /api/v1/notifications/preferences` | `POST /internal/v1/preferences` | Bearer JWT | `notifications:write` | `notification-preferences-update` |
| `POST /api/v1/admin/test-notification` | `POST /internal/v1/test` | Admin bearer JWT | role `NOTIFICATION_ADMIN` | `admin-test-notification` |

The gateway strips credentials and inbound reserved headers, then forwards
trusted context.

## Common Rules

- JSON uses UTF-8.
- Timestamps are RFC 3339 UTC instants.
- IDs are canonical UUID strings.
- Unknown JSON fields are rejected.
- `POST` requests require `Content-Type: application/json`.
- Default maximum request body is 16 KiB.
- Responses include `X-Request-Id`.
- User responses use `Cache-Control: no-store`.
- Development fault controls are unavailable outside `local` and `test`.
- The gateway owns fallback responses and retry/circuit behavior.

## Trusted Context

### User Routes

```text
X-Sentra-Request-Id: <approved request ID>
X-Sentra-Subject: <validated JWT subject>
X-Sentra-Actor-Type: USER
X-Sentra-Tenant-Id: <validated tenant, when applicable>
X-Sentra-Scopes: notifications:read|notifications:write
X-Sentra-Route-Id: <exact route ID>
```

### Admin Test Route

```text
X-Sentra-Request-Id: <approved request ID>
X-Sentra-Subject: <validated admin subject>
X-Sentra-Actor-Type: USER
X-Sentra-Roles: NOTIFICATION_ADMIN
X-Sentra-Route-Id: admin-test-notification
```

Duplicate security-critical headers are rejected.

## Notification Response

```json
{
  "id": "70000000-0000-4000-8000-000000000001",
  "channel": "EMAIL",
  "title": "Welcome to Sentra",
  "message": "Your notification route is working.",
  "status": "SENT",
  "createdAt": "2026-06-01T10:00:00Z"
}
```

User responses omit `subject`, `tenantId`, provider identifiers, credentials,
retry counters, fault internals, roles, scopes, and gateway security metadata.

## Preference Response

```json
{
  "emailEnabled": true,
  "smsEnabled": false,
  "pushEnabled": true,
  "webhookEnabled": false,
  "version": 3,
  "updatedAt": "2026-06-16T10:00:00Z"
}
```

## List Notifications

### `GET /internal/v1/notifications`

Returns notifications belonging to the trusted `(tenantId, subject)` context.

Required scope: `notifications:read`.

Query parameters:

| Name | Type | Default | Validation |
| --- | --- | --- | --- |
| `page` | integer | `0` | `0-10000` |
| `size` | integer | `20` | `1-100` |
| `channel` | enum | absent | `EMAIL`, `SMS`, `PUSH`, `WEBHOOK` |
| `status` | enum | absent | `QUEUED`, `SENT`, `FAILED`, `SUPPRESSED` |

Unknown query parameters are rejected.

Successful response: `200 OK`.

```json
{
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1,
  "items": []
}
```

Ordering is fixed to `createdAt DESC, id DESC`.

Failure responses:

| Condition | Status | Code |
| --- | ---: | --- |
| Missing/malformed trusted context | 401 | `NTF_TRUSTED_CONTEXT_REQUIRED` |
| Actor is not `USER` | 403 | `NTF_ACTOR_NOT_ALLOWED` |
| Wrong route ID | 403 | `NTF_ROUTE_NOT_ALLOWED` |
| Required scope absent | 403 | `NTF_SCOPE_REQUIRED` |
| Invalid query parameter | 400 | `NTF_REQUEST_INVALID` |
| Repository unavailable | 503 | `NTF_DEPENDENCY_UNAVAILABLE` |

## Update Preferences

### `POST /internal/v1/preferences`

Updates notification preferences for the trusted subject and tenant.

Required scope: `notifications:write`.
Gateway retry policy: no automatic retry.

Request:

```json
{
  "emailEnabled": true,
  "smsEnabled": false,
  "pushEnabled": true,
  "webhookEnabled": false,
  "version": 2
}
```

All four boolean fields and `version` are required.

Successful response: `200 OK`, using preference response.

Failure responses:

| Condition | Status | Code |
| --- | ---: | --- |
| Missing/malformed trusted context | 401 | `NTF_TRUSTED_CONTEXT_REQUIRED` |
| Actor is not `USER` | 403 | `NTF_ACTOR_NOT_ALLOWED` |
| Wrong route ID | 403 | `NTF_ROUTE_NOT_ALLOWED` |
| Required scope absent | 403 | `NTF_SCOPE_REQUIRED` |
| Unsupported media type | 415 | `NTF_MEDIA_TYPE_UNSUPPORTED` |
| Body exceeds configured limit | 413 | `NTF_BODY_TOO_LARGE` |
| Malformed JSON, unknown field, missing field, invalid value | 400 | `NTF_REQUEST_INVALID` |
| Version does not match current preferences | 409 | `NTF_VERSION_CONFLICT` |
| Repository unavailable | 503 | `NTF_DEPENDENCY_UNAVAILABLE` |

## Admin Test Notification

### `POST /internal/v1/test`

Triggers a deterministic admin-only test response used for timeout, circuit, and
fallback verification. It does not send a real notification.

Required role: `NOTIFICATION_ADMIN`.
Gateway retry policy: no automatic retry.

Request:

```json
{
  "scenario": "SUCCESS",
  "channel": "EMAIL",
  "recipientReference": "test-recipient",
  "message": "Gateway resilience smoke test"
}
```

| Field | Type | Validation |
| --- | --- | --- |
| `scenario` | enum | `SUCCESS`, `DELAY`, `FAILURE`, `MALFORMED`, `DISCONNECT` |
| `channel` | enum | `EMAIL`, `SMS`, `PUSH`, `WEBHOOK` |
| `recipientReference` | string | 1-128 visible characters |
| `message` | string | 1-1000 characters |

Successful response:

```json
{
  "scenario": "SUCCESS",
  "accepted": true,
  "result": "TEST_ACCEPTED",
  "createdAt": "2026-06-16T10:00:00Z"
}
```

Failure responses:

| Condition | Status | Code |
| --- | ---: | --- |
| Missing/malformed trusted context | 401 | `NTF_TRUSTED_CONTEXT_REQUIRED` |
| Actor is not `USER` | 403 | `NTF_ACTOR_NOT_ALLOWED` |
| Wrong route ID | 403 | `NTF_ROUTE_NOT_ALLOWED` |
| Required admin role absent | 403 | `NTF_ROLE_REQUIRED` |
| Unsupported media type | 415 | `NTF_MEDIA_TYPE_UNSUPPORTED` |
| Body exceeds configured limit | 413 | `NTF_BODY_TOO_LARGE` |
| Malformed JSON, unknown field, invalid scenario | 400 | `NTF_REQUEST_INVALID` |
| Fault controls disabled for current profile | 403 | `NTF_FAULT_CONTROL_DISABLED` |
| Configured failure scenario | 500/502/503/504 | `NTF_TEST_FAILURE` |
| Repository/fault subsystem unavailable | 503 | `NTF_DEPENDENCY_UNAVAILABLE` |

`MALFORMED` and `DISCONNECT` are local/test-only scenarios intended for gateway
resilience tests. They may intentionally violate the normal response contract
only when fault controls are enabled.

## Fault Controls

Fault controls can be supplied by admin test request fields and, in local/test
only, by documented internal headers:

| Header | Purpose |
| --- | --- |
| `X-Sentra-Test-Delay-Millis` | bounded artificial delay |
| `X-Sentra-Test-Status` | bounded failure status |
| `X-Sentra-Test-Malformed` | request malformed response |
| `X-Sentra-Test-Disconnect` | request disconnect simulation |

These headers are reserved for direct local/test service calls and must be
rejected or ignored safely outside those profiles. They are not public client
controls. External clients cannot rely on them because the gateway strips
inbound reserved headers before forwarding trusted context.

## Error Contract

```json
{
  "timestamp": "2026-06-16T12:00:00Z",
  "requestId": "8e3a95b8-6674-423e-83e6-0df84c2d66d0",
  "status": 409,
  "code": "NTF_VERSION_CONFLICT",
  "message": "Notification preferences were changed by another request.",
  "path": "/internal/v1/preferences",
  "routeId": "notification-preferences-update",
  "details": []
}
```

Primary codes:

| Code | Status |
| --- | ---: |
| `NTF_REQUEST_INVALID` | 400 |
| `NTF_TRUSTED_CONTEXT_REQUIRED` | 401 |
| `NTF_ACTOR_NOT_ALLOWED` | 403 |
| `NTF_ROUTE_NOT_ALLOWED` | 403 |
| `NTF_SCOPE_REQUIRED` | 403 |
| `NTF_ROLE_REQUIRED` | 403 |
| `NTF_FAULT_CONTROL_DISABLED` | 403 |
| `NTF_VERSION_CONFLICT` | 409 |
| `NTF_BODY_TOO_LARGE` | 413 |
| `NTF_MEDIA_TYPE_UNSUPPORTED` | 415 |
| `NTF_TEST_FAILURE` | 500 or configured local/test status |
| `NTF_DEPENDENCY_UNAVAILABLE` | 503 |
| `NTF_INTERNAL_ERROR` | 500 |

Errors never expose stack traces, provider internals, hosts, credentials,
message bodies, subjects, tenants, roles, or scopes.

## Management Contract

| Method | Path | Exposure |
| --- | --- | --- |
| `GET` | `/actuator/health/liveness` | Internal health |
| `GET` | `/actuator/health/readiness` | Internal health |
| `GET` | `/actuator/prometheus` | Operations network |
| `GET` | `/actuator/metrics` | Protected operations access |
| `GET` | `/v3/api-docs` | Local/test or protected operations access |
| `GET` | `/swagger-ui.html` | Local/test or protected operations access |

## OpenAPI Requirements

Generated OpenAPI must include:

- all three internal operations;
- trusted-header requirements;
- local/test fault-control documentation;
- pagination, notification, preference, and admin-test schemas;
- every documented status and error schema;
- field constraints, nullability, enums, and examples;
- a statement that gateway owns external JWT, retry, timeout, circuit, and fallback behavior.
