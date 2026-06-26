"use client";

import { EndpointRunner } from "@/components/features/endpoint-runner";
import { LoadTestPanel } from "@/components/features/load-test-panel";
import { ResourceList } from "@/components/features/resource-list";
import { PageSection, StatusPill } from "@/components/ui/primitives";
import { findEndpoint } from "@/lib/backend-contract";

type RiskRuleRow = {
  id: string;
  signal: string;
  thresholdValue: number;
  weight: number;
  action: string;
  routeId?: string;
  enabled: boolean;
  version: number;
};

const actions = ["risk-rules-get", "risk-rules-create", "risk-rules-update", "risk-rules-delete"];

export default function RiskRulesPage() {
  const listEndpoint = findEndpoint("risk-rules-list");
  if (!listEndpoint) return null;

  return (
    <div className="space-y-6">
      <PageSection title="Risk Rules">
        <ResourceList<RiskRuleRow>
          endpoint={listEndpoint}
          title="Rules"
          columns={[
            { key: "id", header: "ID", render: (row) => row.id },
            { key: "signal", header: "Signal", render: (row) => row.signal },
            { key: "threshold", header: "Threshold", render: (row) => row.thresholdValue },
            { key: "action", header: "Action", render: (row) => row.action },
            { key: "route", header: "Route", render: (row) => row.routeId ?? "" },
            {
              key: "enabled",
              header: "Enabled",
              render: (row) => <StatusPill tone={row.enabled ? "ok" : "warn"}>{String(row.enabled)}</StatusPill>,
            },
            { key: "version", header: "Version", render: (row) => row.version },
          ]}
        />
      </PageSection>
      <PageSection title="Rule Actions">
        <div className="grid gap-4 xl:grid-cols-2">
          {actions.map((id) => {
            const endpoint = findEndpoint(id);
            return endpoint ? <EndpointRunner key={id} endpoint={endpoint} compact /> : null;
          })}
        </div>
      </PageSection>
      <PageSection title="Risk Decision Test">
        <LoadTestPanel title="Risk policy behavior" />
      </PageSection>
    </div>
  );
}
