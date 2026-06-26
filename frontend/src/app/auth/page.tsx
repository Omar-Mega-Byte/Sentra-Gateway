"use client";

import { Trash2 } from "lucide-react";
import { EndpointRunner } from "@/components/features/endpoint-runner";
import { JsonPanel } from "@/components/ui/json-panel";
import { Button, Card, CardHeader, Input, Label, PageSection, Select, Textarea } from "@/components/ui/primitives";
import { findEndpoint } from "@/lib/backend-contract";
import { buildAuthHeaders, previewJwt, useAuthStore, type AuthMode } from "@/lib/auth-store";
import { localAdminAuth, sampleAuthForJwtPreview } from "@/lib/test-data";

export default function AuthPage() {
  const auth = useAuthStore();
  const jwtPreview = previewJwt(auth.jwt);
  const profileProbe = findEndpoint("user-profile-read");
  const paymentProbe = findEndpoint("partner-payment-read");

  return (
    <div className="space-y-6">
      <PageSection title="Authentication">
        <Card>
          <CardHeader
            title="Credential Store"
            action={
              <div className="flex flex-wrap gap-2">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => auth.setAuth(localAdminAuth)}
                >
                  Fill local admin test data
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => auth.setAuth(sampleAuthForJwtPreview())}
                >
                  Fill JWT preview test data
                </Button>
                <Button type="button" variant="danger" onClick={auth.clearAuth}>
                  <Trash2 className="h-4 w-4" aria-hidden="true" />
                  Clear
                </Button>
              </div>
            }
          />
          <div className="grid gap-4 p-4 xl:grid-cols-2">
            <div className="space-y-4">
              <Label label="Mode">
                <Select
                  value={auth.authMode}
                  onChange={(event) => auth.setAuth({ authMode: event.target.value as AuthMode })}
                >
                  <option value="none">none</option>
                  <option value="jwt">jwt</option>
                  <option value="apiKey">apiKey</option>
                  <option value="basic">basic</option>
                </Select>
                <p className="text-xs text-slate-500">
                  Use basic for gateway admin APIs, jwt for user/order/notification routes, and apiKey for partner payment routes.
                </p>
              </Label>
              <Label label="JWT">
                <Textarea
                  value={auth.jwt}
                  onChange={(event) => auth.setAuth({ jwt: event.target.value })}
                  placeholder="Paste a real issuer-signed JWT. Seed subject: sentra-user-omar; tenant_id: tenant-demo."
                  spellCheck={false}
                />
                <p className="text-xs text-slate-500">
                  Required user scopes include profile:read, profile:write, orders:read,
                  orders:write, notifications:read, and notifications:write.
                </p>
              </Label>
              <div className="grid gap-3 md:grid-cols-2">
                <Label label="API Key">
                  <Input
                    value={auth.apiKey}
                    onChange={(event) => auth.setAuth({ apiKey: event.target.value })}
                    placeholder="Paste plaintext key returned by Issue API key"
                    spellCheck={false}
                  />
                  <p className="text-xs text-slate-500">
                    Create client partner-acme-ui, issue key, then paste the returned apiKey here.
                  </p>
                </Label>
                <Label label="API Key ID">
                  <Input
                    value={auth.apiKeyId}
                    onChange={(event) => auth.setAuth({ apiKeyId: event.target.value })}
                    placeholder="Paste issued keyId UUID"
                  />
                  <p className="text-xs text-slate-500">
                    Needed for signed payment/refund requests.
                  </p>
                </Label>
              </div>
              <div className="grid gap-3 md:grid-cols-2">
                <Label label="Basic Username">
                  <Input
                    value={auth.basicUsername}
                    onChange={(event) => auth.setAuth({ basicUsername: event.target.value })}
                    placeholder="admin"
                  />
                  <p className="text-xs text-slate-500">Local admin user from application-local.yml.</p>
                </Label>
                <Label label="Basic Password">
                  <Input
                    type="password"
                    value={auth.basicPassword}
                    onChange={(event) => auth.setAuth({ basicPassword: event.target.value })}
                    placeholder="sentra-admin"
                  />
                  <p className="text-xs text-slate-500">Local password for gateway admin APIs.</p>
                </Label>
              </div>
              <Label label="Request ID Prefix">
                <Input
                  value={auth.requestIdPrefix}
                  onChange={(event) => auth.setAuth({ requestIdPrefix: event.target.value })}
                  placeholder="ui-smoke"
                />
                <p className="text-xs text-slate-500">
                  Prefix for generated X-Request-Id values.
                </p>
              </Label>
            </div>
            <div className="space-y-4">
              <JsonPanel title="Identity Preview" value={jwtPreview} dense />
              <JsonPanel
                title="Header Preview"
                value={{
                  none: buildAuthHeaders("none", auth),
                  jwt: buildAuthHeaders("jwt", auth),
                  apiKey: buildAuthHeaders("apiKey", auth),
                  basic: redactBasic(buildAuthHeaders("basic", auth)),
                }}
                dense
              />
            </div>
          </div>
        </Card>
      </PageSection>

      <PageSection title="Live Probes">
        <div className="grid gap-4 xl:grid-cols-2">
          {profileProbe ? <EndpointRunner endpoint={profileProbe} compact /> : null}
          {paymentProbe ? <EndpointRunner endpoint={paymentProbe} compact /> : null}
        </div>
      </PageSection>
    </div>
  );
}

function redactBasic(headers: Record<string, string>) {
  if (!headers.Authorization) return headers;
  return { ...headers, Authorization: "Basic ********" };
}
