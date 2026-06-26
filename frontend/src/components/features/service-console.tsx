import { EndpointRunner } from "@/components/features/endpoint-runner";
import { PageSection } from "@/components/ui/primitives";
import type { EndpointDefinition } from "@/lib/backend-contract";

export function ServiceConsole({
  title,
  endpoints,
}: {
  title: string;
  endpoints: EndpointDefinition[];
}) {
  return (
    <div className="space-y-6">
      <PageSection title={title}>
        <div className="grid gap-4 xl:grid-cols-2">
          {endpoints.map((endpoint) => (
            <EndpointRunner key={endpoint.id} endpoint={endpoint} compact />
          ))}
        </div>
      </PageSection>
    </div>
  );
}
