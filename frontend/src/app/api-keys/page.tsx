"use client";

import { EndpointRunner } from "@/components/features/endpoint-runner";
import { PageSection } from "@/components/ui/primitives";
import { findEndpoint } from "@/lib/backend-contract";

const actions = [
  "api-keys-list",
  "api-keys-issue",
  "api-keys-rotate",
  "api-keys-revoke",
];

export default function ApiKeysPage() {
  return (
    <div className="space-y-6">
      <PageSection title="API Key Management">
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
