"use client";

import { EndpointRunner } from "@/components/features/endpoint-runner";
import { ResourceList } from "@/components/features/resource-list";
import { PageSection, StatusPill } from "@/components/ui/primitives";
import { findEndpoint } from "@/lib/backend-contract";

type ApiClientRow = {
  id: string;
  name: string;
  owner: string;
  tenantId?: string;
  status: string;
  version: number;
  createdAt: string;
};

const actions = [
  "api-clients-get",
  "api-clients-create",
  "api-clients-update",
  "api-clients-disable",
];

export default function ClientsPage() {
  const listEndpoint = findEndpoint("api-clients-list");
  if (!listEndpoint) return null;

  return (
    <div className="space-y-6">
      <PageSection title="API Clients">
        <ResourceList<ApiClientRow>
          endpoint={listEndpoint}
          title="Registered Clients"
          columns={[
            { key: "name", header: "Name", render: (row) => row.name },
            { key: "owner", header: "Owner", render: (row) => row.owner },
            { key: "tenant", header: "Tenant", render: (row) => row.tenantId ?? "" },
            {
              key: "status",
              header: "Status",
              render: (row) => <StatusPill tone={row.status === "ACTIVE" ? "ok" : "warn"}>{row.status}</StatusPill>,
            },
            { key: "version", header: "Version", render: (row) => row.version },
            { key: "id", header: "ID", render: (row) => row.id },
          ]}
        />
      </PageSection>

      <PageSection title="Client Actions">
        <div className="grid gap-4 xl:grid-cols-2">
          {actions.map((id) => {
            const endpoint = findEndpoint(id);
            return endpoint ? <EndpointRunner key={id} endpoint={endpoint} compact /> : null;
          })}
        </div>
      </PageSection>
    </div>
  );
}
