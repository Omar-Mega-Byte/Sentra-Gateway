"use client";

import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { JsonPanel } from "@/components/ui/json-panel";
import { Button, Card, CardHeader, EmptyState, Label, PageSection, Select, StatusPill } from "@/components/ui/primitives";
import { DataTable } from "@/components/ui/data-table";
import { services } from "@/lib/backend-contract";
import { sendProxyRequest, type ServiceTarget } from "@/lib/proxy-client";

type Probe = "health" | "metrics" | "prometheus" | "openapi";

const paths: Record<Probe, string> = {
  health: "/actuator/health",
  metrics: "/actuator/metrics",
  prometheus: "/actuator/prometheus",
  openapi: "/v3/api-docs",
};

export default function ObservabilityPage() {
  const [target, setTarget] = useState<ServiceTarget>("gateway");
  const [probe, setProbe] = useState<Probe>("health");

  const health = useQuery({
    queryKey: ["observability-health"],
    queryFn: async () => {
      const rows = await Promise.all(
        services.map(async (service) => {
          const response = await sendProxyRequest(
            { target: service.target, method: "GET", path: "/actuator/health" },
            "none",
          );
          return {
            service: service.label,
            target: service.target,
            status: response.status,
            ok: response.ok,
            time: response.durationMs,
            body: response.body,
          };
        }),
      );
      return rows;
    },
  });

  const preview = useQuery({
    queryKey: ["observability-preview", target, probe],
    queryFn: () =>
      sendProxyRequest(
        { target, method: "GET", path: paths[probe] },
        target === "gateway" && probe !== "health" ? "basic" : "none",
      ),
  });

  return (
    <div className="space-y-6">
      <PageSection title="Service Status">
        <DataTable
          rows={health.data ?? []}
          columns={[
            { key: "service", header: "Service", render: (row) => row.service },
            {
              key: "status",
              header: "Status",
              render: (row) => <StatusPill tone={row.ok ? "ok" : "bad"}>{row.status}</StatusPill>,
            },
            { key: "time", header: "Time", render: (row) => `${row.time} ms` },
            {
              key: "reported",
              header: "Reported",
              render: (row) => (isRecord(row.body) ? String(row.body.status ?? "") : ""),
            },
          ]}
        />
      </PageSection>

      <PageSection title="Endpoint Preview">
        <Card>
          <CardHeader
            title="Probe"
            meta={preview.data ? <StatusPill>{preview.data.status}</StatusPill> : null}
            action={
              <div className="flex flex-wrap gap-2">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => {
                    setTarget("gateway");
                    setProbe("health");
                  }}
                >
                  Fill with test data
                </Button>
                <Button type="button" variant="secondary" onClick={() => preview.refetch()}>
                  Refresh
                </Button>
              </div>
            }
          />
          <div className="grid gap-3 p-4 md:grid-cols-2">
            <Label label="Service">
              <Select value={target} onChange={(event) => setTarget(event.target.value as ServiceTarget)}>
                {services.map((service) => (
                  <option key={service.target} value={service.target}>
                  {service.label}
                </option>
              ))}
              </Select>
              <p className="text-xs text-slate-500">
                Gateway health is the quickest full-stack smoke probe.
              </p>
            </Label>
            <Label label="Endpoint">
              <Select value={probe} onChange={(event) => setProbe(event.target.value as Probe)}>
                <option value="health">Health</option>
                <option value="metrics">Metrics</option>
                <option value="prometheus">Prometheus</option>
                <option value="openapi">OpenAPI</option>
              </Select>
              <p className="text-xs text-slate-500">
                Metrics, Prometheus, and OpenAPI use local admin auth for the gateway.
              </p>
            </Label>
          </div>
          <div className="p-4 pt-0">
            {preview.error ? (
              <EmptyState>{preview.error.message}</EmptyState>
            ) : preview.data ? (
              <JsonPanel
                title={`${paths[probe]} · ${preview.data.durationMs} ms`}
                value={{
                  url: preview.data.url,
                  status: preview.data.status,
                  headers: preview.data.headers,
                  body: preview.data.body,
                }}
              />
            ) : (
              <EmptyState>Loading</EmptyState>
            )}
          </div>
        </Card>
      </PageSection>
    </div>
  );
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
