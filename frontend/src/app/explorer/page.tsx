"use client";

import { useMemo, useState } from "react";
import { JsonPanel } from "@/components/ui/json-panel";
import { Button, Card, CardHeader, Input, Label, PageSection, Select, Textarea } from "@/components/ui/primitives";
import { endpointRegistry, services, type EndpointDefinition } from "@/lib/backend-contract";
import type { AuthMode } from "@/lib/auth-store";
import { sendProxyRequest, type ProxyResponse, type ServiceTarget } from "@/lib/proxy-client";
import { sampleExplorerRequest } from "@/lib/test-data";

export default function ExplorerPage() {
  const [endpointId, setEndpointId] = useState(endpointRegistry[0]?.id ?? "");
  const selected = endpointRegistry.find((endpoint) => endpoint.id === endpointId);
  return (
    <div className="space-y-6">
      <PageSection title="API Explorer">
        <ExplorerForm
          key={selected?.id}
          endpoint={selected}
          endpointId={endpointId}
          setEndpointId={setEndpointId}
        />
      </PageSection>
    </div>
  );
}

function ExplorerForm({
  endpoint,
  endpointId,
  setEndpointId,
}: {
  endpoint?: EndpointDefinition;
  endpointId: string;
  setEndpointId: (id: string) => void;
}) {
  const initial = sampleExplorerRequest(endpoint);
  const [target, setTarget] = useState<ServiceTarget>(initial.target);
  const [method, setMethod] = useState<string>(initial.method);
  const [path, setPath] = useState(initial.path);
  const [auth, setAuth] = useState<AuthMode>(initial.auth);
  const [query, setQuery] = useState(initial.query);
  const [headers, setHeaders] = useState(initial.headers);
  const [body, setBody] = useState(initial.body);
  const [response, setResponse] = useState<ProxyResponse | null>(null);
  const [error, setError] = useState<string>("");
  const grouped = useMemo(() => {
    return endpointRegistry.reduce<Record<string, EndpointDefinition[]>>((acc, item) => {
      acc[item.group] = [...(acc[item.group] ?? []), item];
      return acc;
    }, {});
  }, []);

  async function send() {
    setError("");
    try {
      const result = await sendProxyRequest(
        {
          target,
          method,
          path,
          query: parseJson(query),
          headers: parseJson(headers) as Record<string, string>,
          rawBody: body.trim() || undefined,
        },
        auth,
      );
      setResponse(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Request failed");
    }
  }

  function fillWithTestData(nextEndpoint = endpoint) {
    const sample = sampleExplorerRequest(nextEndpoint);
    setTarget(sample.target);
    setMethod(sample.method);
    setPath(sample.path);
    setAuth(sample.auth);
    setQuery(sample.query);
    setHeaders(sample.headers);
    setBody(sample.body);
  }

  return (
    <Card>
      <CardHeader title="Request" />
      <div className="space-y-4 p-4">
        <div className="grid gap-3 md:grid-cols-2">
          <Label label="Endpoint">
            <Select
              value={endpointId}
              onChange={(event) => {
                const next = endpointRegistry.find((item) => item.id === event.target.value);
                setEndpointId(event.target.value);
                if (next) {
                  fillWithTestData(next);
                }
              }}
            >
              {Object.entries(grouped).map(([group, endpoints]) => (
                <optgroup key={group} label={group}>
                  {endpoints.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.method} {item.path}
                    </option>
                  ))}
                </optgroup>
              ))}
            </Select>
            <p className="text-xs text-slate-500">
              Pick a discovered backend endpoint to load its real method, path, auth, and sample DTO.
            </p>
          </Label>
          <Label label="Target">
            <Select value={target} onChange={(event) => setTarget(event.target.value as ServiceTarget)}>
              {services.map((service) => (
                <option key={service.target} value={service.target}>
                  {service.label}
                </option>
              ))}
            </Select>
            <p className="text-xs text-slate-500">
              Use Gateway for routed API tests; direct services are useful for health/OpenAPI checks.
            </p>
          </Label>
        </div>
        <div className="grid gap-3 md:grid-cols-[140px_1fr_140px]">
          <Label label="Method">
            <Select value={method} onChange={(event) => setMethod(event.target.value)}>
              {["GET", "POST", "PUT", "PATCH", "DELETE"].map((value) => (
                <option key={value}>{value}</option>
              ))}
            </Select>
            <p className="text-xs text-slate-500">
              Matches the selected endpoint; change only for manual probes.
            </p>
          </Label>
          <Label label="Path">
            <Input
              value={path}
              onChange={(event) => setPath(event.target.value)}
              placeholder="/api/v1/public/users/7aa99db8-a943-4b63-b4b7-79f769ef9f87"
            />
            <p className="text-xs text-slate-500">
              Use a gateway path with path variables already replaced by test IDs.
            </p>
          </Label>
          <Label label="Auth">
            <Select value={auth} onChange={(event) => setAuth(event.target.value as AuthMode)}>
              <option value="none">none</option>
              <option value="jwt">jwt</option>
              <option value="apiKey">apiKey</option>
              <option value="basic">basic</option>
            </Select>
            <p className="text-xs text-slate-500">
              Credentials are read from the Authentication page.
            </p>
          </Label>
        </div>
        <div className="grid gap-3 xl:grid-cols-3">
          <Label label="Query JSON">
            <Textarea
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder={'{\n  "page": 0,\n  "size": 20\n}'}
              spellCheck={false}
            />
            <p className="text-xs text-slate-500">
              JSON object for query parameters. Leave as {} when the endpoint has no query DTO.
            </p>
          </Label>
          <Label label="Headers JSON">
            <Textarea
              value={headers}
              onChange={(event) => setHeaders(event.target.value)}
              placeholder={'{\n  "Idempotency-Key": "ui-order-create-001"\n}'}
              spellCheck={false}
            />
            <p className="text-xs text-slate-500">
              Extra headers only. Auth headers come from the Authentication page.
            </p>
          </Label>
          <Label label="Body">
            <Textarea
              value={body}
              onChange={(event) => setBody(event.target.value)}
              placeholder={'{\n  "items": [{ "sku": "BOOK-JAVA-25", "quantity": 1 }]\n}'}
              spellCheck={false}
            />
            <p className="text-xs text-slate-500">
              JSON request body from the selected backend DTO. Leave empty for GET/DELETE.
            </p>
          </Label>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button type="button" variant="secondary" onClick={() => fillWithTestData()}>
            Fill with test data
          </Button>
          <Button type="button" onClick={send}>
            Send
          </Button>
        </div>
        {error ? <p className="text-sm text-red-600">{error}</p> : null}
      </div>
      <div className="grid gap-4 border-t border-slate-100 p-4 xl:grid-cols-2">
        <JsonPanel
          title="Request"
          value={{
            target,
            method,
            path,
            query: safeParse(query),
            headers: safeParse(headers),
            body: safeParse(body),
            auth,
          }}
          dense
        />
        <JsonPanel title="Response" value={response ?? {}} dense />
      </div>
    </Card>
  );
}

function parseJson(value: string) {
  if (!value.trim()) return {};
  return JSON.parse(value);
}

function safeParse(value: string) {
  if (!value.trim()) return "";
  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
}
