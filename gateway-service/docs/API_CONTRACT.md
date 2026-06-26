# Gateway API Contract

**Version:** 1.0.0  
**Base URL:** `http://localhost:8080` in the local profile  
**Administrative base path:** `/api/v1/admin`  
**Content type:** `application/json`  
**Implementation status:** Implemented and covered by OpenAPI

## Authentication

The local profile accepts HTTP Basic authentication. Other profiles use bearer
JWT authentication.

| Role | Access |
| --- | --- |
| `GATEWAY_ROUTE_ADMIN` | Route reads and mutations |
| `GATEWAY_SECURITY_ADMIN` | API clients, API keys, and policies |
| `GATEWAY_AUDITOR` | Audit events and admin actions |
| `GATEWAY_OPERATOR` | Read-only routes, metrics, and protected docs |
| `GATEWAY_SUPER_ADMIN` | All administrative APIs |

Missing credentials return `401`. Authenticated callers without the required role
receive `403`.

## Common Rules

- Timestamps are RFC 3339/ISO-8601 instants.
- Unknown JSON properties are rejected.
- Mutation resources use optimistic `version` values.
- Route and policy creates return version `1`.
- Successful deletes and key revocation return `204`.
- Key issue and rotation require a nonblank `Idempotency-Key` header.
- List endpoints currently return JSON arrays. Audit endpoints support bounded
  offset pagination through `page` and `pageSize`.
- API-key plaintext appears only in an issue or rotation response.
- Persistence verifiers, credentials, stack traces, SQL, and internal exception
  details are never returned.

## Routes

| Method | Path | Result |
| --- | --- | --- |
| `GET` | `/routes` | All persisted routes |
| `GET` | `/routes/{id}` | One route |
| `POST` | `/routes/validate` | Validation result without persistence |
| `POST` | `/routes` | Create and activate a route definition |
| `PUT` | `/routes/{id}` | Replace using the body `version` |
| `POST` | `/routes/{id}/enable` | Enable and increment version |
| `POST` | `/routes/{id}/disable` | Disable and increment version |
| `DELETE` | `/routes/{id}` | Delete |
| `GET` | `/routes/generation` | This instance's refresh generation |

Example:

```json
{
  "id": "orders-list",
  "category": "USER",
  "pathPatterns": ["/api/v1/orders"],
  "methods": ["GET"],
  "targetUri": "http://order-service:8082",
  "stripPrefix": 0,
  "order": 100,
  "enabled": true,
  "authentication": ["JWT"],
  "requiredRoles": [],
  "requiredScopes": ["orders:read"],
  "signingRequired": false,
  "rateLimitPolicyId": null,
  "ipPolicyId": null,
  "riskPolicyId": null,
  "connectTimeoutMs": 1000,
  "responseTimeoutMs": 3000,
  "retryPolicy": {
    "enabled": true,
    "maxAttempts": 2,
    "eligibleMethods": ["GET"]
  },
  "circuitBreaker": {
    "enabled": true,
    "name": "order-service"
  },
  "auditMode": "DENIALS_AND_MUTATIONS",
  "version": 0
}
```

Route IDs are lowercase kebab-case. Paths cannot overlap reserved admin, Actuator,
or Swagger prefixes. Targets must use an allowed scheme and an allowlisted host.
Credentials and fragments in target URIs are rejected. Retries are limited to
`GET`, `HEAD`, and `OPTIONS`, with at most two total attempts.

## API Clients And Keys

| Method | Path | Result |
| --- | --- | --- |
| `GET` | `/api-clients` | Client metadata |
| `GET` | `/api-clients/{id}` | One client |
| `POST` | `/api-clients` | Create active client |
| `PUT` | `/api-clients/{id}` | Replace metadata/status |
| `POST` | `/api-clients/{id}/disable` | Disable client |
| `GET` | `/api-clients/{id}/keys` | Secret-free key metadata |
| `POST` | `/api-clients/{id}/keys` | Issue key |
| `POST` | `/api-keys/{id}/rotate` | Issue successor and revoke predecessor |
| `POST` | `/api-keys/{id}/revoke` | Revoke active key |

Issue request:

```json
{
  "scopes": ["payments:write"],
  "allowedRoutes": ["payment-create"],
  "expiresAt": "2026-06-16T00:00:00Z"
}
```

One-time response:

```json
{
  "keyId": "6bb09f4f-44db-4be2-b21a-46a5e3245aa5",
  "apiKey": "sgw_local_ab12cd34ef56_<secret>",
  "prefix": "ab12cd34ef56",
  "createdAt": "2026-06-15T00:00:00Z",
  "expiresAt": "2026-06-16T00:00:00Z",
  "warning": "This API key will not be shown again."
}
```

Metadata responses omit `apiKey` and `verifier`.

## Rate-Limit Policies

| Method | Path |
| --- | --- |
| `GET`, `POST` | `/rate-limits` |
| `GET`, `PUT`, `DELETE` | `/rate-limits/{id}` |

```json
{
  "id": "partner-payment-write",
  "subjectType": "CLIENT",
  "routeId": "payment-create",
  "method": "POST",
  "capacity": 20,
  "refillTokens": 20,
  "refillPeriodSeconds": 60,
  "priority": 100,
  "redisOutageMode": "DENY",
  "responseHeadersEnabled": true,
  "enabled": true,
  "version": 0
}
```

The runtime uses an atomic Redis server-time token bucket. `DENY` fails closed;
`ALLOW` explicitly allows during Redis failure. `LOCAL_FALLBACK` is accepted by
the schema but currently follows fail-closed behavior.

## IP Rules

| Method | Path |
| --- | --- |
| `GET`, `POST` | `/ip-rules` |
| `GET`, `PUT`, `DELETE` | `/ip-rules/{id}` |

Rules accept `ALLOW`, `BLOCK`, or `TEMP_BLOCK`, IPv4/IPv6 CIDR notation, optional
route scope, validity interval, priority, enabled state, and version. A route
references one selected rule through `ipPolicyId`.

## Risk Rules

| Method | Path |
| --- | --- |
| `GET`, `POST` | `/risk-rules` |
| `GET`, `PUT`, `DELETE` | `/risk-rules/{id}` |

Implemented signals:

- `HEADER_COUNT`
- `QUERY_PARAMETER_COUNT`
- `PATH_SEGMENTS`

Actions are `OBSERVE`, `THROTTLE`, `TEMP_BLOCK`, or `DENY`. `DENY` and
`TEMP_BLOCK` reject immediately. `THROTTLE` produces a runtime throttle decision;
the current route-selected rate policy remains the enforcing bucket.

## Audit

| Method | Path | Parameters |
| --- | --- | --- |
| `GET` | `/audit-events` | Required `from`, `to`; optional `requestId`, `routeId`, `page`, `pageSize` |
| `GET` | `/audit-events/{id}` | Audit UUID |
| `GET` | `/admin-actions` | `page`, `pageSize` |

`pageSize` is 1 through 100. Audit search ranges cannot exceed
`AUDIT_SEARCH_MAX_RANGE`, which defaults to 31 days.

## Signing Protocol

Partner routes with `signingRequired=true` require:

```text
X-API-Key
X-Sentra-Key-Id
X-Sentra-Timestamp
X-Sentra-Nonce
X-Sentra-Signature
```

Canonical UTF-8 input:

```text
HTTP_METHOD
NORMALIZED_RAW_PATH
SORTED_RFC3986_QUERY
LOWERCASE_SHA256_BODY_HEX
EPOCH_SECONDS
NONCE
KEY_ID
```

`X-Sentra-Signature` is base64url HMAC-SHA-256 without padding, keyed by the
presented API key. Signed content encoding is rejected. Timestamp skew defaults to
five minutes. Redis atomically claims `(key ID, nonce)` for ten minutes.

## Error Contract

```json
{
  "timestamp": "2026-06-15T00:00:00Z",
  "requestId": "8e3a95b8-6674-423e-83e6-0df84c2d66d0",
  "status": 400,
  "code": "GW_REQUEST_INVALID",
  "message": "The request is invalid.",
  "path": "/api/v1/admin/routes",
  "routeId": null,
  "details": []
}
```

Primary codes:

| Code | Status |
| --- | ---: |
| `GW_REQUEST_INVALID` | 400 |
| `GW_AUTH_REQUIRED`, `GW_TOKEN_INVALID`, `GW_API_KEY_INVALID`, `GW_SIGNATURE_INVALID` | 401 |
| `GW_PERMISSION_DENIED`, `GW_REPLAY_DETECTED`, `GW_IP_DENIED`, `GW_RISK_DENIED` | 403 |
| `GW_ROUTE_NOT_FOUND`, `GW_RESOURCE_NOT_FOUND` | 404 |
| `GW_POLICY_CONFLICT` | 409 |
| `GW_BODY_TOO_LARGE` | 413 |
| `GW_RATE_LIMITED` | 429 |
| `GW_INTERNAL_ERROR` | 500 |
| `GW_DEPENDENCY_UNAVAILABLE`, `GW_DOWNSTREAM_UNAVAILABLE` | 503 |
| `GW_DOWNSTREAM_TIMEOUT` | 504 |

## OpenAPI

- JSON: `/v3/api-docs`
- UI: `/swagger-ui.html`

The local profile exposes both without authentication. JWT profiles require
`GATEWAY_OPERATOR` or `GATEWAY_SUPER_ADMIN`.
