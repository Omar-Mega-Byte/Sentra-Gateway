# Payment Service API Contract

**Version:** 1.0.0  
**Target internal base URL:** `http://payment-service:8083`  
**External base URL:** gateway deployment URL  
**Content type:** `application/json; charset=UTF-8`  
**Implementation status:** Implemented for the direct internal service contract;
gateway end-to-end API-key, HMAC, nonce, and replay scenarios require the
gateway stack.

## Routing And Authentication

Partners call the gateway. Internal paths are not public APIs.

| External request | Internal request | Gateway auth | Signing | Service permission | Route ID |
| --- | --- | --- | --- | --- | --- |
| `GET /api/v1/partner/payments/{id}` | `GET /internal/v1/payments/{id}` | API key | policy-controlled | `payments:read` | `partner-payment-read` |
| `POST /api/v1/partner/payments` | `POST /internal/v1/payments` | API key | required | `payments:write` | `partner-payment-create` |
| `POST /api/v1/partner/refunds` | `POST /internal/v1/refunds` | API key | required | `refunds:write` | `partner-refund-create` |

The gateway strips plaintext key material, external signature headers, and any
inbound reserved `X-Sentra-*` values. The service consumes only sanitized trusted
context.

## Common Rules

- JSON uses UTF-8.
- Timestamps are RFC 3339 UTC instants.
- IDs are canonical UUID strings.
- Unknown JSON fields are rejected.
- Mutation requests require `Content-Type: application/json`.
- Successful JSON responses use `application/json`.
- The default maximum request body is 16 KiB.
- `POST` payment and refund require `Idempotency-Key`.
- Responses include `X-Request-Id`.
- Mutation responses use `Cache-Control: no-store`.
- The service OpenAPI describes internal paths.
- Gateway OpenAPI/route catalog describes external API-key and signing headers.
- Create/refund routes must not be automatically retried unless the idempotency
  key and body are preserved.

## Required Trusted Context

### Common Partner Headers

```text
X-Sentra-Request-Id: <approved request ID>
X-Sentra-Actor-Type: API_CLIENT
X-Sentra-Client-Id: <validated partner client>
X-Sentra-Key-Id: <validated API key identifier>
X-Sentra-Scopes: ... payments:read|payments:write|refunds:write ...
X-Sentra-Route-Id: <exact route ID>
```

### Signed Route Evidence

Required for `partner-payment-create` and `partner-refund-create`:

```text
X-Sentra-Signature-Verified: true
X-Sentra-Signature-Key-Id: <validated signing key identifier>
X-Sentra-Nonce-Status: accepted
```

`partner-payment-read` consumes the same evidence when gateway route policy
marks signing as required.

Duplicate security-critical trusted headers are rejected. If `Authorization`,
`X-Api-Key`, `X-Signature`, or equivalent plaintext external credential headers
reach the service, the request is rejected as a gateway contract violation.

## Payment Response

```json
{
  "id": "40000000-0000-4000-8000-000000000001",
  "merchantReference": "acme-order-1001",
  "amount": "125.50",
  "currency": "USD",
  "status": "CAPTURED",
  "createdAt": "2026-06-01T10:00:00Z",
  "updatedAt": "2026-06-01T10:00:00Z"
}
```

The partner response does not include `clientId`, API key ID, signature metadata,
nonce, idempotency key, body hash, internal provider simulation reason, or
security context.

## Refund Response

```json
{
  "id": "60000000-0000-4000-8000-000000000001",
  "paymentId": "40000000-0000-4000-8000-000000000001",
  "merchantReference": "acme-refund-1001",
  "amount": "25.00",
  "currency": "USD",
  "status": "ACCEPTED",
  "createdAt": "2026-06-02T10:00:00Z"
}
```

## Money Fields

| Field | Type | Rule |
| --- | --- | --- |
| `amount` | string | Decimal, exactly two fractional digits |
| `currency` | string | Uppercase 3-letter ISO-style code |

The service treats amounts as decimal values. Floating-point parsing is
prohibited.

## Get Payment

### `GET /internal/v1/payments/{id}`

Returns one payment when it belongs to the trusted `X-Sentra-Client-Id`.

Required scope: `payments:read`.

Path parameter:

| Name | Type | Rule |
| --- | --- | --- |
| `id` | UUID | Required canonical payment ID |

Successful response: `200 OK`.

Failure responses:

| Condition | Status | Code |
| --- | ---: | --- |
| Missing/malformed trusted context | 401 | `PAY_TRUSTED_CONTEXT_REQUIRED` |
| Actor is not `API_CLIENT` | 403 | `PAY_ACTOR_NOT_ALLOWED` |
| Wrong route ID | 403 | `PAY_ROUTE_NOT_ALLOWED` |
| Required scope absent | 403 | `PAY_SCOPE_REQUIRED` |
| Signing evidence required by route but missing/false | 403 | `PAY_SIGNATURE_CONTEXT_REQUIRED` |
| Invalid UUID | 400 | `PAY_REQUEST_INVALID` |
| Unknown or foreign-client payment | 404 | `PAY_PAYMENT_NOT_FOUND` |
| Repository unavailable | 503 | `PAY_DEPENDENCY_UNAVAILABLE` |

Unknown and foreign-client payments are intentionally indistinguishable.

## Create Payment

### `POST /internal/v1/payments`

Creates a deterministic mock payment for the trusted partner client.

Required scope: `payments:write`.
Required signing evidence: successful.
Required header: `Idempotency-Key`.

Request:

```json
{
  "merchantReference": "acme-order-1002",
  "amount": "125.50",
  "currency": "USD",
  "description": "Security gateway lab payment"
}
```

| Field | Type | Validation |
| --- | --- | --- |
| `merchantReference` | string | Required, trimmed, 1-128 visible characters |
| `amount` | string | Required, `0.01` to configured max, two decimal places |
| `currency` | string | Required, uppercase 3 letters |
| `description` | string/null | Optional, at most 255 characters |

Fields `id`, `clientId`, `keyId`, `status`, `createdAt`, `updatedAt`, signature
data, nonce data, and idempotency metadata are server-controlled and rejected.

Successful original response: `201 Created`.

Headers:

```text
Location: /internal/v1/payments/<generated UUID>
Idempotency-Replayed: false
X-Request-Id: <approved request ID>
Cache-Control: no-store
```

The body is a payment response. Baseline status is `AUTHORIZED` unless the
deterministic mock rule returns `DECLINED`.

Successful replay response:

- same `201`, `Location`, and body as the original;
- `Idempotency-Replayed: true`;
- no second payment created;
- current request ID is still returned in `X-Request-Id`.

Failure responses:

| Condition | Status | Code |
| --- | ---: | --- |
| Missing/malformed trusted context | 401 | `PAY_TRUSTED_CONTEXT_REQUIRED` |
| Actor is not `API_CLIENT` | 403 | `PAY_ACTOR_NOT_ALLOWED` |
| Wrong route ID | 403 | `PAY_ROUTE_NOT_ALLOWED` |
| Required scope absent | 403 | `PAY_SCOPE_REQUIRED` |
| Missing/false signature evidence | 403 | `PAY_SIGNATURE_CONTEXT_REQUIRED` |
| Unsupported media type | 415 | `PAY_MEDIA_TYPE_UNSUPPORTED` |
| Body exceeds configured limit | 413 | `PAY_BODY_TOO_LARGE` |
| Malformed JSON, unknown field, invalid value | 400 | `PAY_REQUEST_INVALID` |
| Missing/invalid idempotency key | 400 | `PAY_IDEMPOTENCY_KEY_REQUIRED` |
| Same idempotency key with different validated request | 409 | `PAY_IDEMPOTENCY_CONFLICT` |
| Merchant reference already exists for the client | 409 | `PAY_REFERENCE_CONFLICT` |
| Safe idempotency capacity exhausted | 503 | `PAY_IDEMPOTENCY_CAPACITY_EXCEEDED` |
| Repository unavailable | 503 | `PAY_DEPENDENCY_UNAVAILABLE` |

## Create Refund

### `POST /internal/v1/refunds`

Creates a deterministic mock refund for a payment owned by the trusted partner
client.

Required scope: `refunds:write`.
Required signing evidence: successful.
Required header: `Idempotency-Key`.

Request:

```json
{
  "paymentId": "40000000-0000-4000-8000-000000000001",
  "merchantReference": "acme-refund-1002",
  "amount": "25.00"
}
```

| Field | Type | Validation |
| --- | --- | --- |
| `paymentId` | UUID | Required payment owned by trusted client |
| `merchantReference` | string/null | Optional, 1-128 visible characters when present |
| `amount` | string | Required, positive, two decimals, not greater than refundable amount |

Currency is inherited from the payment and cannot be submitted.

Successful original response: `201 Created`.

Headers:

```text
Location: /internal/v1/refunds/<generated UUID>
Idempotency-Replayed: false
X-Request-Id: <approved request ID>
Cache-Control: no-store
```

Failure responses:

| Condition | Status | Code |
| --- | ---: | --- |
| Missing/malformed trusted context | 401 | `PAY_TRUSTED_CONTEXT_REQUIRED` |
| Actor is not `API_CLIENT` | 403 | `PAY_ACTOR_NOT_ALLOWED` |
| Wrong route ID | 403 | `PAY_ROUTE_NOT_ALLOWED` |
| Required scope absent | 403 | `PAY_SCOPE_REQUIRED` |
| Missing/false signature evidence | 403 | `PAY_SIGNATURE_CONTEXT_REQUIRED` |
| Unsupported media type | 415 | `PAY_MEDIA_TYPE_UNSUPPORTED` |
| Body exceeds configured limit | 413 | `PAY_BODY_TOO_LARGE` |
| Malformed JSON, unknown field, invalid value | 400 | `PAY_REQUEST_INVALID` |
| Missing/invalid idempotency key | 400 | `PAY_IDEMPOTENCY_KEY_REQUIRED` |
| Unknown or foreign-client payment | 404 | `PAY_PAYMENT_NOT_FOUND` |
| Payment is not refundable or amount exceeds remaining refundable amount | 409 | `PAY_REFUND_NOT_ALLOWED` |
| Same idempotency key with different validated request | 409 | `PAY_IDEMPOTENCY_CONFLICT` |
| Merchant refund reference already exists for the client | 409 | `PAY_REFERENCE_CONFLICT` |
| Safe idempotency capacity exhausted | 503 | `PAY_IDEMPOTENCY_CAPACITY_EXCEEDED` |
| Repository unavailable | 503 | `PAY_DEPENDENCY_UNAVAILABLE` |

## Idempotency Contract

Idempotency scope:

```text
route ID | client ID | Idempotency-Key
```

Requirements:

1. `Idempotency-Key` is required for both mutation routes.
2. The key is one visible ASCII value of 1-128 characters.
3. Duplicate key headers are invalid.
4. The key is never accepted from query or body.
5. Fingerprint is calculated from validated canonical request fields.
6. The mutation and idempotency record are committed atomically.
7. Same key/same fingerprint returns the original response.
8. Same key/different fingerprint returns `409`.
9. Records expire after configured retention, default 24 hours.
10. Expiry permits later reuse as a new operation.

## Error Contract

```json
{
  "timestamp": "2026-06-16T12:00:00Z",
  "requestId": "8e3a95b8-6674-423e-83e6-0df84c2d66d0",
  "status": 403,
  "code": "PAY_SIGNATURE_CONTEXT_REQUIRED",
  "message": "A verified request signature is required for this payment route.",
  "path": "/internal/v1/payments",
  "routeId": "partner-payment-create",
  "details": []
}
```

Primary codes:

| Code | Status |
| --- | ---: |
| `PAY_REQUEST_INVALID` | 400 |
| `PAY_IDEMPOTENCY_KEY_REQUIRED` | 400 |
| `PAY_TRUSTED_CONTEXT_REQUIRED` | 401 |
| `PAY_ACTOR_NOT_ALLOWED` | 403 |
| `PAY_ROUTE_NOT_ALLOWED` | 403 |
| `PAY_SCOPE_REQUIRED` | 403 |
| `PAY_SIGNATURE_CONTEXT_REQUIRED` | 403 |
| `PAY_PAYMENT_NOT_FOUND` | 404 |
| `PAY_REFUND_NOT_FOUND` | 404 |
| `PAY_IDEMPOTENCY_CONFLICT` | 409 |
| `PAY_REFERENCE_CONFLICT` | 409 |
| `PAY_REFUND_NOT_ALLOWED` | 409 |
| `PAY_BODY_TOO_LARGE` | 413 |
| `PAY_MEDIA_TYPE_UNSUPPORTED` | 415 |
| `PAY_IDEMPOTENCY_CAPACITY_EXCEEDED` | 503 |
| `PAY_DEPENDENCY_UNAVAILABLE` | 503 |
| `PAY_INTERNAL_ERROR` | 500 |

Errors never expose plaintext keys, signatures, nonces, canonical strings,
request bodies, provider internals, hostnames, stack traces, or cryptographic
details.

## Management Contract

| Method | Path | Exposure |
| --- | --- | --- |
| `GET` | `/actuator/health/liveness` | Internal health |
| `GET` | `/actuator/health/readiness` | Internal health |
| `GET` | `/actuator/prometheus` | Operations network |
| `GET` | `/actuator/metrics` | Protected operations access |
| `GET` | `/v3/api-docs` | Local/test or protected operations access |
| `GET` | `/swagger-ui.html` | Local/test or protected operations access |

Management endpoints are not public partner APIs.

## OpenAPI Requirements

Generated OpenAPI must include:

- all three internal operations;
- trusted client headers and signature-evidence headers;
- `Idempotency-Key`;
- payment/refund request and response schemas;
- every documented status and `ApiError`;
- field lengths, formats, enums, nullability, and examples;
- a statement that API-key validation, signature validation, and replay checks
  are performed by the gateway;
- a statement that direct access is unsupported.
