import type { AuthState } from "@/lib/auth-store";
import type { EndpointDefinition, FieldConfig } from "@/lib/backend-contract";
import type { ServiceTarget } from "@/lib/proxy-client";

export const testValues = {
  tenantId: "tenant-demo",
  subject: "sentra-user-omar",
  profile: {
    activeId: "7aa99db8-a943-4b63-b4b7-79f769ef9f87",
    disabledId: "11111111-1111-4111-8111-111111111111",
    deletedId: "22222222-2222-4222-8222-222222222222",
    version: 3,
  },
  orders: {
    completedId: "10000000-0000-4000-8000-000000000001",
    createdId: "10000000-0000-4000-8000-000000000002",
    foreignSubjectId: "20000000-0000-4000-8000-000000000001",
    foreignTenantId: "30000000-0000-4000-8000-000000000001",
    createdVersion: 1,
    completedVersion: 2,
  },
  payments: {
    capturedId: "40000000-0000-4000-8000-000000000001",
    declinedId: "40000000-0000-4000-8000-000000000002",
    foreignId: "50000000-0000-4000-8000-000000000001",
    refundId: "60000000-0000-4000-8000-000000000001",
  },
  notifications: {
    sentId: "70000000-0000-4000-8000-000000000001",
    queuedId: "70000000-0000-4000-8000-000000000002",
    preferenceVersion: 2,
  },
  gateway: {
    routeId: "user-profile-read",
    customRouteId: "ui-public-profile-copy",
    ratePolicyId: "ui-user-profile-read-rate",
    ipAllowRuleId: "ui-localhost-allow",
    ipBlockRuleId: "ui-localhost-block",
    riskObserveRuleId: "ui-header-count-observe",
    riskDenyRuleId: "ui-query-count-deny",
  },
  client: {
    name: "partner-acme-ui",
    owner: "QA Console",
  },
} as const;

export const localAdminAuth: AuthState = {
  authMode: "basic",
  jwt: "",
  apiKey: "",
  apiKeyId: "",
  basicUsername: "admin",
  basicPassword: "sentra-admin",
  requestIdPrefix: "ui-smoke",
};

export const sampleJwtPreview =
  "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzZW50cmEtdXNlci1vbWFyIiwidGVuYW50X2lkIjoidGVuYW50LWRlbW8iLCJzY29wZSI6InByb2ZpbGU6cmVhZCBwcm9maWxlOndyaXRlIG9yZGVyczpyZWFkIG9yZGVyczp3cml0ZSBub3RpZmljYXRpb25zOnJlYWQgbm90aWZpY2F0aW9uczp3cml0ZSIsInJvbGVzIjpbIlVTRVJfQURNSU4iLCJPUkRFUl9BRE1JTiIsIk5PVElGSUNBVElPTl9BRE1JTiJdfQ.signature-from-your-issuer";

export function sampleAuthForJwtPreview(): AuthState {
  return {
    ...localAdminAuth,
    authMode: "jwt",
    jwt: sampleJwtPreview,
  };
}

export function samplePlaceholder(field: FieldConfig, endpoint?: EndpointDefinition) {
  const value = sampleFieldValue(field, endpoint);
  if (value === undefined || value === null || value === "") {
    return field.placeholder ?? sampleByType(field);
  }
  if (Array.isArray(value)) return value.join(", ");
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}

export function sampleHelper(field: FieldConfig, endpoint?: EndpointDefinition) {
  if (field.helperText) return field.helperText;
  const sample = samplePlaceholder(field, endpoint);
  return sample ? `Example: ${sample}` : "Use a backend-valid value for this field.";
}

export function sampleFieldValue(field: FieldConfig, endpoint?: EndpointDefinition): unknown {
  if (field.defaultValue !== undefined) return field.defaultValue;

  const endpointId = endpoint?.id ?? "";
  const name = field.name;

  if (name === "id") return sampleIdForEndpoint(endpointId);
  if (name === "requestId") return "ui-smoke-request";
  if (name === "routeId") return sampleRouteForEndpoint(endpointId);
  if (name === "tenantId") return testValues.tenantId;
  if (name === "subject") return testValues.subject;
  if (name === "query") return "Omar";
  if (name === "from") return toLocalDatetime(new Date(Date.now() - 24 * 60 * 60 * 1000));
  if (name === "to") return toLocalDatetime(new Date());
  if (name === "expiresAt") return toLocalDatetime(new Date("2026-12-31T23:59:59Z"));
  if (name === "validFrom") return toLocalDatetime(new Date("2026-06-17T00:00:00Z"));
  if (name === "network") return "127.0.0.1/32";
  if (name === "reason") return "UI smoke test";
  if (name === "paymentId") return testValues.payments.capturedId;
  if (name === "merchantReference") return endpointId.includes("refund") ? "ui-acme-refund-1002" : "ui-acme-order-1002";
  if (name === "amount") return endpointId.includes("refund") ? "25.00" : "125.50";
  if (name === "currency") return "USD";
  if (name === "description") return "Security gateway UI payment test";
  if (name === "displayName") return "Omar H.";
  if (name === "bio") return "Building secure Java services";
  if (name === "avatarUrl") return "https://cdn.example.test/avatars/7aa99db8.png";
  if (name === "email") return "omar.hassan.ui@example.test";
  if (name === "locale") return "en-EG";
  if (name === "timezone") return "Africa/Cairo";
  if (name === "owner") return testValues.client.owner;
  if (name === "name") return testValues.client.name;
  if (name === "version") return sampleVersionForEndpoint(endpointId);
  if (name === "page") return 0;
  if (name === "pageSize" || name === "size") return 20;
  if (name === "Idempotency-Key") return sampleIdempotencyKey(endpointId);

  return undefined;
}

export function sampleBody(endpoint?: EndpointDefinition) {
  if (!endpoint) return "";
  if (endpoint.defaultBody !== undefined) return endpoint.defaultBody;
  switch (endpoint.id) {
    case "orders-create":
      return { items: [{ sku: "SECURE-GATEWAY-LAB", quantity: 1 }] };
    case "partner-payment-create":
      return {
        merchantReference: "ui-acme-order-1002",
        amount: "125.50",
        currency: "USD",
        description: "Security gateway UI payment test",
      };
    case "partner-refund-create":
      return {
        paymentId: testValues.payments.capturedId,
        merchantReference: "ui-acme-refund-1002",
        amount: "25.00",
      };
    default:
      return "";
  }
}

export function sampleExplorerRequest(endpoint?: EndpointDefinition) {
  const path = interpolateEndpointPath(endpoint);
  return {
    target: endpoint?.service ?? ("gateway" as ServiceTarget),
    method: endpoint?.method ?? "GET",
    path,
    auth: endpoint?.auth ?? ("none" as const),
    query: JSON.stringify(sampleQuery(endpoint), null, 2),
    headers: JSON.stringify(sampleHeaders(endpoint), null, 2),
    body: bodyText(endpoint),
  };
}

export function sampleLoadTest() {
  return {
    path: "/api/v1/public/users/7aa99db8-a943-4b63-b4b7-79f769ef9f87",
    method: "GET",
    authMode: "none" as const,
    count: 3,
  };
}

export function sampleAuditSearch() {
  return {
    from: toLocalDatetime(new Date(Date.now() - 24 * 60 * 60 * 1000)),
    to: toLocalDatetime(new Date()),
    requestId: "",
    routeId: "user-profile-read",
    subject: "sentra-user-omar",
    status: "",
    decision: "",
    page: 0,
    pageSize: 20,
  };
}

export function sampleSigning() {
  return {
    method: "POST" as const,
    path: "/api/v1/partner/payments",
    body: JSON.stringify(sampleBody({ id: "partner-payment-create" } as EndpointDefinition), null, 2),
    idempotencyKey: "ui-payment-create-001",
  };
}

export function toLocalDatetime(date: Date) {
  const offset = date.getTimezoneOffset();
  return new Date(date.getTime() - offset * 60_000).toISOString().slice(0, 16);
}

function sampleQuery(endpoint?: EndpointDefinition) {
  const values: Record<string, unknown> = {};
  (endpoint?.queryParams ?? []).forEach((field) => {
    const value = sampleFieldValue(field, endpoint);
    if (value !== undefined && value !== "") values[field.name] = value;
  });
  return values;
}

function sampleHeaders(endpoint?: EndpointDefinition) {
  const values: Record<string, unknown> = {};
  (endpoint?.headerFields ?? []).forEach((field) => {
    const value = sampleFieldValue(field, endpoint);
    if (value !== undefined && value !== "") values[field.name] = value;
  });
  return values;
}

function bodyText(endpoint?: EndpointDefinition) {
  if (!endpoint) return "";
  if (endpoint.bodyFields?.length) {
    const values: Record<string, unknown> = {};
    endpoint.bodyFields.forEach((field) => {
      const value = sampleFieldValue(field, endpoint);
      if (value !== undefined && value !== "") values[field.name] = value;
    });
    return JSON.stringify(values, null, 2);
  }
  const body = sampleBody(endpoint);
  return body ? JSON.stringify(body, null, 2) : "";
}

function interpolateEndpointPath(endpoint?: EndpointDefinition) {
  if (!endpoint) return "/actuator/health";
  return endpoint.path.replace(/\{([^}]+)\}/g, () =>
    encodeURIComponent(String(sampleIdForEndpoint(endpoint.id))),
  );
}

function sampleIdForEndpoint(endpointId: string) {
  if (endpointId.includes("user-public-profile")) return testValues.profile.activeId;
  if (endpointId.includes("admin-users")) return testValues.profile.disabledId;
  if (endpointId.includes("orders-cancel") || endpointId.includes("admin-orders-update")) return testValues.orders.createdId;
  if (endpointId.includes("orders")) return testValues.orders.completedId;
  if (endpointId.includes("partner-payment")) return testValues.payments.capturedId;
  if (endpointId.includes("api-clients")) return "paste-created-client-uuid";
  if (endpointId.includes("api-keys")) return "paste-issued-key-uuid";
  if (endpointId.includes("rate-limits")) return testValues.gateway.ratePolicyId;
  if (endpointId.includes("ip-rules")) return testValues.gateway.ipAllowRuleId;
  if (endpointId.includes("risk-rules")) return testValues.gateway.riskObserveRuleId;
  if (endpointId.includes("routes")) return testValues.gateway.routeId;
  if (endpointId.includes("audit-events")) return "paste-audit-event-uuid";
  return "user-profile-read";
}

function sampleRouteForEndpoint(endpointId: string) {
  if (endpointId.includes("rate")) return "user-profile-read";
  if (endpointId.includes("ip") || endpointId.includes("risk")) return "user-public-profile";
  return "user-profile-read";
}

function sampleVersionForEndpoint(endpointId: string) {
  if (endpointId.includes("user-profile-update")) return testValues.profile.version;
  if (endpointId.includes("admin-users")) return 1;
  if (endpointId.includes("orders-cancel") || endpointId.includes("admin-orders-update")) return 1;
  if (endpointId.includes("notification-preferences")) return testValues.notifications.preferenceVersion;
  return 0;
}

function sampleIdempotencyKey(endpointId: string) {
  if (endpointId.includes("payment")) return "ui-payment-create-001";
  if (endpointId.includes("refund")) return "ui-refund-create-001";
  if (endpointId.includes("api-keys")) return "ui-key-issue-001";
  if (endpointId.includes("orders")) return "ui-order-create-001";
  return "ui-idempotency-001";
}

function sampleByType(field: FieldConfig) {
  if (field.type === "number") return "0";
  if (field.type === "datetime") return toLocalDatetime(new Date());
  if (field.type === "array") return "value-one, value-two";
  if (field.type === "json") return "{}";
  if (field.type === "boolean") return "true";
  return "test-value";
}
