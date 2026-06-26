"use client";

import { useState } from "react";
import { Button, Card, CardHeader, Input, Label, Select, StatusPill } from "@/components/ui/primitives";
import { DataTable } from "@/components/ui/data-table";
import { sendProxyRequest, type ProxyResponse } from "@/lib/proxy-client";
import type { AuthMode } from "@/lib/auth-store";
import { sampleLoadTest } from "@/lib/test-data";

const methods = ["GET", "POST", "PUT", "PATCH", "DELETE"];

export function LoadTestPanel({ title = "Gateway test" }: { title?: string }) {
  const sample = sampleLoadTest();
  const [path, setPath] = useState(sample.path);
  const [method, setMethod] = useState(sample.method);
  const [authMode, setAuthMode] = useState<AuthMode>(sample.authMode);
  const [count, setCount] = useState(sample.count);
  const [running, setRunning] = useState(false);
  const [rows, setRows] = useState<ProxyResponse[]>([]);

  return (
    <Card>
      <CardHeader
        title={title}
        meta={<StatusPill>{rows.length} attempts</StatusPill>}
      />
      <div className="space-y-4 p-4">
        <div className="grid gap-3 md:grid-cols-[1fr_120px_120px_120px]">
          <Label label="Path">
            <Input
              value={path}
              onChange={(event) => setPath(event.target.value)}
              placeholder="/api/v1/public/users/7aa99db8-a943-4b63-b4b7-79f769ef9f87"
            />
            <p className="text-xs text-slate-500">
              Public seeded profile route is useful for quick gateway and rate-limit checks.
            </p>
          </Label>
          <Label label="Method">
            <Select value={method} onChange={(event) => setMethod(event.target.value)}>
              {methods.map((value) => (
                <option key={value}>{value}</option>
              ))}
            </Select>
            <p className="text-xs text-slate-500">GET is safest for repeated smoke requests.</p>
          </Label>
          <Label label="Auth">
            <Select value={authMode} onChange={(event) => setAuthMode(event.target.value as AuthMode)}>
              <option value="none">none</option>
              <option value="jwt">jwt</option>
              <option value="apiKey">apiKey</option>
              <option value="basic">basic</option>
            </Select>
            <p className="text-xs text-slate-500">
              Choose the auth mode required by the route under test.
            </p>
          </Label>
          <Label label="Count">
            <Input
              type="number"
              min={1}
              max={25}
              value={count}
              onChange={(event) => setCount(Number(event.target.value))}
              placeholder="3"
            />
            <p className="text-xs text-slate-500">
              Keep this small for smoke tests; use 3-10 to see rate-limit headers.
            </p>
          </Label>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            variant="secondary"
            disabled={running}
            onClick={() => {
              const next = sampleLoadTest();
              setPath(next.path);
              setMethod(next.method);
              setAuthMode(next.authMode);
              setCount(next.count);
            }}
          >
            Fill with test data
          </Button>
          <Button
            type="button"
            disabled={running}
            onClick={async () => {
              setRunning(true);
              setRows([]);
              const next: ProxyResponse[] = [];
              for (let index = 0; index < count; index += 1) {
                const response = await sendProxyRequest(
                  { target: "gateway", method, path },
                  authMode,
                );
                next.push(response);
                setRows([...next]);
              }
              setRunning(false);
            }}
          >
            {running ? "Running" : "Run"}
          </Button>
        </div>
        <DataTable
          rows={rows}
          columns={[
            { key: "status", header: "Status", render: (row) => `${row.status} ${row.statusText}` },
            { key: "time", header: "Time", render: (row) => `${row.durationMs} ms` },
            { key: "remaining", header: "Remaining", render: (row) => row.headers["ratelimit-remaining"] ?? "" },
            { key: "body", header: "Code", render: (row) => codeFromBody(row.body) },
          ]}
        />
      </div>
    </Card>
  );
}

function codeFromBody(body: unknown) {
  if (typeof body === "object" && body !== null && "code" in body) {
    return String((body as { code?: unknown }).code ?? "");
  }
  return "";
}
