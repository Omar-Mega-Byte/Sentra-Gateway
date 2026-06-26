"use client";

import { useMutation } from "@tanstack/react-query";
import { Search } from "lucide-react";
import { useMemo, useState } from "react";
import { EndpointRunner } from "@/components/features/endpoint-runner";
import { DataTable } from "@/components/ui/data-table";
import { JsonPanel } from "@/components/ui/json-panel";
import { Button, Card, CardHeader, Input, Label, PageSection, Select, StatusPill } from "@/components/ui/primitives";
import { findEndpoint } from "@/lib/backend-contract";
import { sendProxyRequest } from "@/lib/proxy-client";
import { sampleAuditSearch } from "@/lib/test-data";

type AuditEvent = {
  id: string;
  eventTime: string;
  requestId: string;
  eventType: string;
  decision: string;
  reasonCode: string;
  routeId?: string;
  method?: string;
  path?: string;
  actorType?: string;
  subjectRef?: string;
  status: number;
  latencyMs: number;
};

export default function AuditPage() {
  const sample = sampleAuditSearch();
  const [from, setFrom] = useState(sample.from);
  const [to, setTo] = useState(sample.to);
  const [requestId, setRequestId] = useState(sample.requestId);
  const [routeId, setRouteId] = useState(sample.routeId);
  const [subject, setSubject] = useState(sample.subject);
  const [status, setStatus] = useState(sample.status);
  const [decision, setDecision] = useState(sample.decision);
  const [selected, setSelected] = useState<AuditEvent | null>(null);
  const [page, setPage] = useState(sample.page);
  const [pageSize, setPageSize] = useState(sample.pageSize);

  const search = useMutation({
    mutationFn: async () => {
      const response = await sendProxyRequest(
        {
          target: "gateway",
          method: "GET",
          path: "/api/v1/admin/audit-events",
          query: {
            from: new Date(from).toISOString(),
            to: new Date(to).toISOString(),
            requestId,
            routeId,
            page,
            pageSize,
          },
        },
        "basic",
      );
      if (!response.ok) throw new Error(`${response.status} ${response.statusText}`);
      return Array.isArray(response.body) ? (response.body as AuditEvent[]) : [];
    },
  });

  const rows = useMemo(() => {
    return (search.data ?? []).filter((event) => {
      if (subject && !event.subjectRef?.includes(subject)) return false;
      if (status && String(event.status) !== status) return false;
      if (decision && event.decision !== decision) return false;
      return true;
    });
  }, [decision, search.data, status, subject]);

  const getEndpoint = findEndpoint("audit-events-get");
  const actionsEndpoint = findEndpoint("admin-actions-list");

  function fillWithTestData() {
    const next = sampleAuditSearch();
    setFrom(next.from);
    setTo(next.to);
    setRequestId(next.requestId);
    setRouteId(next.routeId);
    setSubject(next.subject);
    setStatus(next.status);
    setDecision(next.decision);
    setPage(next.page);
    setPageSize(next.pageSize);
  }

  return (
    <div className="space-y-6">
      <PageSection title="Audit Search">
        <Card>
          <CardHeader
            title="Search"
            action={
              <Button type="button" variant="secondary" onClick={fillWithTestData}>
                Fill with test data
              </Button>
            }
            meta={
              search.error ? (
                <StatusPill tone="bad">{search.error.message}</StatusPill>
              ) : search.data ? (
                <StatusPill tone="ok">{rows.length} events</StatusPill>
              ) : null
            }
          />
          <div className="space-y-4 p-4">
            <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
              <Label label="From">
                <Input
                  type="datetime-local"
                  value={from}
                  onChange={(event) => setFrom(event.target.value)}
                />
                <p className="text-xs text-slate-500">
                  Start of the backend audit search window.
                </p>
              </Label>
              <Label label="To">
                <Input
                  type="datetime-local"
                  value={to}
                  onChange={(event) => setTo(event.target.value)}
                />
                <p className="text-xs text-slate-500">
                  End of the backend audit search window.
                </p>
              </Label>
              <Label label="Request ID">
                <Input
                  value={requestId}
                  onChange={(event) => setRequestId(event.target.value)}
                  placeholder="ui-smoke-request"
                />
                <p className="text-xs text-slate-500">
                  Optional backend filter for one X-Request-Id.
                </p>
              </Label>
              <Label label="Route ID">
                <Input
                  value={routeId}
                  onChange={(event) => setRouteId(event.target.value)}
                  placeholder="user-profile-read"
                />
                <p className="text-xs text-slate-500">
                  Backend route filter; seeded routes include user-profile-read.
                </p>
              </Label>
              <Label label="Subject">
                <Input
                  value={subject}
                  onChange={(event) => setSubject(event.target.value)}
                  placeholder="sentra-user-omar"
                />
                <p className="text-xs text-slate-500">
                  Client-side filter against returned subjectRef values.
                </p>
              </Label>
              <Label label="HTTP Status">
                <Input
                  value={status}
                  onChange={(event) => setStatus(event.target.value)}
                  placeholder="200"
                />
                <p className="text-xs text-slate-500">
                  Optional client-side status filter.
                </p>
              </Label>
              <Label label="Decision">
                <Select value={decision} onChange={(event) => setDecision(event.target.value)}>
                  <option value=""></option>
                  <option value="ALLOW">ALLOW</option>
                  <option value="DENY">DENY</option>
                  <option value="ERROR">ERROR</option>
                </Select>
                <p className="text-xs text-slate-500">
                  Optional client-side decision filter.
                </p>
              </Label>
              <div className="grid grid-cols-2 gap-3">
                <Label label="Page">
                  <Input
                    type="number"
                    value={page}
                    onChange={(event) => setPage(Number(event.target.value))}
                    placeholder="0"
                  />
                  <p className="text-xs text-slate-500">Backend page index.</p>
                </Label>
                <Label label="Size">
                  <Input
                    type="number"
                    value={pageSize}
                    onChange={(event) => setPageSize(Number(event.target.value))}
                    placeholder="20"
                  />
                  <p className="text-xs text-slate-500">Backend page size.</p>
                </Label>
              </div>
            </div>
            <Button type="button" onClick={() => search.mutate()} disabled={search.isPending}>
              <Search className="h-4 w-4" aria-hidden="true" />
              {search.isPending ? "Searching" : "Search"}
            </Button>
          </div>
        </Card>
      </PageSection>

      <div className="grid gap-6 xl:grid-cols-[1fr_420px]">
        <PageSection title="Events">
          <DataTable
            rows={rows}
            columns={[
              { key: "time", header: "Time", render: (row) => new Date(row.eventTime).toLocaleString() },
              { key: "route", header: "Route", render: (row) => row.routeId ?? "" },
              { key: "subject", header: "Subject", render: (row) => row.subjectRef ?? "" },
              { key: "decision", header: "Decision", render: (row) => row.decision },
              { key: "status", header: "Status", render: (row) => row.status },
              {
                key: "view",
                header: "",
                render: (row) => (
                  <Button type="button" variant="secondary" className="h-8 px-2" onClick={() => setSelected(row)}>
                    View
                  </Button>
                ),
              },
            ]}
          />
        </PageSection>
        <PageSection title="Event Details">
          <JsonPanel title={selected?.id ?? "Selected event"} value={selected ?? {}} dense />
        </PageSection>
      </div>

      <PageSection title="Audit Actions">
        <div className="grid gap-4 xl:grid-cols-2">
          {getEndpoint ? <EndpointRunner endpoint={getEndpoint} compact /> : null}
          {actionsEndpoint ? <EndpointRunner endpoint={actionsEndpoint} compact /> : null}
        </div>
      </PageSection>
    </div>
  );
}
