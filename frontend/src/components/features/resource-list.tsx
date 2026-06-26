"use client";

import { useQuery } from "@tanstack/react-query";
import { RefreshCcw } from "lucide-react";
import { Button, Card, CardHeader, StatusPill } from "@/components/ui/primitives";
import { DataTable, type Column } from "@/components/ui/data-table";
import type { EndpointDefinition } from "@/lib/backend-contract";
import { sendProxyRequest } from "@/lib/proxy-client";

export function ResourceList<T extends Record<string, unknown>>({
  endpoint,
  title,
  columns,
}: {
  endpoint: EndpointDefinition;
  title: string;
  columns: Column<T>[];
}) {
  const query = useQuery({
    queryKey: ["resource-list", endpoint.id],
    queryFn: async () => {
      const response = await sendProxyRequest(
        {
          target: endpoint.service,
          method: endpoint.method,
          path: endpoint.path,
        },
        endpoint.auth,
      );
      if (!response.ok) {
        throw new Error(`${response.status} ${response.statusText}`);
      }
      return Array.isArray(response.body) ? (response.body as T[]) : [];
    },
  });

  return (
    <Card>
      <CardHeader
        title={title}
        meta={
          query.error ? (
            <StatusPill tone="bad">{query.error.message}</StatusPill>
          ) : query.isLoading ? (
            <StatusPill>Loading</StatusPill>
          ) : (
            <StatusPill tone="ok">{query.data?.length ?? 0} records</StatusPill>
          )
        }
        action={
          <Button
            type="button"
            variant="secondary"
            className="h-8 px-2"
            onClick={() => query.refetch()}
            title="Refresh"
          >
            <RefreshCcw className="h-4 w-4" aria-hidden="true" />
          </Button>
        }
      />
      <div className="p-4">
        <DataTable rows={query.data ?? []} columns={columns} />
      </div>
    </Card>
  );
}
