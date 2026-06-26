"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { Send, ShieldCheck } from "lucide-react";
import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import {
  Button,
  Card,
  CardHeader,
  Input,
  Label,
  Select,
  StatusPill,
  Textarea,
} from "@/components/ui/primitives";
import { JsonPanel } from "@/components/ui/json-panel";
import type { EndpointDefinition, FieldConfig } from "@/lib/backend-contract";
import { useAuthStore } from "@/lib/auth-store";
import { compactObject, sendProxyRequest, type ProxyResponse } from "@/lib/proxy-client";
import { signSentraRequest } from "@/lib/signing";
import {
  sampleFieldValue,
  sampleHelper,
  samplePlaceholder,
} from "@/lib/test-data";

type FormValues = Record<string, unknown>;

export function EndpointRunner({
  endpoint,
  compact = false,
}: {
  endpoint: EndpointDefinition;
  compact?: boolean;
}) {
  const auth = useAuthStore();
  const fields = useMemo(
    () => [
      ...(endpoint.pathParams ?? []).map((field) => ({ ...field, scope: "path" as const })),
      ...(endpoint.queryParams ?? []).map((field) => ({ ...field, scope: "query" as const })),
      ...(endpoint.headerFields ?? []).map((field) => ({ ...field, scope: "header" as const })),
      ...(endpoint.bodyFields ?? []).map((field) => ({ ...field, scope: "body" as const })),
    ],
    [endpoint],
  );
  const [rawBody, setRawBody] = useState(
    endpoint.defaultBody === undefined ? "" : JSON.stringify(endpoint.defaultBody, null, 2),
  );
  const [lastRequest, setLastRequest] = useState<unknown>(null);
  const schema = useMemo(() => buildSchema(fields), [fields]);
  const defaults = useMemo(() => defaultsFor(fields, endpoint), [fields, endpoint]);
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: defaults,
  });

  const mutation = useMutation<ProxyResponse, Error, FormValues>({
    mutationFn: async (values) => {
      const pathValues = pickScope(fields, values, "path");
      const query = compactObject(pickScope(fields, values, "query")) as Record<
        string,
        string | number | boolean | null | undefined
      >;
      const extraHeaders = compactObject(pickScope(fields, values, "header")) as Record<string, string>;
      const body =
        endpoint.bodyFields && endpoint.bodyFields.length > 0
          ? compactObject(pickScope(fields, values, "body"))
          : undefined;
      const interpolatedPath = interpolate(endpoint.path, pathValues);
      const bodyText =
        endpoint.bodyFields && endpoint.bodyFields.length > 0
          ? JSON.stringify(body)
          : rawBody.trim()
            ? rawBody
            : undefined;

      let headers = extraHeaders;
      if (endpoint.signingRequired) {
        if (!auth.apiKey.trim() || !auth.apiKeyId.trim()) {
          throw new Error("API key and key ID are required for signed routes.");
        }
        const queryText = new URLSearchParams(
          Object.entries(query).map(([key, value]) => [key, `${value}`]),
        ).toString();
        const signed = await signSentraRequest({
          method: endpoint.method,
          path: interpolatedPath,
          query: queryText,
          body: bodyText ?? "",
          timestamp: Math.floor(Date.now() / 1000).toString(),
          nonce: crypto.randomUUID().replace(/-/g, ""),
          keyId: auth.apiKeyId.trim(),
          secret: auth.apiKey.trim(),
        });
        headers = { ...headers, ...signed.headers };
        setLastRequest({
          method: endpoint.method,
          path: interpolatedPath,
          query,
          headers,
          body: body ?? bodyText,
          canonical: signed.canonical,
        });
      } else {
        setLastRequest({
          method: endpoint.method,
          path: interpolatedPath,
          query,
          headers,
          body: body ?? bodyText,
        });
      }

      return sendProxyRequest(
        {
          target: endpoint.service,
          method: endpoint.method,
          path: interpolatedPath,
          query,
          headers,
          body,
          rawBody: body ? undefined : bodyText,
        },
        endpoint.auth,
      );
    },
  });

  return (
    <Card>
      <CardHeader
        title={endpoint.title}
        meta={
          <span className="inline-flex flex-wrap gap-2">
            <StatusPill>{endpoint.method}</StatusPill>
            <StatusPill>{endpoint.path}</StatusPill>
            <StatusPill>{endpoint.auth}</StatusPill>
            {endpoint.signingRequired ? (
              <StatusPill tone="warn">signed</StatusPill>
            ) : null}
          </span>
        }
      />
      <form
        className="space-y-4 p-4"
        onSubmit={form.handleSubmit((values) => mutation.mutate(values))}
      >
        {fields.length > 0 ? (
          <div className={`grid gap-3 ${compact ? "md:grid-cols-2" : "md:grid-cols-3"}`}>
            {fields.map((field) => (
              <FieldControl
                key={`${field.scope}:${field.name}`}
                field={field}
                endpoint={endpoint}
                register={form.register(field.name)}
                error={form.formState.errors[field.name]?.message?.toString()}
              />
            ))}
          </div>
        ) : null}

        {!endpoint.bodyFields && endpoint.defaultBody !== undefined ? (
          <Label label="Body">
            <Textarea
              value={rawBody}
              onChange={(event) => setRawBody(event.target.value)}
              placeholder={JSON.stringify(endpoint.defaultBody, null, 2)}
              spellCheck={false}
            />
            <p className="text-xs text-slate-500">
              Example request body from the backend contract.
            </p>
          </Label>
        ) : null}

        <div className="flex flex-wrap items-center gap-2">
          <Button
            type="button"
            variant="secondary"
            onClick={() => {
              form.reset(defaults);
              setRawBody(
                endpoint.defaultBody === undefined
                  ? ""
                  : JSON.stringify(endpoint.defaultBody, null, 2),
              );
            }}
          >
            Fill with test data
          </Button>
          <Button type="submit" disabled={mutation.isPending}>
            {endpoint.signingRequired ? (
              <ShieldCheck className="h-4 w-4" aria-hidden="true" />
            ) : (
              <Send className="h-4 w-4" aria-hidden="true" />
            )}
            {mutation.isPending ? "Sending" : "Send"}
          </Button>
          {mutation.data ? (
            <StatusPill tone={mutation.data.ok ? "ok" : "bad"}>
              {mutation.data.status} {mutation.data.statusText}
            </StatusPill>
          ) : null}
          {mutation.data ? (
            <StatusPill>{mutation.data.durationMs} ms</StatusPill>
          ) : null}
          {mutation.error ? <StatusPill tone="bad">{mutation.error.message}</StatusPill> : null}
        </div>
      </form>

      {(lastRequest || mutation.data) && (
        <div className="grid gap-4 border-t border-slate-100 p-4 xl:grid-cols-2">
          {lastRequest ? <JsonPanel title="Request" value={lastRequest} dense /> : null}
          {mutation.data ? (
            <JsonPanel
              title="Response"
              value={{
                status: mutation.data.status,
                headers: mutation.data.headers,
                body: mutation.data.body,
              }}
              dense
            />
          ) : null}
        </div>
      )}
    </Card>
  );
}

function FieldControl({
  field,
  endpoint,
  register,
  error,
}: {
  field: FieldConfig & { scope: string };
  endpoint: EndpointDefinition;
  register: ReturnType<typeof useForm>["register"] extends (...args: never[]) => infer R ? R : never;
  error?: string;
}) {
  const label = `${field.label}${field.required ? " *" : ""}`;
  const placeholder = samplePlaceholder(field, endpoint);
  const helper = sampleHelper(field, endpoint);
  return (
    <div className="space-y-1">
      <Label label={label}>
        {field.type === "textarea" ? (
          <Textarea {...register} placeholder={placeholder} />
        ) : field.type === "select" ? (
          <Select {...register} defaultValue={`${field.defaultValue ?? ""}`}>
            <option value=""></option>
            {(field.options ?? []).map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </Select>
        ) : field.type === "boolean" ? (
          <div className="flex h-9 items-center rounded-md border border-slate-200 px-3">
            <input
              {...register}
              type="checkbox"
              className="h-4 w-4 rounded border-slate-300 text-slate-900"
            />
          </div>
        ) : field.type === "json" ? (
          <Textarea {...register} placeholder={placeholder} spellCheck={false} />
        ) : (
          <Input
            {...register}
            type={field.type === "number" ? "number" : field.type === "datetime" ? "datetime-local" : "text"}
            placeholder={placeholder}
          />
        )}
      </Label>
      <p className="text-xs text-slate-500">{helper}</p>
      {error ? <p className="text-xs text-red-600">{error}</p> : null}
    </div>
  );
}

function buildSchema(fields: (FieldConfig & { scope: string })[]) {
  const shape: Record<string, z.ZodTypeAny> = {};
  fields.forEach((field) => {
    let schema: z.ZodTypeAny =
      field.type === "number"
        ? z.coerce.number()
        : field.type === "boolean"
          ? z.coerce.boolean()
          : z.any();
    if (field.required) {
      schema =
        field.type === "number" || field.type === "boolean"
          ? schema
          : z.any().refine((value) => value !== undefined && `${value}`.trim() !== "", {
              message: "Required",
            });
    }
    shape[field.name] = schema;
  });
  return z.object(shape);
}

function defaultsFor(fields: (FieldConfig & { scope: string })[], endpoint: EndpointDefinition) {
  return Object.fromEntries(
    fields.map((field) => {
      const value = sampleFieldValue(field, endpoint);
      if (field.type === "array") {
        return [
          field.name,
          Array.isArray(value) ? value.join(", ") : value ?? "",
        ];
      }
      if (field.type === "json") {
        return [
          field.name,
          value === undefined
            ? ""
            : JSON.stringify(value, null, 2),
        ];
      }
      if (field.type === "datetime") {
        return [field.name, value ?? ""];
      }
      return [field.name, value ?? ""];
    }),
  );
}

function pickScope(
  fields: (FieldConfig & { scope: string })[],
  values: FormValues,
  scope: "path" | "query" | "header" | "body",
) {
  const selected: Record<string, unknown> = {};
  fields
    .filter((field) => field.scope === scope)
    .forEach((field) => {
      const value = values[field.name];
      selected[field.name] = normalizeFieldValue(field, value);
    });
  return selected;
}

function normalizeFieldValue(field: FieldConfig, value: unknown) {
  if (field.type === "array") {
    if (Array.isArray(value)) return value;
    return `${value ?? ""}`
      .split(/[\n,]/)
      .map((part) => part.trim())
      .filter(Boolean);
  }
  if (field.type === "json") {
    if (typeof value !== "string" || value.trim() === "") return undefined;
    return JSON.parse(value);
  }
  if (field.type === "datetime") {
    const text = `${value ?? ""}`.trim();
    return text ? new Date(text).toISOString() : undefined;
  }
  if (field.type === "number") {
    return value === "" || value === undefined ? undefined : Number(value);
  }
  return value;
}

function interpolate(path: string, values: Record<string, unknown>) {
  return path.replace(/\{([^}]+)\}/g, (_, name: string) =>
    encodeURIComponent(`${values[name] ?? ""}`),
  );
}
