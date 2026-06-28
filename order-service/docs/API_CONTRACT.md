# Order Service API Contract

**Version:** 1.0.0  
**Target internal base URL:** `http://order-service:8082`  
**External base URL:** gateway deployment URL  
**Content type:** `application/json; charset=UTF-8`  
**Implementation status:** Implemented and verified by automated service-local contract tests

## Routing And Authentication

External clients call the gateway. Internal paths are not public APIs.

| External request | Internal request | Gateway authentication | Service permission | Route ID |
| --- | --- | --- | --- | --- |
| `GET /api/v1/orders` | `GET /internal/v1/orders` | Bearer JWT | `orders:read` | `orders-list` |
| `GET /api/v1/orders/{id}` | `GET /internal/v1/orders/{id}` | Bearer JWT | `orders:read` | `orders-get` |
| `POST /api/v1/orders` | `POST /internal/v1/orders` | Bearer JWT | `orders:write` | `orders-create` |
| `GET /api/v1/admin/orders` | `GET /internal/v1/admin/orders` | Admin bearer JWT | role `ORDER_ADMIN` | `admin-orders-list` |

The gateway removes the bearer token and any inbound reserved headers before
creating trusted context. The service does not accept ownership in path, query,
or body fields.

## Common Rules

- JSON uses UTF-8.
- Timestamps are RFC 3339 UTC instants.
- Order IDs are canonical UUID strings.
- Unknown JSON properties are rejected.
- `POST` requires `Content-Type: application/json`.
- The default maximum request body is 32 KiB.
- The default maximum trusted-header value is 255 characters.
- Collection results are bounded and paginated.
- Responses include `X-Request-Id`.
- User responses use `Cache-Control: no-store`.
- The service OpenAPI documents internal paths and trusted headers.
- The gateway route catalog documents external paths and JWT security.
- Automatic retries for `POST` are disabled unless gateway policy explicitly
  recognizes the idempotency contract.

## Trusted Context

### User Routes

```text
X-Sentra-Request-Id: <approved request ID>
X-Sentra-Subject: <validated JWT subject>
X-Sentra-Actor-Type: USER
X-Sentra-Tenant-Id: <validated tenant, when applicable>
X-Sentra-Scopes: ... orders:read|orders:write ...
X-Sentra-Route-Id: <exact route ID>
X-Sentra-Source-Ip: <resolved address, optional to the service>
```

### Administrator Route

```text
X-Sentra-Request-Id: <approved request ID>
X-Sentra-Subject: <validated administrator subject>
X-Sentra-Actor-Type: USER
X-Sentra-Tenant-Id: <administrator tenant, when applicable>
X-Sentra-Roles: ... ORDER_ADMIN ...
X-Sentra-Route-Id: admin-orders-list
```

Duplicate security-critical headers are rejected. The service must use the same
unambiguous role/scope list encoding as the gateway.

## User Order Representation

```json
{
  "id": "10000000-0000-4000-8000-000000000001",
  "items": [
    {
      "sku": "BOOK-JAVA-25",
      "quantity": 1
    }
  ],
  "status": "COMPLETED",
  "createdAt": "2026-06-01T10:00:00Z",
  "updatedAt": "2026-06-02T09:30:00Z"
}
```

Fixed fields:

| Field | Type | Nullable |
| --- | --- | --- |
| `id` | UUID string | No |
| `items` | array of `OrderItem` | No |
| `status` | enum | No |
| `createdAt` | instant | No |
| `updatedAt` | instant | No |

The user representation must not contain `ownerSubject`, `tenantId`,
idempotency metadata, internal repository fields, trusted headers, credentials,
roles, scopes, or gateway security metadata.

## Administrator Order Representation

```json
{
  "id": "10000000-0000-4000-8000-000000000001",
  "ownerSubject": "sentra-user-omar",
  "tenantId": "tenant-demo",
  "items": [
    {
      "sku": "BOOK-JAVA-25",
      "quantity": 1
    }
  ],
  "status": "COMPLETED",
  "createdAt": "2026-06-01T10:00:00Z",
  "updatedAt": "2026-06-02T09:30:00Z"
}
```

The admin representation adds only `ownerSubject` and `tenantId`. It still omits
credentials, complete trusted context, source IP, idempotency keys, fingerprints,
and repository internals.

## Order Item

| Field | Type | Validation |
| --- | --- | --- |
| `sku` | string | Required, trimmed, 1-64 visible characters |
| `quantity` | integer | Required, 1-100 |

Duplicate SKUs in one request are rejected rather than silently merged.

## Pagination Envelope

```json
{
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1,
  "items": []
}
```

| Field | Type | Rule |
| --- | --- | --- |
| `page` | integer | Zero-based current page |
| `size` | integer | Requested bounded page size |
| `totalElements` | integer | Non-negative matching records |
| `totalPages` | integer | `0` when no elements |
| `items` | array | Ordered response items |

Collection ordering is always `createdAt DESC, id DESC`.

## List Current User Orders

### `GET /internal/v1/orders`

Returns orders belonging to the exact trusted `(tenantId, subject)` owner.

Required scope: `orders:read`.

Query parameters:

| Name | Type | Default | Validation |
| --- | --- | ---: | --- |
| `page` | integer | `0` | `0-10000` |
| `size` | integer | `20` | `1-100` |
| `status` | enum | absent | Optional exact status |

Unknown query parameters are rejected to keep the contract deterministic.

Successful response: `200 OK`

```json
{
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1,
  "items": [
    {
      "id": "10000000-0000-4000-8000-000000000002",
      "items": [
        {
          "sku": "SECURE-GATEWAY-LAB",
          "quantity": 1
        }
      ],
      "status": "CREATED",
      "createdAt": "2026-06-10T14:00:00Z",
      "updatedAt": "2026-06-10T14:00:00Z"
    }
  ]
}
```

An empty collection returns `200` with `items: []`, `totalElements: 0`, and
`totalPages: 0`.

Failure responses:

| Condition | Status | Code |
| --- | ---: | --- |
| Missing/malformed gateway context | 401 | `ORD_TRUSTED_CONTEXT_REQUIRED` |
| Actor is not `USER` | 403 | `ORD_ACTOR_NOT_ALLOWED` |
| Wrong route ID | 403 | `ORD_ROUTE_NOT_ALLOWED` |
| Required scope absent | 403 | `ORD_SCOPE_REQUIRED` |
| Invalid/unknown query parameter | 400 | `ORD_REQUEST_INVALID` |
| Repository unavailable | 503 | `ORD_DEPENDENCY_UNAVAILABLE` |

## Get Current User Order

### `GET /internal/v1/orders/{id}`

Returns one order only when it belongs to the exact trusted owner.

Required scope: `orders:read`.

Path parameter:

| Name | Type | Rule |
| --- | --- | --- |
| `id` | UUID | Required canonical order identifier |

Successful response: `200 OK`, using the user order representation.

Failure responses:

| Condition | Status | Code |
| --- | ---: | --- |
| Missing/malformed gateway context | 401 | `ORD_TRUSTED_CONTEXT_REQUIRED` |
| Actor is not `USER` | 403 | `ORD_ACTOR_NOT_ALLOWED` |
| Wrong route ID | 403 | `ORD_ROUTE_NOT_ALLOWED` |
| Required scope absent | 403 | `ORD_SCOPE_REQUIRED` |
| Invalid UUID | 400 | `ORD_REQUEST_INVALID` |
| Unknown, foreign-subject, or foreign-tenant order | 404 | `ORD_ORDER_NOT_FOUND` |
| Repository unavailable | 503 | `ORD_DEPENDENCY_UNAVAILABLE` |

The response intentionally does not distinguish an unknown order from a foreign
order.

## Create Current User Order

### `POST /internal/v1/orders`

Creates one order owned by the trusted subject and trusted tenant context.

Required scope: `orders:write`.

Optional request header:

```text
Idempotency-Key: order-create-01J...
```

Request:

```json
{
  "items": [
    {
      "sku": "BOOK-JAVA-25",
      "quantity": 1
    },
    {
      "sku": "SECURE-GATEWAY-LAB",
      "quantity": 2
    }
  ]
}
```

Request fields:

| Field | Type | Validation |
| --- | --- | --- |
| `items` | array | Required, 1-50 elements |
| `items[].sku` | string | Required, trimmed, 1-64 visible characters, unique in request |
| `items[].quantity` | integer | Required, 1-100 |

Fields `id`, `ownerSubject`, `tenantId`, `status`, `createdAt`, and `updatedAt`
are server-controlled and rejected if supplied.

Successful original response: `201 Created`

Headers:

```text
Location: /internal/v1/orders/<generated UUID>
X-Request-Id: <approved request ID>
Cache-Control: no-store
Idempotency-Replayed: false
```

The body is the created user order representation with `status: CREATED`.
`Idempotency-Replayed` is present only when the request supplied a valid
`Idempotency-Key`; it is absent for an unkeyed create.

Successful replay response: `201 Created`

- Same status, body, and `Location` as the original.
- `Idempotency-Replayed: true`.
- No second order is created.
- The current request still receives its own approved `X-Request-Id`; stored
  response data does not overwrite correlation.

Failure responses:

| Condition | Status | Code |
| --- | ---: | --- |
| Missing/malformed gateway context | 401 | `ORD_TRUSTED_CONTEXT_REQUIRED` |
| Actor is not `USER` | 403 | `ORD_ACTOR_NOT_ALLOWED` |
| Wrong route ID | 403 | `ORD_ROUTE_NOT_ALLOWED` |
| Required scope absent | 403 | `ORD_SCOPE_REQUIRED` |
| Unsupported media type | 415 | `ORD_MEDIA_TYPE_UNSUPPORTED` |
| Body exceeds configured limit | 413 | `ORD_BODY_TOO_LARGE` |
| Malformed JSON, unknown field, invalid item, or duplicate SKU | 400 | `ORD_REQUEST_INVALID` |
| Invalid idempotency key | 400 | `ORD_IDEMPOTENCY_KEY_INVALID` |
| Same key used with a different validated request | 409 | `ORD_IDEMPOTENCY_CONFLICT` |
| Safe idempotency record capacity is exhausted | 503 | `ORD_IDEMPOTENCY_CAPACITY_EXCEEDED` |
| Repository unavailable | 503 | `ORD_DEPENDENCY_UNAVAILABLE` |

The service never returns successful fallback content for create.

## List Orders As Administrator

### `GET /internal/v1/admin/orders`

Returns bounded administrator order representations across owners.

Required role: `ORDER_ADMIN`.

Query parameters:

| Name | Type | Default | Validation |
| --- | --- | ---: | --- |
| `page` | integer | `0` | `0-10000` |
| `size` | integer | `20` | `1-100` |
| `status` | enum | absent | Optional exact status |
| `tenantId` | string | absent | Optional exact value, 1-128 characters |
| `subject` | string | absent | Optional exact value, 1-255 characters |

The endpoint does not support substring searches, arbitrary sort expressions, or
unbounded export.

Successful response: `200 OK`, using the pagination envelope with administrator
order representations.

Failure responses:

| Condition | Status | Code |
| --- | ---: | --- |
| Missing/malformed gateway context | 401 | `ORD_TRUSTED_CONTEXT_REQUIRED` |
| Actor is not `USER` | 403 | `ORD_ACTOR_NOT_ALLOWED` |
| Wrong route ID | 403 | `ORD_ROUTE_NOT_ALLOWED` |
| Required role absent | 403 | `ORD_ROLE_REQUIRED` |
| Invalid/unknown query parameter | 400 | `ORD_REQUEST_INVALID` |
| Repository unavailable | 503 | `ORD_DEPENDENCY_UNAVAILABLE` |

## Idempotency Contract

The idempotency scope is:

```text
orders-create | normalized tenant partition | subject | Idempotency-Key
```

The request fingerprint is computed after JSON parsing and validation from the
ordered item list. JSON whitespace and object-property order therefore do not
change the fingerprint; item order does.

Requirements:

1. The key is one visible ASCII value of 1-128 characters.
2. Duplicate `Idempotency-Key` headers are invalid.
3. The key is never accepted from a query parameter or request body.
4. The original result and order are committed atomically.
5. A same-payload replay returns the original representation.
6. A different-payload replay returns `409`.
7. A record expires after the configured retention period, default 24 hours.
8. Expiry permits a later request to create a new order.
9. The key and request fingerprint are never returned in the body.
10. Gateway retries remain disabled unless they preserve the same key and body.

## Trusted Header Encoding

| Header | Encoding and validation |
| --- | --- |
| `X-Sentra-Request-Id` | One visible ASCII value, maximum 128 characters |
| `X-Sentra-Subject` | One nonblank value, maximum 255 characters |
| `X-Sentra-Actor-Type` | Uppercase token; `USER` required |
| `X-Sentra-Tenant-Id` | Optional opaque value, maximum 128 characters |
| `X-Sentra-Roles` | Gateway-defined escaped list; bounded |
| `X-Sentra-Scopes` | Gateway-defined escaped list; bounded |
| `X-Sentra-Route-Id` | Exact lowercase route ID, maximum 128 characters |
| `X-Sentra-Source-Ip` | Optional canonical IPv4 or IPv6 text |
| `X-Sentra-Auth-Time` | Optional validated RFC 3339 instant |

`X-Sentra-Client-Id` is not valid ownership for user order routes. A request that
mixes a user actor with contradictory API-client identity is rejected.

## Error Contract

```json
{
  "timestamp": "2026-06-15T12:00:00Z",
  "requestId": "8e3a95b8-6674-423e-83e6-0df84c2d66d0",
  "status": 409,
  "code": "ORD_IDEMPOTENCY_CONFLICT",
  "message": "The idempotency key was already used for a different request.",
  "path": "/internal/v1/orders",
  "routeId": "orders-create",
  "details": [
    {
      "field": "Idempotency-Key",
      "code": "conflict",
      "message": "Use a new key for a different order request."
    }
  ]
}
```

Error fields:

| Field | Type | Rule |
| --- | --- | --- |
| `timestamp` | instant | Server UTC time |
| `requestId` | string | Same approved value returned in `X-Request-Id` |
| `status` | integer | HTTP status |
| `code` | string | Stable machine-readable service code |
| `message` | string | Sanitized client-safe message |
| `path` | string | Internal path without query string |
| `routeId` | string/null | Trusted route ID when valid and available |
| `details` | array | Bounded field-level details |

Primary codes:

| Code | Status |
| --- | ---: |
| `ORD_REQUEST_INVALID` | 400 |
| `ORD_IDEMPOTENCY_KEY_INVALID` | 400 |
| `ORD_TRUSTED_CONTEXT_REQUIRED` | 401 |
| `ORD_ACTOR_NOT_ALLOWED` | 403 |
| `ORD_ROUTE_NOT_ALLOWED` | 403 |
| `ORD_SCOPE_REQUIRED` | 403 |
| `ORD_ROLE_REQUIRED` | 403 |
| `ORD_ORDER_NOT_FOUND` | 404 |
| `ORD_IDEMPOTENCY_CONFLICT` | 409 |
| `ORD_BODY_TOO_LARGE` | 413 |
| `ORD_MEDIA_TYPE_UNSUPPORTED` | 415 |
| `ORD_IDEMPOTENCY_CAPACITY_EXCEEDED` | 503 |
| `ORD_DEPENDENCY_UNAVAILABLE` | 503 |
| `ORD_INTERNAL_ERROR` | 500 |

No error may expose stack traces, repository details, hostnames, SQL, submitted
item arrays, owner identities, tokens, or idempotency values.

## Management Contract

| Method | Path | Exposure |
| --- | --- | --- |
| `GET` | `/actuator/health/liveness` | Internal health |
| `GET` | `/actuator/health/readiness` | Internal health |
| `GET` | `/actuator/prometheus` | Operations network |
| `GET` | `/actuator/metrics` | Protected operations access |
| `GET` | `/v3/api-docs` | Local/test or protected operations access |
| `GET` | `/swagger-ui.html` | Local/test or protected operations access |

Management endpoints are not gateway-routed order APIs.

## OpenAPI Requirements

Generated OpenAPI must include:

- all four internal operations;
- path, query, trusted-header, and idempotency-header definitions;
- user and administrator response schemas;
- pagination and item schemas;
- every documented response status and `ApiError`;
- constraints, formats, nullability, enums, and examples;
- the statement that external JWT authentication is performed by the gateway;
- the statement that direct access is unsupported.

OpenAPI contract tests must fail when any documented route, header, status, or
schema is missing.
