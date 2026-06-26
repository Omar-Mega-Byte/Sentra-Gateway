# User Service API Contract

**Version:** 1.0.0  
**Target internal base URL:** `http://user-service:8081`  
**External base URL:** gateway deployment URL  
**Content type:** `application/json; charset=UTF-8`  
**Implementation status:** Implemented and covered by HTTP/OpenAPI contract tests

## Routing And Authentication

External clients call the gateway. They do not call the internal paths directly.

| External request | Internal request | Authentication | Required scope |
| --- | --- | --- | --- |
| `GET /api/v1/public/users/{id}` | `GET /internal/v1/users/{id}/public` | None | None |
| `GET /api/v1/users/me` | `GET /internal/v1/users/me` | Bearer JWT at gateway | `profile:read` |
| `PATCH /api/v1/users/me` | `PATCH /internal/v1/users/me` | Bearer JWT at gateway | `profile:write` |

The gateway strips external credentials and creates trusted context headers. The
service resolves `/me` from `X-Sentra-Subject`; it never accepts a client-selected
user ID for self-service operations.

## Common Rules

- JSON is UTF-8.
- Timestamps are RFC 3339 UTC instants.
- Profile IDs are canonical UUID strings.
- Unknown JSON properties are rejected.
- JSON mutation requests require `Content-Type: application/json`.
- Successful JSON responses use `Content-Type: application/json`.
- The default maximum request body is 16 KiB.
- `PATCH` requires optimistic `version`.
- Responses include `X-Request-Id`.
- Empty success bodies are not used by the three profile operations.
- Credentials, internal flags, subject identifiers, and security metadata are
  never included in a public profile.
- The service OpenAPI describes internal paths. The gateway route catalog
  describes external paths.

## Public Profile

### `GET /internal/v1/users/{id}/public`

Returns public fields for an active profile.

Path parameter:

| Name | Type | Rule |
| --- | --- | --- |
| `id` | UUID | Required canonical profile identifier |

Successful response: `200 OK`

```json
{
  "id": "7aa99db8-a943-4b63-b4b7-79f769ef9f87",
  "displayName": "Omar Hassan",
  "bio": "Backend engineer",
  "avatarUrl": "https://cdn.example.test/avatars/7aa99db8.png"
}
```

The response uses the configured public `Cache-Control` policy.

Public response fields are fixed:

| Field | Nullable |
| --- | --- |
| `id` | No |
| `displayName` | No |
| `bio` | Yes |
| `avatarUrl` | Yes |

The response must not contain `subject`, `email`, `locale`, `timezone`, `status`,
`version`, timestamps, credentials, roles, scopes, tenant data, internal flags, or
security metadata.

Failure responses:

| Condition | Status | Code |
| --- | ---: | --- |
| Missing required gateway provenance | 401 | `USR_TRUSTED_CONTEXT_REQUIRED` |
| Invalid UUID | 400 | `USR_REQUEST_INVALID` |
| Missing, disabled, or deleted profile | 404 | `USR_PROFILE_NOT_FOUND` |

Disabled and deleted profiles return the same public `404` as an unknown profile.

## Current Profile

### `GET /internal/v1/users/me`

Returns the safe private representation associated with
`X-Sentra-Subject`.

Required trusted context:

```text
X-Sentra-Request-Id: <approved request ID>
X-Sentra-Subject: <validated JWT subject>
X-Sentra-Actor-Type: USER
X-Sentra-Scopes: ... profile:read ...
X-Sentra-Route-Id: <approved route ID>
```

Successful response: `200 OK`

```json
{
  "id": "7aa99db8-a943-4b63-b4b7-79f769ef9f87",
  "displayName": "Omar Hassan",
  "bio": "Backend engineer",
  "avatarUrl": "https://cdn.example.test/avatars/7aa99db8.png",
  "email": "omar@example.test",
  "locale": "en-EG",
  "timezone": "Africa/Cairo",
  "version": 3,
  "createdAt": "2026-06-01T10:00:00Z",
  "updatedAt": "2026-06-15T00:00:00Z"
}
```

The response omits `subject`, `status`, credentials, authentication metadata,
roles, scopes, and internal flags.
It returns `Cache-Control: no-store`.

Failure responses:

| Condition | Status | Code |
| --- | ---: | --- |
| Missing/malformed gateway context | 401 | `USR_TRUSTED_CONTEXT_REQUIRED` |
| Actor type is not `USER` | 403 | `USR_ACTOR_NOT_ALLOWED` |
| Required scope absent | 403 | `USR_SCOPE_REQUIRED` |
| Subject has no active profile | 404 | `USR_PROFILE_NOT_FOUND` |

## Update Current Profile

### `PATCH /internal/v1/users/me`

Updates mutable fields for the profile associated with `X-Sentra-Subject`.

Required scope: `profile:write`.

Request:

```json
{
  "displayName": "Omar H.",
  "bio": "Building secure Java services",
  "avatarUrl": null,
  "email": "omar.h@example.test",
  "locale": "en-EG",
  "timezone": "Africa/Cairo",
  "version": 3
}
```

Every mutable field except `version` is optional. At least one mutable field must
be present.

| Field | Type | Validation |
| --- | --- | --- |
| `displayName` | string | 1-100 characters after trimming; not null |
| `bio` | string/null | At most 500 characters |
| `avatarUrl` | string/null | Absolute HTTPS URI, at most 2048 characters |
| `email` | string | Valid address, normalized for comparison, at most 254 characters |
| `locale` | string | Valid BCP 47 language tag, at most 35 characters |
| `timezone` | string | Valid IANA time-zone ID, at most 64 characters |
| `version` | integer | Required, positive, current stored version |

Fields `id`, `subject`, `status`, `createdAt`, and `updatedAt` are immutable and
rejected if supplied.

Successful response: `200 OK`

The body is the current-profile representation. A state-changing update increments
`version` and sets `updatedAt`. A valid no-op returns the unchanged representation.
The response returns `Cache-Control: no-store`.

Failure responses:

| Condition | Status | Code |
| --- | ---: | --- |
| Missing/malformed gateway context | 401 | `USR_TRUSTED_CONTEXT_REQUIRED` |
| Actor type is not `USER` | 403 | `USR_ACTOR_NOT_ALLOWED` |
| Required scope absent | 403 | `USR_SCOPE_REQUIRED` |
| Unsupported content type | 415 | `USR_MEDIA_TYPE_UNSUPPORTED` |
| Body exceeds configured limit | 413 | `USR_BODY_TOO_LARGE` |
| Malformed JSON, unknown/immutable field, or invalid value | 400 | `USR_REQUEST_INVALID` |
| No mutable field supplied | 400 | `USR_REQUEST_INVALID` |
| Subject has no active profile | 404 | `USR_PROFILE_NOT_FOUND` |
| Version does not match | 409 | `USR_VERSION_CONFLICT` |
| Unique email conflict, when enabled | 409 | `USR_EMAIL_CONFLICT` |

## Trusted Header Encoding

| Header | Encoding |
| --- | --- |
| `X-Sentra-Request-Id` | One visible ASCII value, maximum 128 characters |
| `X-Sentra-Subject` | One nonblank value, maximum 255 characters |
| `X-Sentra-Actor-Type` | Uppercase token; `USER` required for `/me` |
| `X-Sentra-Tenant-Id` | Optional opaque value, maximum 128 characters |
| `X-Sentra-Roles` | Gateway-defined encoded list |
| `X-Sentra-Scopes` | Gateway-defined encoded list |
| `X-Sentra-Route-Id` | Lowercase route ID, maximum 128 characters |
| `X-Sentra-Source-Ip` | Canonical IPv4 or IPv6 text |

Duplicate security-critical trusted headers are rejected. The implementation must
reuse the gateway's documented list codec or a contract-tested equivalent rather
than split ambiguous values ad hoc.

## Error Contract

```json
{
  "timestamp": "2026-06-15T00:00:00Z",
  "requestId": "8e3a95b8-6674-423e-83e6-0df84c2d66d0",
  "status": 409,
  "code": "USR_VERSION_CONFLICT",
  "message": "The profile was changed by another request.",
  "path": "/internal/v1/users/me",
  "routeId": "user-profile-update",
  "details": [
    {
      "field": "version",
      "code": "conflict",
      "message": "Refresh the profile and retry the update."
    }
  ]
}
```

Error fields:

| Field | Type | Rule |
| --- | --- | --- |
| `timestamp` | instant | Server time |
| `requestId` | string | Same approved value as `X-Request-Id` |
| `status` | integer | HTTP status |
| `code` | string | Stable machine-readable code |
| `message` | string | Sanitized client-safe message |
| `path` | string | Internal request path without query string |
| `routeId` | string/null | Trusted gateway route ID when available |
| `details` | array | Bounded field-level details |

Primary service codes:

| Code | Status |
| --- | ---: |
| `USR_REQUEST_INVALID` | 400 |
| `USR_TRUSTED_CONTEXT_REQUIRED` | 401 |
| `USR_ACTOR_NOT_ALLOWED` | 403 |
| `USR_SCOPE_REQUIRED` | 403 |
| `USR_PROFILE_NOT_FOUND` | 404 |
| `USR_VERSION_CONFLICT` | 409 |
| `USR_EMAIL_CONFLICT` | 409 |
| `USR_BODY_TOO_LARGE` | 413 |
| `USR_MEDIA_TYPE_UNSUPPORTED` | 415 |
| `USR_DEPENDENCY_UNAVAILABLE` | 503 |
| `USR_INTERNAL_ERROR` | 500 |

Field-level details never echo passwords, tokens, complete submitted values, or
other sensitive data.

## Management Contract

| Method | Path | Exposure |
| --- | --- | --- |
| `GET` | `/actuator/health/liveness` | Internal health |
| `GET` | `/actuator/health/readiness` | Internal health |
| `GET` | `/actuator/prometheus` | Operations network |
| `GET` | `/actuator/metrics` | Operations network/authenticated |
| `GET` | `/v3/api-docs` | Local/test or protected operations access |
| `GET` | `/swagger-ui.html` | Local/test or protected operations access |

Management endpoints are not routed as public user APIs.

## OpenAPI

The implementation shall publish OpenAPI containing:

- all three internal profile paths;
- trusted-header requirements;
- request and response schemas;
- every documented status and error schema;
- field lengths, formats, nullability, and examples; and
- a clear statement that external authentication is enforced by the gateway.

OpenAPI is generated by springdoc and verified by
`UserServiceApplicationTest.openApiContainsEveryPathHeaderSchemaAndResponse`.
