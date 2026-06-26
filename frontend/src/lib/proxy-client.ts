import type { AuthMode } from "@/lib/auth-store";
import { buildAuthHeaders, currentAuthState, requestId } from "@/lib/auth-store";

export type ServiceTarget =
  | "gateway"
  | "user"
  | "order"
  | "payment"
  | "notification";

export type ProxyRequest = {
  target: ServiceTarget;
  method: string;
  path: string;
  query?: Record<string, string | number | boolean | null | undefined>;
  headers?: Record<string, string>;
  body?: unknown;
  rawBody?: string;
};

export type ProxyResponse = {
  ok: boolean;
  status: number;
  statusText: string;
  headers: Record<string, string>;
  body: unknown;
  rawBody: string;
  durationMs: number;
  url: string;
};

export async function sendProxyRequest(
  request: ProxyRequest,
  authMode: AuthMode = currentAuthState().authMode,
): Promise<ProxyResponse> {
  const auth = currentAuthState();
  const headers: Record<string, string> = {
    "X-Request-Id": requestId(auth.requestIdPrefix),
    ...buildAuthHeaders(authMode, auth),
    ...request.headers,
  };

  if (request.body !== undefined || request.rawBody !== undefined) {
    headers["Content-Type"] = headers["Content-Type"] ?? "application/json";
  }

  const response = await fetch("/api/proxy", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      ...request,
      headers,
    }),
  });

  const payload = (await response.json()) as ProxyResponse;
  return payload;
}

export function compactObject(values: Record<string, unknown>) {
  return Object.fromEntries(
    Object.entries(values).filter(([, value]) => {
      if (value === undefined || value === null) return false;
      if (typeof value === "string" && value.trim() === "") return false;
      return true;
    }),
  );
}
