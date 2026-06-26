"use client";

import { EndpointRunner } from "@/components/features/endpoint-runner";
import { LoadTestPanel } from "@/components/features/load-test-panel";
import { ResourceList } from "@/components/features/resource-list";
import { PageSection, StatusPill } from "@/components/ui/primitives";
import { findEndpoint } from "@/lib/backend-contract";

type RateLimitRow = {
  id: string;
  subjectType: string;
  routeId?: string;
  method?: string;
  capacity: number;
  refillTokens: number;
  refillPeriodSeconds: number;
  enabled: boolean;
  version: number;
};

const actions = [
  "rate-limits-get",
  "rate-limits-create",
  "rate-limits-update",
  "rate-limits-delete",
];

export default function RateLimitsPage() {
  const listEndpoint = findEndpoint("rate-limits-list");
  if (!listEndpoint) return null;

  return (
    <div className="space-y-6">
      <PageSection title="Rate Limit Policies">
        <ResourceList<RateLimitRow>
          endpoint={listEndpoint}
          title="Policies"
          columns={[
            { key: "id", header: "ID", render: (row) => row.id },
            { key: "subject", header: "Subject", render: (row) => row.subjectType },
            { key: "route", header: "Route", render: (row) => row.routeId ?? "" },
            { key: "method", header: "Method", render: (row) => row.method ?? "" },
            { key: "limit", header: "Limit", render: (row) => `${row.capacity}/${row.refillPeriodSeconds}s` },
            {
              key: "enabled",
              header: "Enabled",
              render: (row) => <StatusPill tone={row.enabled ? "ok" : "warn"}>{String(row.enabled)}</StatusPill>,
            },
            { key: "version", header: "Version", render: (row) => row.version },
          ]}
        />
      </PageSection>
      <PageSection title="Policy Actions">
        <div className="grid gap-4 xl:grid-cols-2">
          {actions.map((id) => {
            const endpoint = findEndpoint(id);
            return endpoint ? <EndpointRunner key={id} endpoint={endpoint} compact /> : null;
          })}
        </div>
      </PageSection>
      <PageSection title="Behavior Test">
        <LoadTestPanel title="Rate-limit behavior" />
      </PageSection>
    </div>
  );
}
