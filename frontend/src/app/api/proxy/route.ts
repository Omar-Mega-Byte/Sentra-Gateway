import { NextRequest, NextResponse } from "next/server";
import type { ProxyRequest, ServiceTarget } from "@/lib/proxy-client";

const defaultBases: Record<ServiceTarget, string> = {
  gateway: "http://localhost:8080",
  user: "http://localhost:8081",
  order: "http://localhost:8082",
  payment: "http://localhost:8083",
  notification: "http://localhost:8084",
};

const envNames: Record<ServiceTarget, string> = {
  gateway: "GATEWAY_BASE_URL",
  user: "USER_SERVICE_BASE_URL",
  order: "ORDER_SERVICE_BASE_URL",
  payment: "PAYMENT_SERVICE_BASE_URL",
  notification: "NOTIFICATION_SERVICE_BASE_URL",
};

export async function POST(request: NextRequest) {
  const startedAt = performance.now();
  try {
    const payload = (await request.json()) as ProxyRequest;
    const base = baseUrl(payload.target);
    const url = buildUrl(base, payload.path, payload.query);
    const headers = new Headers(payload.headers ?? {});
    const method = payload.method.toUpperCase();
    const body =
      method === "GET" || method === "HEAD"
        ? undefined
        : payload.rawBody !== undefined
          ? payload.rawBody
          : payload.body !== undefined
            ? JSON.stringify(payload.body)
            : undefined;

    const upstream = await fetch(url, {
      method,
      headers,
      body,
      cache: "no-store",
    });
    const rawBody = await upstream.text();
    const contentType = upstream.headers.get("content-type") ?? "";
    const responseHeaders = Object.fromEntries(upstream.headers.entries());
    const responseBody = contentType.includes("application/json")
      ? safeJson(rawBody)
      : rawBody;

    return NextResponse.json({
      ok: upstream.ok,
      status: upstream.status,
      statusText: upstream.statusText,
      headers: responseHeaders,
      body: responseBody,
      rawBody,
      durationMs: Math.round(performance.now() - startedAt),
      url,
    });
  } catch (error) {
    return NextResponse.json(
      {
        ok: false,
        status: 599,
        statusText: "Proxy Error",
        headers: {},
        body: {
          code: "FRONTEND_PROXY_ERROR",
          message: error instanceof Error ? error.message : "Proxy failed",
        },
        rawBody: "",
        durationMs: Math.round(performance.now() - startedAt),
        url: "",
      },
      { status: 200 },
    );
  }
}

function baseUrl(target: ServiceTarget) {
  if (!Object.hasOwn(defaultBases, target)) {
    throw new Error(`Unsupported target: ${target}`);
  }
  return process.env[envNames[target]] ?? defaultBases[target];
}

function buildUrl(
  base: string,
  path: string,
  query: ProxyRequest["query"] = {},
) {
  if (!path.startsWith("/")) {
    throw new Error("Path must start with /");
  }
  const url = new URL(path, base);
  Object.entries(query ?? {}).forEach(([name, value]) => {
    if (value !== undefined && value !== null && `${value}`.trim() !== "") {
      url.searchParams.set(name, `${value}`);
    }
  });
  return url.toString();
}

function safeJson(rawBody: string) {
  try {
    return rawBody ? JSON.parse(rawBody) : null;
  } catch {
    return rawBody;
  }
}
