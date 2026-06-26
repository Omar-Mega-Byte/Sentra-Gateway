"use client";

import { EndpointRunner } from "@/components/features/endpoint-runner";
import { LoadTestPanel } from "@/components/features/load-test-panel";
import { ResourceList } from "@/components/features/resource-list";
import { PageSection, StatusPill } from "@/components/ui/primitives";
import { findEndpoint } from "@/lib/backend-contract";

type IpRuleRow = {
  id: string;
  network: string;
  action: string;
  routeId?: string;
  priority: number;
  enabled: boolean;
  version: number;
};

const actions = ["ip-rules-get", "ip-rules-create", "ip-rules-update", "ip-rules-delete"];

export default function IpRulesPage() {
  const listEndpoint = findEndpoint("ip-rules-list");
  if (!listEndpoint) return null;

  return (
    <div className="space-y-6">
      <PageSection title="IP Rules">
        <ResourceList<IpRuleRow>
          endpoint={listEndpoint}
          title="Rules"
          columns={[
            { key: "id", header: "ID", render: (row) => row.id },
            { key: "network", header: "Network", render: (row) => row.network },
            { key: "action", header: "Action", render: (row) => row.action },
            { key: "route", header: "Route", render: (row) => row.routeId ?? "" },
            { key: "priority", header: "Priority", render: (row) => row.priority },
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
      <PageSection title="Gateway Decision Test">
        <LoadTestPanel title="IP policy behavior" />
      </PageSection>
    </div>
  );
}
