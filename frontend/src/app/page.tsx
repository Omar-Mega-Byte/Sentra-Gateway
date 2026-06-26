"use client";

import { useQuery } from "@tanstack/react-query";
import { Activity, AlertTriangle, Route, ShieldCheck } from "lucide-react";
import { DataTable } from "@/components/ui/data-table";
import { Card, EmptyState, PageSection, StatusPill } from "@/components/ui/primitives";
import { JsonPanel } from "@/components/ui/json-panel";
import { services } from "@/lib/backend-contract";
import { sendProxyRequest, type ServiceTarget } from "@/lib/proxy-client";

type HealthRow = {
  target: ServiceTarget;
  label: string;
  status: number;
  body: unknown;
  durationMs: number;
  ok: boolean;
};

type AuditEvent = {
  id: string;
  eventTime: string;
  eventType: string;
  decision: string;
  reasonCode: string;
  routeId?: string;
  method?: string;
  path?: string;
  status: number;
  latencyMs: number;
};

export default function DashboardPage() {
  const health = useQuery({
    queryKey: ["dashboard-health"],
    queryFn: async () => {
      const rows = await Promise.all(
        services.map(async (service) => {
          const response = await sendProxyRequest(
            {
              target: service.target,
              method: "GET",
              path: "/actuator/health",
            },
            "none",
          );
          return {
            target: service.target,
            label: service.label,
            status: response.status,
            body: response.body,
            durationMs: response.durationMs,
            ok: response.ok,
          } satisfies HealthRow;
        }),
      );
      return rows;
    },
  });

  const routes = useQuery({
    queryKey: ["dashboard-routes"],
    queryFn: async () => {
      const response = await sendProxyRequest(
        { target: "gateway", method: "GET", path: "/api/v1/admin/routes" },
        "basic",
      );
      if (!response.ok) throw new Error(`${response.status} ${response.statusText}`);
      return Array.isArray(response.body) ? response.body : [];
    },
  });

  const audit = useQuery({
    queryKey: ["dashboard-audit"],
    queryFn: async () => {
      const to = new Date();
      const from = new Date(to.getTime() - 24 * 60 * 60 * 1000);
      const response = await sendProxyRequest(
        {
          target: "gateway",
          method: "GET",
          path: "/api/v1/admin/audit-events",
          query: {
            from: from.toISOString(),
            to: to.toISOString(),
            page: 0,
            pageSize: 10,
          },
        },
        "basic",
      );
      if (!response.ok) throw new Error(`${response.status} ${response.statusText}`);
      return Array.isArray(response.body) ? (response.body as AuditEvent[]) : [];
    },
  });

  const activeRoutes = routes.data?.filter((route) => isRecord(route) && route.enabled).length ?? 0;
  const signedRoutes = routes.data?.filter((route) => isRecord(route) && route.signingRequired).length ?? 0;
  const deniedEvents = audit.data?.filter((event) => event.decision !== "ALLOW").length ?? 0;

  return (
    <div className="space-y-6">
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          title="Gateway Health"
          value={health.data?.find((row) => row.target === "gateway")?.ok ? "UP" : "CHECK"}
          icon={<Activity className="h-5 w-5" />}
          tone={health.data?.find((row) => row.target === "gateway")?.ok ? "ok" : "warn"}
        />
        <MetricCard
          title="Active Routes"
          value={routes.error ? "AUTH" : `${activeRoutes}`}
          icon={<Route className="h-5 w-5" />}
          tone={routes.error ? "warn" : "ok"}
        />
        <MetricCard
          title="Signed Routes"
          value={routes.error ? "AUTH" : `${signedRoutes}`}
          icon={<ShieldCheck className="h-5 w-5" />}
        />
        <MetricCard
          title="Recent Denials"
          value={audit.error ? "AUTH" : `${deniedEvents}`}
          icon={<AlertTriangle className="h-5 w-5" />}
          tone={deniedEvents > 0 ? "warn" : "neutral"}
        />
      </div>

      <PageSection title="Service Health">
        <DataTable
          rows={health.data ?? []}
          columns={[
            { key: "service", header: "Service", render: (row) => row.label },
            {
              key: "status",
              header: "Status",
              render: (row) => (
                <StatusPill tone={row.ok ? "ok" : "bad"}>{row.status}</StatusPill>
              ),
            },
            { key: "time", header: "Time", render: (row) => `${row.durationMs} ms` },
            { key: "body", header: "Body", render: (row) => healthStatus(row.body) },
          ]}
        />
      </PageSection>

      <div className="grid gap-6 xl:grid-cols-2">
        <PageSection title="Recent Audit Events">
          {audit.error ? (
            <EmptyState>{audit.error.message}</EmptyState>
          ) : (
            <DataTable
              rows={audit.data ?? []}
              columns={[
                { key: "time", header: "Time", render: (row) => shortDate(row.eventTime) },
                { key: "route", header: "Route", render: (row) => row.routeId ?? "" },
                { key: "decision", header: "Decision", render: (row) => row.decision },
                { key: "status", header: "Status", render: (row) => row.status },
              ]}
            />
          )}
        </PageSection>

        <PageSection title="Gateway Metrics Preview">
          <MetricsPreview />
        </PageSection>
      </div>
    </div>
  );
}

function MetricCard({
  title,
  value,
  icon,
  tone = "neutral",
}: {
  title: string;
  value: string;
  icon: React.ReactNode;
  tone?: "neutral" | "ok" | "warn";
}) {
  const colors = {
    neutral: "text-slate-600",
    ok: "text-emerald-700",
    warn: "text-amber-700",
  };
  return (
    <Card className="p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm font-medium text-slate-500">{title}</p>
          <p className="mt-2 text-2xl font-semibold text-slate-950">{value}</p>
        </div>
        <div className={`rounded-md border border-slate-200 bg-slate-50 p-2 ${colors[tone]}`}>
          {icon}
        </div>
      </div>
    </Card>
  );
}

function MetricsPreview() {
  const metrics = useQuery({
    queryKey: ["dashboard-metrics"],
    queryFn: async () =>
      sendProxyRequest(
        { target: "gateway", method: "GET", path: "/actuator/metrics" },
        "basic",
      ),
  });

  if (metrics.error) return <EmptyState>{metrics.error.message}</EmptyState>;
  if (!metrics.data) return <EmptyState>Loading</EmptyState>;
  return (
    <JsonPanel
      title={`${metrics.data.status} ${metrics.data.statusText}`}
      value={metrics.data.body}
      dense
    />
  );
}

function healthStatus(body: unknown) {
  if (isRecord(body) && typeof body.status === "string") {
    return body.status;
  }
  return "";
}

function shortDate(value: string) {
  return value ? new Date(value).toLocaleString() : "";
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
