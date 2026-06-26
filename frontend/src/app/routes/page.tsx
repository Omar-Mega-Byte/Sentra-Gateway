"use client";

import { EndpointRunner } from "@/components/features/endpoint-runner";
import { LoadTestPanel } from "@/components/features/load-test-panel";
import { ResourceList } from "@/components/features/resource-list";
import { PageSection, StatusPill } from "@/components/ui/primitives";
import { findEndpoint } from "@/lib/backend-contract";

type GatewayRouteRow = {
  id: string;
  category: string;
  pathPatterns: string[];
  methods: string[];
  enabled: boolean;
  signingRequired: boolean;
  version: number;
};

const actions = [
  "routes-get",
  "routes-validate",
  "routes-create",
  "routes-update",
  "routes-enable",
  "routes-disable",
  "routes-delete",
  "routes-generation",
];

export default function RoutesPage() {
  const listEndpoint = findEndpoint("routes-list");
  if (!listEndpoint) return null;

  return (
    <div className="space-y-6">
      <PageSection title="Routes">
        <ResourceList<GatewayRouteRow>
          endpoint={listEndpoint}
          title="Route Registry"
          columns={[
            { key: "id", header: "ID", render: (row) => row.id },
            { key: "category", header: "Category", render: (row) => row.category },
            { key: "methods", header: "Methods", render: (row) => row.methods?.join(", ") },
            { key: "paths", header: "Paths", render: (row) => row.pathPatterns?.join(", ") },
            {
              key: "enabled",
              header: "Enabled",
              render: (row) => <StatusPill tone={row.enabled ? "ok" : "warn"}>{String(row.enabled)}</StatusPill>,
            },
            {
              key: "signed",
              header: "Signed",
              render: (row) => String(row.signingRequired),
            },
            { key: "version", header: "Version", render: (row) => row.version },
          ]}
        />
      </PageSection>

      <PageSection title="Route Actions">
        <div className="grid gap-4 xl:grid-cols-2">
          {actions.map((id) => {
            const endpoint = findEndpoint(id);
            return endpoint ? <EndpointRunner key={id} endpoint={endpoint} compact /> : null;
          })}
        </div>
      </PageSection>

      <PageSection title="Forwarding Test">
        <LoadTestPanel title="Route forwarding" />
      </PageSection>
    </div>
  );
}
