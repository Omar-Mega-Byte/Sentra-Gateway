# Sentra Gateway Console Testing Guide

This guide covers manual testing for every UI page in the Next.js frontend.

## 1. Setup

From `frontend`:

```bash
npm install
npm run dev
```

Open:

```txt
http://127.0.0.1:3000
```

Run automated checks:

```bash
npm run lint
npm run typecheck
npm run test
npm run build
```

## 2. Backend Requirements

The UI calls the backend through the frontend proxy at `/api/proxy`.

Default backend targets:

```txt
Gateway:       http://localhost:8080
User:          http://localhost:8081
Order:         http://localhost:8082
Payment:       http://localhost:8083
Notification:  http://localhost:8084
```

Override these in `.env.local` if needed:

```txt
GATEWAY_BASE_URL=http://localhost:8080
USER_SERVICE_BASE_URL=http://localhost:8081
ORDER_SERVICE_BASE_URL=http://localhost:8082
PAYMENT_SERVICE_BASE_URL=http://localhost:8083
NOTIFICATION_SERVICE_BASE_URL=http://localhost:8084
```

If services are offline, requests should show a structured proxy error instead of breaking the page:

```json
{
  "status": 599,
  "body": {
    "code": "FRONTEND_PROXY_ERROR"
  }
}
```

## 3. Authentication Setup

Go to `Authentication`.

For local gateway admin endpoints, use:

```txt
Auth mode: Basic
Username: admin
Password: sentra-admin
```

For user routes, paste a real JWT issued by the configured gateway issuer. To exercise the seeded user/order/notification records, the JWT should resolve to:

```txt
sub: sentra-user-omar
tenant_id: tenant-demo
scope: profile:read profile:write orders:read orders:write notifications:read notifications:write
roles: USER_ADMIN ORDER_ADMIN NOTIFICATION_ADMIN
```

If your issuer uses Keycloak roles, the gateway also reads `realm_access.roles` and `resource_access.*.roles`.

For partner payment routes, set:

```txt
Auth mode: API key
API Key: issued plaintext key from API Key Management
API Key ID: issued key UUID from API Key Management
```

The identity preview should decode JWT subject, tenant, roles, scopes, and claims.

## 3.1 Real Seed Values

Use these values for manual testing when local/test seeds are enabled.

### Gateway Admin

```txt
Basic username: admin
Basic password: sentra-admin
Operator username: operator
Operator password: sentra-operator
```

### User Service

```txt
Active profile ID: 7aa99db8-a943-4b63-b4b7-79f769ef9f87
Active subject: sentra-user-omar
Tenant: tenant-demo
Display name: Omar Hassan
Email: omar@example.test
Profile version: 3

Disabled profile ID: 11111111-1111-4111-8111-111111111111
Disabled subject: sentra-user-disabled
Disabled profile version: 1

Deleted profile ID: 22222222-2222-4222-8222-222222222222
Deleted subject: sentra-user-deleted
Deleted profile version: 1
```

### Order Service

```txt
Owned completed order ID: 10000000-0000-4000-8000-000000000001
Owned completed order version: 2
Owned created order ID: 10000000-0000-4000-8000-000000000002
Owned created order version: 1
Foreign subject order ID: 20000000-0000-4000-8000-000000000001
Foreign tenant order ID: 30000000-0000-4000-8000-000000000001

Seed item SKU: BOOK-JAVA-25
Create item SKU: SECURE-GATEWAY-LAB
Create quantity: 1
Order idempotency key: ui-order-create-001
```

### Payment Service

```txt
Seed client name used inside payment records: partner-acme
Captured payment ID: 40000000-0000-4000-8000-000000000001
Declined payment ID: 40000000-0000-4000-8000-000000000002
Foreign client payment ID: 50000000-0000-4000-8000-000000000001
Accepted refund ID: 60000000-0000-4000-8000-000000000001

Create merchant reference: ui-acme-order-1002
Create amount: 125.50
Create currency: USD
Create payment idempotency key: ui-payment-create-001

Refund merchant reference: ui-acme-refund-1002
Refund amount: 25.00
Refund idempotency key: ui-refund-create-001
```

The gateway does not seed API clients or keys. Create one before payment tests:

```txt
Client name: partner-acme-ui
Owner: QA Console
Tenant ID: tenant-demo

Key scopes:
payments:read
payments:write
refunds:write

Allowed routes:
partner-payment-read
partner-payment-create
partner-refund-create
```

After issuing the key, copy:

```txt
apiKey -> Authentication / API Key
keyId  -> Authentication / API Key ID
```

### Notification Service

```txt
Notification ID 1: 70000000-0000-4000-8000-000000000001
Notification 1 channel: EMAIL
Notification 1 status: SENT

Notification ID 2: 70000000-0000-4000-8000-000000000002
Notification 2 channel: PUSH
Notification 2 status: QUEUED

Other subject notification ID: 80000000-0000-4000-8000-000000000001
Other tenant notification ID: 90000000-0000-4000-8000-000000000001

Preference version for sentra-user-omar / tenant-demo: 2
Admin test recipient: ui-test-recipient
Admin test message: Gateway resilience smoke test from UI
```

### Seeded Gateway Route IDs

```txt
user-public-profile
user-profile-create
user-profile-read
user-profile-update
admin-users-list
admin-users-enable
admin-users-disable
admin-users-delete
orders-list
orders-create
orders-cancel
orders-get
admin-orders-list
admin-orders-update
partner-payment-read
partner-payment-create
partner-refund-create
notifications-list
notification-preferences-update
admin-test-notification
```

## 4. Dashboard

Page: `/`

Test:

- Gateway health card loads from `/actuator/health`.
- Service health table checks gateway, user, order, payment, notification.
- Active route and signed route counts load when Basic auth is configured.
- Recent audit events load when auditor/admin permissions are available.
- Gateway metrics preview loads from `/actuator/metrics`.

Expected:

- Online services show `200` and health status.
- Unauthorized admin calls show visible errors, not blank UI.
- Offline backend shows structured proxy errors.

## 5. Authentication Panel

Page: `/auth`

Test:

- Change auth mode between `none`, `jwt`, `apiKey`, and `basic`.
- Paste JWT and verify decoded identity preview.
- Set API key and key ID.
- Set Basic credentials.
- Refresh the page and verify values persist in local storage.
- Run the live profile probe.
- Run the live payment probe with a payment ID.

Expected:

- Header preview changes with selected credentials.
- JWT preview shows roles/scopes from `roles`, `scope`, `scp`, or Keycloak role claims.
- Invalid JWT is shown as invalid without crashing.

## 6. Route Management

Page: `/routes`

Test:

- List routes.
- Get route by ID: `user-profile-read`.
- Validate a route body.
- Create a route with a non-reserved path and allowlisted target host.
- Update the route using its current `version`.
- Disable the route.
- Enable the route.
- Delete the route.
- Check route generation.
- Run the forwarding test panel against a known gateway path.

Expected:

- Route list shows category, methods, paths, enabled, signed, and version.
- Invalid routes return `GW_REQUEST_INVALID`.
- Stale version updates return conflict.

Concrete route create/update body:

```json
{
  "id": "ui-public-profile-copy",
  "category": "PUBLIC",
  "pathPatterns": ["/api/v1/ui/public/users/*"],
  "methods": ["GET"],
  "targetUri": "http://user-service:8081",
  "stripPrefix": 0,
  "rewriteRegex": "/api/v1/ui/public/users/(?<id>[^/]+)",
  "rewriteReplacement": "/internal/v1/users/${id}/public",
  "order": 900,
  "enabled": true,
  "authentication": ["NONE"],
  "requiredRoles": [],
  "requiredScopes": [],
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
    "name": "user-service"
  },
  "auditMode": "DENIALS_AND_MUTATIONS",
  "version": 0
}
```

## 7. API Client Management

Page: `/clients`

Test:

- List API clients.
- Create a client with:

```txt
Name: partner-acme-ui
Owner: QA Console
Tenant ID: tenant-demo
```

- Get the client by UUID.
- Update name, owner, tenant, status, and version.
- Disable the client.

Expected:

- New client appears in the list.
- Disable changes status to `DISABLED`.
- Stale version updates return conflict.

## 8. API Key Management

Page: `/api-keys`

Test:

- List key metadata for a client ID.
- Issue a key using scopes and allowed routes.
- Copy the returned plaintext key immediately.
- Rotate an active key.
- Revoke a key.

Expected:

- Issued key response contains `apiKey`, `keyId`, `prefix`, `createdAt`, and warning.
- Plaintext key appears only in the issue/rotate response.
- Metadata list never shows verifier material.

Concrete key issue body:

```json
{
  "scopes": ["payments:read", "payments:write", "refunds:write"],
  "allowedRoutes": [
    "partner-payment-read",
    "partner-payment-create",
    "partner-refund-create"
  ],
  "expiresAt": "2026-12-31T23:59:59Z"
}
```

Header:

```txt
Idempotency-Key: ui-key-issue-001
```

## 9. Rate Limit Policies

Page: `/rate-limits`

Test:

- List policies.
- Create a policy with subject type, capacity, refill values, outage mode.
- Update the policy using `version`.
- Delete the policy.
- Attach the policy ID to a route through Route Management.
- Run the behavior test multiple times against that route.

Expected:

- Policy CRUD uses real `/api/v1/admin/rate-limits` endpoints.
- If attached and Redis is available, repeated requests eventually return `429` with gateway rate-limit error.
- Response may include `RateLimit-Remaining`.

Concrete policy:

```json
{
  "id": "ui-user-profile-read-rate",
  "subjectType": "SUBJECT",
  "routeId": "user-profile-read",
  "method": "GET",
  "capacity": 2,
  "refillTokens": 2,
  "refillPeriodSeconds": 60,
  "priority": 100,
  "redisOutageMode": "DENY",
  "responseHeadersEnabled": true,
  "enabled": true,
  "version": 0
}
```

## 10. IP Rules

Page: `/ip-rules`

Test:

- List IP rules.
- Create an allow/block/temp-block rule.
- Update using `version`.
- Delete the rule.
- Attach the rule ID to a route.
- Run Gateway Decision Test against that route.

Expected:

- Rule CRUD uses `/api/v1/admin/ip-rules`.
- There is no separate backend decision endpoint; decisions are tested by real gateway traffic.
- Matching block/temp-block rules return gateway IP denial.

Concrete allow rule:

```json
{
  "id": "ui-localhost-allow",
  "network": "127.0.0.1/32",
  "action": "ALLOW",
  "routeId": "user-public-profile",
  "priority": 100,
  "reason": "Allow local UI smoke test",
  "validFrom": "2026-06-17T00:00:00Z",
  "expiresAt": "2026-12-31T23:59:59Z",
  "enabled": true,
  "version": 0
}
```

Concrete block rule:

```json
{
  "id": "ui-localhost-block",
  "network": "127.0.0.1/32",
  "action": "BLOCK",
  "routeId": "user-public-profile",
  "priority": 200,
  "reason": "Block local UI smoke test",
  "validFrom": "2026-06-17T00:00:00Z",
  "expiresAt": "2026-12-31T23:59:59Z",
  "enabled": true,
  "version": 0
}
```

## 11. Request Signing Playground

Page: `/signing`

Test:

- Enter API key and API key ID.
- Use path `/api/v1/partner/payments`.
- Generate canonical string and signature.
- Send signed payment request.
- Click `Send Replay` with same nonce/timestamp/signature.
- Repeat for `/api/v1/partner/refunds`.

Expected:

- Signed payment/refund requests include:
  - `X-API-Key`
  - `X-Sentra-Key-Id`
  - `X-Sentra-Timestamp`
  - `X-Sentra-Nonce`
  - `X-Sentra-Signature`
  - `Idempotency-Key`
- First valid request succeeds or returns a service-level validation error.
- Replay with same nonce should return replay denial when gateway replay guard is active.

Concrete payment body:

```json
{
  "merchantReference": "ui-acme-order-1002",
  "amount": "125.50",
  "currency": "USD",
  "description": "Security gateway UI payment test"
}
```

Concrete refund body:

```json
{
  "paymentId": "40000000-0000-4000-8000-000000000001",
  "merchantReference": "ui-acme-refund-1002",
  "amount": "25.00"
}
```

## 12. Risk Rules

Page: `/risk-rules`

Test:

- List risk rules.
- Create rule for `HEADER_COUNT`, `QUERY_PARAMETER_COUNT`, or `PATH_SEGMENTS`.
- Update using `version`.
- Delete rule.
- Attach rule ID to a route.
- Run Risk Decision Test with enough headers/query/path shape to trigger the threshold.

Expected:

- CRUD uses `/api/v1/admin/risk-rules`.
- There is no separate backend decision endpoint; decisions are tested through real gateway traffic.
- `DENY` or `TEMP_BLOCK` rules return gateway risk denial.

Concrete observe rule:

```json
{
  "id": "ui-header-count-observe",
  "signal": "HEADER_COUNT",
  "thresholdValue": 20,
  "weight": 10,
  "action": "OBSERVE",
  "routeId": "user-public-profile",
  "enabled": true,
  "version": 0
}
```

Concrete deny rule:

```json
{
  "id": "ui-query-count-deny",
  "signal": "QUERY_PARAMETER_COUNT",
  "thresholdValue": 2,
  "weight": 50,
  "action": "DENY",
  "routeId": "user-public-profile",
  "enabled": true,
  "version": 0
}
```

## 13. Audit Logs

Page: `/audit`

Test:

- Search with a valid `from` and `to` range.
- Filter backend query by request ID and route ID.
- Apply UI-side filters for subject, HTTP status, and decision.
- Select an event and inspect details.
- Get one audit event by UUID.
- List admin actions.

Expected:

- Backend search supports only `from`, `to`, `requestId`, `routeId`, `page`, `pageSize`.
- Subject/status/decision filters apply to currently returned rows.
- Invalid date ranges return gateway request validation error.

## 14. Observability

Page: `/observability`

Test:

- Confirm service status table probes all five services.
- Select each target service.
- Probe:
  - Health
  - Metrics
  - Prometheus
  - OpenAPI

Expected:

- Health should be public for services.
- Gateway metrics/prometheus require Basic or operator auth.
- Downstream OpenAPI may be disabled unless service config enables it.

## 15. User Service Consumer

Page: `/services/user`

Test:

- Public profile lookup.
- Current profile read with JWT.
- Create current profile with JWT and `profile:write`.
- Update profile with JWT, mutable fields, and version.
- Admin users list with `USER_ADMIN`.
- Enable, disable, and delete user with version.

Expected:

- Public profile requires canonical UUID.
- Current profile is selected by trusted gateway subject, not by form fields.
- Admin lifecycle actions require role and optimistic version.

Concrete values:

```txt
Public profile ID: 7aa99db8-a943-4b63-b4b7-79f769ef9f87
Admin list status: ACTIVE
Admin list query: Omar
Disable/enable target ID: 11111111-1111-4111-8111-111111111111
Lifecycle version: 1
Patch profile version: 3
Patch displayName: Omar H.
Patch email: omar.hassan.ui@example.test
Patch locale: en-EG
Patch timezone: Africa/Cairo
```

## 16. Order Service Consumer

Page: `/services/order`

Test:

- List user orders.
- Get order by UUID.
- Create order with item list and optional idempotency key.
- Cancel order with version.
- Admin list orders with filters.
- Admin update order lifecycle fields.

Expected:

- Unknown query params are rejected by backend.
- Create supports idempotency.
- Cancel/update stale versions return conflict.

Concrete values:

```txt
List status: CREATED
Get order ID: 10000000-0000-4000-8000-000000000001
Cancel order ID: 10000000-0000-4000-8000-000000000002
Cancel version: 1
Admin filter tenantId: tenant-demo
Admin filter subject: sentra-user-omar
Admin update order ID: 10000000-0000-4000-8000-000000000002
Admin update version: 1
Admin update status: PROCESSING
Admin update paymentStatus: PENDING
Admin update fulfillmentStatus: PROCESSING
```

Create body:

```json
{
  "items": [
    {
      "sku": "SECURE-GATEWAY-LAB",
      "quantity": 1
    }
  ]
}
```

## 17. Payment Service Consumer

Page: `/services/payment`

Test:

- Get partner payment by UUID using API key.
- Create partner payment with API key, key ID, signing, and idempotency.
- Create partner refund with API key, key ID, signing, and idempotency.

Expected:

- Mutations require verified Sentra signing headers.
- Missing idempotency key returns payment idempotency error.
- Foreign/unknown payment IDs return not found.

Concrete values:

```txt
Get payment ID: 40000000-0000-4000-8000-000000000001
Declined payment ID: 40000000-0000-4000-8000-000000000002
Foreign payment ID: 50000000-0000-4000-8000-000000000001
Create idempotency key: ui-payment-create-001
Refund payment ID: 40000000-0000-4000-8000-000000000001
Refund idempotency key: ui-refund-create-001
```

## 18. Notification Service Consumer

Page: `/services/notification`

Test:

- List notifications with optional channel/status filters.
- Update notification preferences with all boolean fields and version.
- Run admin notification test for:
  - `SUCCESS`
  - `DELAY`
  - `FAILURE`
  - `MALFORMED`
  - `DISCONNECT`

Expected:

- Unknown query params are rejected.
- Preference version conflicts return `409`.
- Fault scenarios work only when backend fault controls are enabled in local/test config.

Concrete values:

```txt
List channel: EMAIL
List status: SENT
Preference version: 2
Preference body: emailEnabled=true, smsEnabled=false, pushEnabled=true, webhookEnabled=false
Admin test scenario: SUCCESS
Admin test channel: EMAIL
Admin test recipientReference: ui-test-recipient
Admin test message: Gateway resilience smoke test from UI
```

## 19. API Explorer

Page: `/explorer`

Test:

- Select each endpoint group.
- Edit target, method, path, query JSON, headers JSON, body, and auth mode.
- Send requests through the proxy.
- Inspect status, headers, response body, timing, and error details.

Expected:

- All discovered endpoints are selectable.
- Invalid JSON in query/headers shows a UI error.
- Gateway-owned `X-Sentra-*` headers should not be relied on from the browser because gateway strips spoofed values and injects trusted context itself.

## 20. Regression Checklist

Before considering the UI good:

- No hydration warnings in console.
- No `getSnapshot should be cached` warning.
- Sidebar navigation works on desktop.
- Mobile horizontal nav works.
- Every page renders without placeholder/TODO content.
- Empty, loading, and error states are visible.
- JSON request and response panels can copy content.
- Forms show required fields and backend errors clearly.
- `npm run lint` passes.
- `npm run typecheck` passes.
- `npm run test` passes.
- `npm run build` passes.
