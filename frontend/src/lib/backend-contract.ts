import type { AuthMode } from "@/lib/auth-store";
import type { ServiceTarget } from "@/lib/proxy-client";

export type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
export type FieldType =
  | "text"
  | "textarea"
  | "number"
  | "boolean"
  | "select"
  | "datetime"
  | "array"
  | "json";

export type FieldConfig = {
  name: string;
  label: string;
  type: FieldType;
  required?: boolean;
  options?: string[];
  defaultValue?: string | number | boolean | string[] | Record<string, unknown>;
  placeholder?: string;
  helperText?: string;
};

export type EndpointDefinition = {
  id: string;
  service: ServiceTarget;
  group: string;
  title: string;
  method: HttpMethod;
  path: string;
  auth: AuthMode;
  signingRequired?: boolean;
  pathParams?: FieldConfig[];
  queryParams?: FieldConfig[];
  headerFields?: FieldConfig[];
  bodyFields?: FieldConfig[];
  defaultBody?: unknown;
  tags?: string[];
};

export const ROUTE_CATEGORIES = ["PUBLIC", "USER", "PARTNER", "ADMIN", "INTERNAL"];
export const HTTP_METHODS = ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"];
export const AUTH_TYPES = ["JWT", "API_KEY", "NONE", "INTERNAL"];
export const CLIENT_STATUSES = ["ACTIVE", "DISABLED"];
export const KEY_STATUSES = ["ACTIVE", "REVOKED"];
export const RATE_SUBJECT_TYPES = ["SUBJECT", "CLIENT", "IP"];
export const REDIS_OUTAGE_MODES = ["DENY", "ALLOW", "LOCAL_FALLBACK"];
export const IP_ACTIONS = ["ALLOW", "BLOCK", "TEMP_BLOCK"];
export const RISK_SIGNALS = ["HEADER_COUNT", "QUERY_PARAMETER_COUNT", "PATH_SEGMENTS"];
export const RISK_ACTIONS = ["OBSERVE", "THROTTLE", "TEMP_BLOCK", "DENY"];
export const ORDER_STATUSES = ["CREATED", "PROCESSING", "COMPLETED", "CANCELLED"];
export const ORDER_PAYMENT_STATUSES = ["PENDING", "PAID", "FAILED"];
export const FULFILLMENT_STATUSES = [
  "UNFULFILLED",
  "PROCESSING",
  "SHIPPED",
  "DELIVERED",
  "CANCELLED",
];
export const PAYMENT_STATUSES = ["AUTHORIZED", "CAPTURED", "DECLINED", "REFUNDED"];
export const REFUND_STATUSES = ["ACCEPTED", "DECLINED"];
export const NOTIFICATION_CHANNELS = ["EMAIL", "SMS", "PUSH", "WEBHOOK"];
export const NOTIFICATION_STATUSES = ["QUEUED", "SENT", "FAILED", "SUPPRESSED"];
export const TEST_SCENARIOS = ["SUCCESS", "DELAY", "FAILURE", "MALFORMED", "DISCONNECT"];

const id: FieldConfig = { name: "id", label: "ID", type: "text", required: true };
const uuid: FieldConfig = {
  name: "id",
  label: "ID",
  type: "text",
  required: true,
  placeholder: "00000000-0000-4000-8000-000000000001",
};
const version: FieldConfig = {
  name: "version",
  label: "Version",
  type: "number",
  required: true,
  defaultValue: 1,
};
const page = { name: "page", label: "Page", type: "number", defaultValue: 0 } satisfies FieldConfig;
const size = { name: "size", label: "Size", type: "number", defaultValue: 20 } satisfies FieldConfig;
const idempotencyKey = {
  name: "Idempotency-Key",
  label: "Idempotency-Key",
  type: "text",
  required: true,
  defaultValue: "sentra-console-key",
} satisfies FieldConfig;

export const endpointRegistry: EndpointDefinition[] = [
  {
    id: "gateway-health",
    service: "gateway",
    group: "Observability",
    title: "Gateway health",
    method: "GET",
    path: "/actuator/health",
    auth: "none",
  },
  {
    id: "gateway-metrics-index",
    service: "gateway",
    group: "Observability",
    title: "Gateway metrics index",
    method: "GET",
    path: "/actuator/metrics",
    auth: "basic",
  },
  {
    id: "gateway-prometheus",
    service: "gateway",
    group: "Observability",
    title: "Gateway Prometheus",
    method: "GET",
    path: "/actuator/prometheus",
    auth: "basic",
  },
  {
    id: "routes-list",
    service: "gateway",
    group: "Gateway Routes",
    title: "List routes",
    method: "GET",
    path: "/api/v1/admin/routes",
    auth: "basic",
  },
  {
    id: "routes-get",
    service: "gateway",
    group: "Gateway Routes",
    title: "Get route",
    method: "GET",
    path: "/api/v1/admin/routes/{id}",
    auth: "basic",
    pathParams: [id],
  },
  {
    id: "routes-validate",
    service: "gateway",
    group: "Gateway Routes",
    title: "Validate route",
    method: "POST",
    path: "/api/v1/admin/routes/validate",
    auth: "basic",
    bodyFields: routeFields(),
  },
  {
    id: "routes-create",
    service: "gateway",
    group: "Gateway Routes",
    title: "Create route",
    method: "POST",
    path: "/api/v1/admin/routes",
    auth: "basic",
    bodyFields: routeFields(),
  },
  {
    id: "routes-update",
    service: "gateway",
    group: "Gateway Routes",
    title: "Update route",
    method: "PUT",
    path: "/api/v1/admin/routes/{id}",
    auth: "basic",
    pathParams: [id],
    bodyFields: routeFields(),
  },
  {
    id: "routes-enable",
    service: "gateway",
    group: "Gateway Routes",
    title: "Enable route",
    method: "POST",
    path: "/api/v1/admin/routes/{id}/enable",
    auth: "basic",
    pathParams: [id],
  },
  {
    id: "routes-disable",
    service: "gateway",
    group: "Gateway Routes",
    title: "Disable route",
    method: "POST",
    path: "/api/v1/admin/routes/{id}/disable",
    auth: "basic",
    pathParams: [id],
  },
  {
    id: "routes-delete",
    service: "gateway",
    group: "Gateway Routes",
    title: "Delete route",
    method: "DELETE",
    path: "/api/v1/admin/routes/{id}",
    auth: "basic",
    pathParams: [id],
  },
  {
    id: "routes-generation",
    service: "gateway",
    group: "Gateway Routes",
    title: "Route generation",
    method: "GET",
    path: "/api/v1/admin/routes/generation",
    auth: "basic",
  },
  {
    id: "api-clients-list",
    service: "gateway",
    group: "API Clients",
    title: "List API clients",
    method: "GET",
    path: "/api/v1/admin/api-clients",
    auth: "basic",
  },
  {
    id: "api-clients-get",
    service: "gateway",
    group: "API Clients",
    title: "Get API client",
    method: "GET",
    path: "/api/v1/admin/api-clients/{id}",
    auth: "basic",
    pathParams: [uuid],
  },
  {
    id: "api-clients-create",
    service: "gateway",
    group: "API Clients",
    title: "Create API client",
    method: "POST",
    path: "/api/v1/admin/api-clients",
    auth: "basic",
    bodyFields: [
      { name: "name", label: "Name", type: "text", required: true },
      { name: "owner", label: "Owner", type: "text", required: true },
      { name: "tenantId", label: "Tenant ID", type: "text" },
    ],
  },
  {
    id: "api-clients-update",
    service: "gateway",
    group: "API Clients",
    title: "Update API client",
    method: "PUT",
    path: "/api/v1/admin/api-clients/{id}",
    auth: "basic",
    pathParams: [uuid],
    bodyFields: [
      { name: "name", label: "Name", type: "text", required: true },
      { name: "owner", label: "Owner", type: "text", required: true },
      { name: "tenantId", label: "Tenant ID", type: "text" },
      { name: "status", label: "Status", type: "select", options: CLIENT_STATUSES, defaultValue: "ACTIVE" },
      version,
    ],
  },
  {
    id: "api-clients-disable",
    service: "gateway",
    group: "API Clients",
    title: "Disable API client",
    method: "POST",
    path: "/api/v1/admin/api-clients/{id}/disable",
    auth: "basic",
    pathParams: [uuid],
  },
  {
    id: "api-keys-list",
    service: "gateway",
    group: "API Keys",
    title: "List key metadata",
    method: "GET",
    path: "/api/v1/admin/api-clients/{id}/keys",
    auth: "basic",
    pathParams: [{ ...uuid, label: "Client ID" }],
  },
  {
    id: "api-keys-issue",
    service: "gateway",
    group: "API Keys",
    title: "Issue API key",
    method: "POST",
    path: "/api/v1/admin/api-clients/{id}/keys",
    auth: "basic",
    pathParams: [{ ...uuid, label: "Client ID" }],
    headerFields: [idempotencyKey],
    bodyFields: [
      { name: "scopes", label: "Scopes", type: "array", defaultValue: ["payments:read", "payments:write", "refunds:write"] },
      { name: "allowedRoutes", label: "Allowed Routes", type: "array", defaultValue: ["partner-payment-read", "partner-payment-create", "partner-refund-create"] },
      { name: "expiresAt", label: "Expires At", type: "datetime" },
    ],
  },
  {
    id: "api-keys-rotate",
    service: "gateway",
    group: "API Keys",
    title: "Rotate API key",
    method: "POST",
    path: "/api/v1/admin/api-keys/{id}/rotate",
    auth: "basic",
    pathParams: [{ ...uuid, label: "Key ID" }],
    headerFields: [idempotencyKey],
    bodyFields: [{ name: "expiresAt", label: "Expires At", type: "datetime" }],
  },
  {
    id: "api-keys-revoke",
    service: "gateway",
    group: "API Keys",
    title: "Revoke API key",
    method: "POST",
    path: "/api/v1/admin/api-keys/{id}/revoke",
    auth: "basic",
    pathParams: [{ ...uuid, label: "Key ID" }],
  },
  ...policyEndpoints("rate-limits", "Rate Limits", rateLimitFields()),
  ...policyEndpoints("ip-rules", "IP Rules", ipRuleFields()),
  ...policyEndpoints("risk-rules", "Risk Rules", riskRuleFields()),
  {
    id: "audit-events-search",
    service: "gateway",
    group: "Audit",
    title: "Search audit events",
    method: "GET",
    path: "/api/v1/admin/audit-events",
    auth: "basic",
    queryParams: [
      { name: "from", label: "From", type: "datetime", required: true },
      { name: "to", label: "To", type: "datetime", required: true },
      { name: "requestId", label: "Request ID", type: "text" },
      { name: "routeId", label: "Route ID", type: "text" },
      page,
      { ...size, name: "pageSize", label: "Page Size" },
    ],
  },
  {
    id: "audit-events-get",
    service: "gateway",
    group: "Audit",
    title: "Get audit event",
    method: "GET",
    path: "/api/v1/admin/audit-events/{id}",
    auth: "basic",
    pathParams: [uuid],
  },
  {
    id: "admin-actions-list",
    service: "gateway",
    group: "Audit",
    title: "List admin actions",
    method: "GET",
    path: "/api/v1/admin/admin-actions",
    auth: "basic",
    queryParams: [page, { ...size, name: "pageSize", label: "Page Size" }],
  },
  ...serviceEndpoints(),
];

export const services: { target: ServiceTarget; label: string; port: number }[] = [
  { target: "gateway", label: "Gateway", port: 8080 },
  { target: "user", label: "User Service", port: 8081 },
  { target: "order", label: "Order Service", port: 8082 },
  { target: "payment", label: "Payment Service", port: 8083 },
  { target: "notification", label: "Notification Service", port: 8084 },
];

export const seededGatewayRoutes = endpointRegistry.filter((endpoint) =>
  endpoint.tags?.includes("seeded-route"),
);

export function findEndpoint(id: string) {
  return endpointRegistry.find((endpoint) => endpoint.id === id);
}

export function endpointsForGroup(group: string) {
  return endpointRegistry.filter((endpoint) => endpoint.group === group);
}

export function endpointsForService(service: ServiceTarget) {
  return endpointRegistry.filter((endpoint) => endpoint.service === service);
}

function routeFields(): FieldConfig[] {
  return [
    { name: "id", label: "Route ID", type: "text", required: true, defaultValue: "custom-route" },
    { name: "category", label: "Category", type: "select", options: ROUTE_CATEGORIES, defaultValue: "USER", required: true },
    { name: "pathPatterns", label: "Path Patterns", type: "array", required: true, defaultValue: ["/api/v1/custom"] },
    { name: "methods", label: "Methods", type: "array", required: true, defaultValue: ["GET"] },
    { name: "targetUri", label: "Target URI", type: "text", required: true, defaultValue: "http://user-service:8081" },
    { name: "stripPrefix", label: "Strip Prefix", type: "number", defaultValue: 0 },
    { name: "rewriteRegex", label: "Rewrite Regex", type: "text" },
    { name: "rewriteReplacement", label: "Rewrite Replacement", type: "text" },
    { name: "order", label: "Order", type: "number", defaultValue: 500 },
    { name: "enabled", label: "Enabled", type: "boolean", defaultValue: true },
    { name: "authentication", label: "Authentication", type: "array", required: true, defaultValue: ["JWT"] },
    { name: "requiredRoles", label: "Required Roles", type: "array", defaultValue: [] },
    { name: "requiredScopes", label: "Required Scopes", type: "array", defaultValue: [] },
    { name: "signingRequired", label: "Signing Required", type: "boolean", defaultValue: false },
    { name: "rateLimitPolicyId", label: "Rate Policy ID", type: "text" },
    { name: "ipPolicyId", label: "IP Policy ID", type: "text" },
    { name: "riskPolicyId", label: "Risk Policy ID", type: "text" },
    { name: "connectTimeoutMs", label: "Connect Timeout", type: "number", defaultValue: 1000 },
    { name: "responseTimeoutMs", label: "Response Timeout", type: "number", defaultValue: 3000 },
    { name: "retryPolicy", label: "Retry Policy", type: "json", defaultValue: { enabled: false, maxAttempts: 1, eligibleMethods: [] } },
    { name: "circuitBreaker", label: "Circuit Breaker", type: "json", defaultValue: { enabled: true, name: "custom-service" } },
    { name: "auditMode", label: "Audit Mode", type: "text", defaultValue: "DENIALS_AND_MUTATIONS" },
    version,
  ];
}

function rateLimitFields(): FieldConfig[] {
  return [
    { name: "id", label: "Policy ID", type: "text", required: true, defaultValue: "custom-rate" },
    { name: "subjectType", label: "Subject Type", type: "select", options: RATE_SUBJECT_TYPES, defaultValue: "SUBJECT" },
    { name: "routeId", label: "Route ID", type: "text" },
    { name: "method", label: "Method", type: "select", options: HTTP_METHODS },
    { name: "capacity", label: "Capacity", type: "number", required: true, defaultValue: 10 },
    { name: "refillTokens", label: "Refill Tokens", type: "number", required: true, defaultValue: 10 },
    { name: "refillPeriodSeconds", label: "Refill Period Seconds", type: "number", required: true, defaultValue: 60 },
    { name: "priority", label: "Priority", type: "number", defaultValue: 0 },
    { name: "redisOutageMode", label: "Redis Outage Mode", type: "select", options: REDIS_OUTAGE_MODES, defaultValue: "DENY" },
    { name: "responseHeadersEnabled", label: "Response Headers", type: "boolean", defaultValue: true },
    { name: "enabled", label: "Enabled", type: "boolean", defaultValue: true },
    version,
  ];
}

function ipRuleFields(): FieldConfig[] {
  return [
    { name: "id", label: "Rule ID", type: "text", required: true, defaultValue: "custom-ip-rule" },
    { name: "network", label: "Network", type: "text", required: true, defaultValue: "127.0.0.1/32" },
    { name: "action", label: "Action", type: "select", options: IP_ACTIONS, defaultValue: "ALLOW" },
    { name: "routeId", label: "Route ID", type: "text" },
    { name: "priority", label: "Priority", type: "number", defaultValue: 0 },
    { name: "reason", label: "Reason", type: "text", required: true, defaultValue: "console" },
    { name: "validFrom", label: "Valid From", type: "datetime", required: true },
    { name: "expiresAt", label: "Expires At", type: "datetime" },
    { name: "enabled", label: "Enabled", type: "boolean", defaultValue: true },
    version,
  ];
}

function riskRuleFields(): FieldConfig[] {
  return [
    { name: "id", label: "Rule ID", type: "text", required: true, defaultValue: "custom-risk-rule" },
    { name: "signal", label: "Signal", type: "select", options: RISK_SIGNALS, defaultValue: "HEADER_COUNT" },
    { name: "thresholdValue", label: "Threshold", type: "number", defaultValue: 20 },
    { name: "weight", label: "Weight", type: "number", defaultValue: 10 },
    { name: "action", label: "Action", type: "select", options: RISK_ACTIONS, defaultValue: "OBSERVE" },
    { name: "routeId", label: "Route ID", type: "text" },
    { name: "enabled", label: "Enabled", type: "boolean", defaultValue: true },
    version,
  ];
}

function policyEndpoints(
  path: "rate-limits" | "ip-rules" | "risk-rules",
  group: string,
  fields: FieldConfig[],
): EndpointDefinition[] {
  return [
    {
      id: `${path}-list`,
      service: "gateway",
      group,
      title: `List ${group.toLowerCase()}`,
      method: "GET",
      path: `/api/v1/admin/${path}`,
      auth: "basic",
    },
    {
      id: `${path}-get`,
      service: "gateway",
      group,
      title: `Get ${group.slice(0, -1).toLowerCase()}`,
      method: "GET",
      path: `/api/v1/admin/${path}/{id}`,
      auth: "basic",
      pathParams: [id],
    },
    {
      id: `${path}-create`,
      service: "gateway",
      group,
      title: `Create ${group.slice(0, -1).toLowerCase()}`,
      method: "POST",
      path: `/api/v1/admin/${path}`,
      auth: "basic",
      bodyFields: fields,
    },
    {
      id: `${path}-update`,
      service: "gateway",
      group,
      title: `Update ${group.slice(0, -1).toLowerCase()}`,
      method: "PUT",
      path: `/api/v1/admin/${path}/{id}`,
      auth: "basic",
      pathParams: [id],
      bodyFields: fields,
    },
    {
      id: `${path}-delete`,
      service: "gateway",
      group,
      title: `Delete ${group.slice(0, -1).toLowerCase()}`,
      method: "DELETE",
      path: `/api/v1/admin/${path}/{id}`,
      auth: "basic",
      pathParams: [id],
    },
  ];
}

function serviceEndpoints(): EndpointDefinition[] {
  return [
    {
      id: "user-public-profile",
      service: "gateway",
      group: "User Service",
      title: "Public profile",
      method: "GET",
      path: "/api/v1/public/users/{id}",
      auth: "none",
      pathParams: [uuid],
      tags: ["seeded-route"],
    },
    {
      id: "user-profile-read",
      service: "gateway",
      group: "User Service",
      title: "Current profile",
      method: "GET",
      path: "/api/v1/users/me",
      auth: "jwt",
      tags: ["seeded-route"],
    },
    {
      id: "user-profile-create",
      service: "gateway",
      group: "User Service",
      title: "Create current profile",
      method: "POST",
      path: "/api/v1/users/me",
      auth: "jwt",
      tags: ["seeded-route"],
      bodyFields: profileFields(false),
    },
    {
      id: "user-profile-update",
      service: "gateway",
      group: "User Service",
      title: "Update current profile",
      method: "PATCH",
      path: "/api/v1/users/me",
      auth: "jwt",
      tags: ["seeded-route"],
      bodyFields: [...profileFields(true), version],
    },
    {
      id: "admin-users-list",
      service: "gateway",
      group: "User Service",
      title: "Admin users",
      method: "GET",
      path: "/api/v1/backoffice/users",
      auth: "jwt",
      tags: ["seeded-route"],
      queryParams: [
        { name: "status", label: "Status", type: "select", options: ["ACTIVE", "DISABLED", "DELETED"] },
        { name: "query", label: "Query", type: "text" },
        page,
        size,
      ],
    },
    lifecycleEndpoint("admin-users-enable", "Enable user", "/api/v1/backoffice/users/{id}/enable", "User Service"),
    lifecycleEndpoint("admin-users-disable", "Disable user", "/api/v1/backoffice/users/{id}/disable", "User Service"),
    {
      id: "admin-users-delete",
      service: "gateway",
      group: "User Service",
      title: "Delete user",
      method: "DELETE",
      path: "/api/v1/backoffice/users/{id}",
      auth: "jwt",
      pathParams: [uuid],
      bodyFields: [version],
      tags: ["seeded-route"],
    },
    {
      id: "orders-list",
      service: "gateway",
      group: "Order Service",
      title: "List orders",
      method: "GET",
      path: "/api/v1/orders",
      auth: "jwt",
      queryParams: [{ name: "status", label: "Status", type: "select", options: ORDER_STATUSES }, page, size],
      tags: ["seeded-route"],
    },
    {
      id: "orders-get",
      service: "gateway",
      group: "Order Service",
      title: "Get order",
      method: "GET",
      path: "/api/v1/orders/{id}",
      auth: "jwt",
      pathParams: [uuid],
      tags: ["seeded-route"],
    },
    {
      id: "orders-create",
      service: "gateway",
      group: "Order Service",
      title: "Create order",
      method: "POST",
      path: "/api/v1/orders",
      auth: "jwt",
      headerFields: [{ ...idempotencyKey, required: false }],
      defaultBody: { items: [{ sku: "BOOK-JAVA-25", quantity: 1 }] },
      tags: ["seeded-route"],
    },
    lifecycleEndpoint("orders-cancel", "Cancel order", "/api/v1/orders/{id}/cancel", "Order Service"),
    {
      id: "admin-orders-list",
      service: "gateway",
      group: "Order Service",
      title: "Admin orders",
      method: "GET",
      path: "/api/v1/backoffice/orders",
      auth: "jwt",
      queryParams: [
        { name: "status", label: "Status", type: "select", options: ORDER_STATUSES },
        { name: "tenantId", label: "Tenant ID", type: "text" },
        { name: "subject", label: "Subject", type: "text" },
        page,
        size,
      ],
      tags: ["seeded-route"],
    },
    {
      id: "admin-orders-update",
      service: "gateway",
      group: "Order Service",
      title: "Update order lifecycle",
      method: "PATCH",
      path: "/api/v1/backoffice/orders/{id}",
      auth: "jwt",
      pathParams: [uuid],
      bodyFields: [
        version,
        { name: "status", label: "Order Status", type: "select", options: ORDER_STATUSES },
        { name: "paymentStatus", label: "Payment Status", type: "select", options: ORDER_PAYMENT_STATUSES },
        { name: "fulfillmentStatus", label: "Fulfillment Status", type: "select", options: FULFILLMENT_STATUSES },
      ],
      tags: ["seeded-route"],
    },
    {
      id: "partner-payment-read",
      service: "gateway",
      group: "Payment Service",
      title: "Get partner payment",
      method: "GET",
      path: "/api/v1/partner/payments/{id}",
      auth: "apiKey",
      pathParams: [uuid],
      tags: ["seeded-route"],
    },
    {
      id: "partner-payment-create",
      service: "gateway",
      group: "Payment Service",
      title: "Create partner payment",
      method: "POST",
      path: "/api/v1/partner/payments",
      auth: "apiKey",
      signingRequired: true,
      headerFields: [idempotencyKey],
      bodyFields: [
        { name: "merchantReference", label: "Merchant Reference", type: "text", required: true, defaultValue: "acme-order-1002" },
        { name: "amount", label: "Amount", type: "text", required: true, defaultValue: "125.50" },
        { name: "currency", label: "Currency", type: "text", required: true, defaultValue: "USD" },
        { name: "description", label: "Description", type: "text", defaultValue: "Security gateway lab payment" },
      ],
      tags: ["seeded-route"],
    },
    {
      id: "partner-refund-create",
      service: "gateway",
      group: "Payment Service",
      title: "Create partner refund",
      method: "POST",
      path: "/api/v1/partner/refunds",
      auth: "apiKey",
      signingRequired: true,
      headerFields: [idempotencyKey],
      bodyFields: [
        { name: "paymentId", label: "Payment ID", type: "text", required: true },
        { name: "merchantReference", label: "Merchant Reference", type: "text", defaultValue: "acme-refund-1002" },
        { name: "amount", label: "Amount", type: "text", required: true, defaultValue: "25.00" },
      ],
      tags: ["seeded-route"],
    },
    {
      id: "notifications-list",
      service: "gateway",
      group: "Notification Service",
      title: "List notifications",
      method: "GET",
      path: "/api/v1/notifications",
      auth: "jwt",
      queryParams: [
        page,
        size,
        { name: "channel", label: "Channel", type: "select", options: NOTIFICATION_CHANNELS },
        { name: "status", label: "Status", type: "select", options: NOTIFICATION_STATUSES },
      ],
      tags: ["seeded-route"],
    },
    {
      id: "notification-preferences-update",
      service: "gateway",
      group: "Notification Service",
      title: "Update preferences",
      method: "POST",
      path: "/api/v1/notifications/preferences",
      auth: "jwt",
      bodyFields: [
        { name: "emailEnabled", label: "Email", type: "boolean", defaultValue: true, required: true },
        { name: "smsEnabled", label: "SMS", type: "boolean", defaultValue: false, required: true },
        { name: "pushEnabled", label: "Push", type: "boolean", defaultValue: true, required: true },
        { name: "webhookEnabled", label: "Webhook", type: "boolean", defaultValue: false, required: true },
        { name: "version", label: "Version", type: "number", defaultValue: 0, required: true },
      ],
      tags: ["seeded-route"],
    },
    {
      id: "admin-test-notification",
      service: "gateway",
      group: "Notification Service",
      title: "Admin notification test",
      method: "POST",
      path: "/api/v1/admin/test-notification",
      auth: "jwt",
      bodyFields: [
        { name: "scenario", label: "Scenario", type: "select", options: TEST_SCENARIOS, defaultValue: "SUCCESS", required: true },
        { name: "channel", label: "Channel", type: "select", options: NOTIFICATION_CHANNELS, defaultValue: "EMAIL", required: true },
        { name: "recipientReference", label: "Recipient", type: "text", defaultValue: "test-recipient", required: true },
        { name: "message", label: "Message", type: "textarea", defaultValue: "Gateway resilience smoke test", required: true },
      ],
      tags: ["seeded-route"],
    },
  ];
}

function profileFields(patch: boolean): FieldConfig[] {
  return [
    { name: "displayName", label: "Display Name", type: "text", required: !patch, defaultValue: "Omar Hassan" },
    { name: "bio", label: "Bio", type: "textarea", defaultValue: "Backend engineer" },
    { name: "avatarUrl", label: "Avatar URL", type: "text" },
    { name: "email", label: "Email", type: "text", required: !patch, defaultValue: "omar@example.test" },
    { name: "locale", label: "Locale", type: "text", required: !patch, defaultValue: "en-EG" },
    { name: "timezone", label: "Timezone", type: "text", required: !patch, defaultValue: "Africa/Cairo" },
  ];
}

function lifecycleEndpoint(
  idValue: string,
  title: string,
  path: string,
  group: string,
): EndpointDefinition {
  return {
    id: idValue,
    service: "gateway",
    group,
    title,
    method: "POST",
    path,
    auth: "jwt",
    pathParams: [uuid],
    bodyFields: [version],
    tags: ["seeded-route"],
  };
}
