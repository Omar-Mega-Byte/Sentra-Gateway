"use client";

import { useState } from "react";
import { EndpointRunner } from "@/components/features/endpoint-runner";
import { JsonPanel } from "@/components/ui/json-panel";
import { Button, Card, CardHeader, Input, Label, PageSection, Select, Textarea } from "@/components/ui/primitives";
import { findEndpoint } from "@/lib/backend-contract";
import { useAuthStore } from "@/lib/auth-store";
import { sendProxyRequest, type ProxyResponse } from "@/lib/proxy-client";
import { epochSeconds, nonce, signSentraRequest } from "@/lib/signing";
import { sampleSigning } from "@/lib/test-data";

export default function SigningPage() {
  const auth = useAuthStore();
  const sample = sampleSigning();
  const [method, setMethod] = useState<"POST">(sample.method);
  const [path, setPath] = useState(sample.path);
  const [body, setBody] = useState(sample.body);
  const [timestamp, setTimestamp] = useState(epochSeconds());
  const [nonceValue, setNonceValue] = useState(nonce());
  const [idempotencyKey, setIdempotencyKey] = useState(sample.idempotencyKey);
  const [result, setResult] = useState<unknown>(null);
  const [responses, setResponses] = useState<ProxyResponse[]>([]);
  const paymentCreate = findEndpoint("partner-payment-create");
  const refundCreate = findEndpoint("partner-refund-create");

  async function build() {
    const signed = await signSentraRequest({
      method,
      path,
      body,
      timestamp,
      nonce: nonceValue,
      keyId: auth.apiKeyId,
      secret: auth.apiKey,
    });
    const headers = {
      "X-API-Key": auth.apiKey,
      "Idempotency-Key": idempotencyKey,
      ...signed.headers,
    };
    setResult({ canonical: signed.canonical, signature: signed.signature, headers });
    return headers;
  }

  async function send(replay = false) {
    const headers = await build();
    const first = await sendProxyRequest(
      { target: "gateway", method, path, headers, rawBody: body },
      "none",
    );
    if (!replay) {
      setResponses([first]);
      return;
    }
    const second = await sendProxyRequest(
      { target: "gateway", method, path, headers, rawBody: body },
      "none",
    );
    setResponses([first, second]);
  }

  function fillWithTestData() {
    const next = sampleSigning();
    setMethod(next.method);
    setPath(next.path);
    setBody(next.body);
    setIdempotencyKey(next.idempotencyKey);
    setTimestamp(epochSeconds());
    setNonceValue(nonce());
  }

  return (
    <div className="space-y-6">
      <PageSection title="Signing Playground">
        <Card>
          <CardHeader title="Signed Request" />
          <div className="space-y-4 p-4">
            <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
              <Label label="Method">
                <Select value={method} onChange={(event) => setMethod(event.target.value as "POST")}>
                  <option value="POST">POST</option>
                </Select>
                <p className="text-xs text-slate-500">
                  Signed partner routes currently use POST.
                </p>
              </Label>
              <Label label="Path">
                <Input
                  value={path}
                  onChange={(event) => setPath(event.target.value)}
                  placeholder="/api/v1/partner/payments"
                />
                <p className="text-xs text-slate-500">
                  Use /api/v1/partner/payments or /api/v1/partner/refunds.
                </p>
              </Label>
              <Label label="Timestamp">
                <Input
                  value={timestamp}
                  onChange={(event) => setTimestamp(event.target.value)}
                  placeholder="1781654400"
                />
                <p className="text-xs text-slate-500">
                  Unix epoch seconds included in the signature and replay check.
                </p>
              </Label>
              <Label label="Nonce">
                <Input
                  value={nonceValue}
                  onChange={(event) => setNonceValue(event.target.value)}
                  placeholder="f3a4b5c6d7e84900a1b2c3d4e5f60718"
                />
                <p className="text-xs text-slate-500">
                  Unique value per signed request; reuse it to test replay rejection.
                </p>
              </Label>
              <Label label="Key ID">
                <Input
                  value={auth.apiKeyId}
                  onChange={(event) => auth.setAuth({ apiKeyId: event.target.value })}
                  placeholder="Paste issued keyId UUID"
                />
                <p className="text-xs text-slate-500">
                  Returned when you issue or rotate a key for partner-acme-ui.
                </p>
              </Label>
              <Label label="API Key">
                <Input
                  value={auth.apiKey}
                  onChange={(event) => auth.setAuth({ apiKey: event.target.value })}
                  placeholder="Paste the plaintext apiKey shown once after issue/rotate"
                  spellCheck={false}
                />
                <p className="text-xs text-slate-500">
                  Plaintext secret is only available in the issue or rotate response.
                </p>
              </Label>
              <Label label="Idempotency-Key">
                <Input
                  value={idempotencyKey}
                  onChange={(event) => setIdempotencyKey(event.target.value)}
                  placeholder="ui-payment-create-001"
                />
                <p className="text-xs text-slate-500">
                  Required by payment/refund creation routes.
                </p>
              </Label>
              <div className="flex items-end gap-2">
                <Button type="button" variant="secondary" onClick={() => setTimestamp(epochSeconds())}>
                  Timestamp
                </Button>
                <Button type="button" variant="secondary" onClick={() => setNonceValue(nonce())}>
                  Nonce
                </Button>
              </div>
            </div>
            <Label label="Body">
              <Textarea
                value={body}
                onChange={(event) => setBody(event.target.value)}
                placeholder={'{\n  "merchantReference": "ui-acme-order-1002",\n  "amount": "125.50",\n  "currency": "USD",\n  "description": "Security gateway UI payment test"\n}'}
                spellCheck={false}
              />
              <p className="text-xs text-slate-500">
                JSON body must match exactly when generating and sending the signature.
              </p>
            </Label>
            <div className="flex flex-wrap gap-2">
              <Button type="button" variant="secondary" onClick={fillWithTestData}>
                Fill with test data
              </Button>
              <Button type="button" variant="secondary" onClick={build}>
                Generate
              </Button>
              <Button type="button" onClick={() => send(false)}>
                Send
              </Button>
              <Button type="button" variant="secondary" onClick={() => send(true)}>
                Send Replay
              </Button>
            </div>
          </div>
          <div className="grid gap-4 border-t border-slate-100 p-4 xl:grid-cols-2">
            <JsonPanel title="Signature" value={result ?? {}} dense />
            <JsonPanel title="Responses" value={responses} dense />
          </div>
        </Card>
      </PageSection>

      <PageSection title="Signed Gateway Routes">
        <div className="grid gap-4 xl:grid-cols-2">
          {paymentCreate ? <EndpointRunner endpoint={paymentCreate} compact /> : null}
          {refundCreate ? <EndpointRunner endpoint={refundCreate} compact /> : null}
        </div>
      </PageSection>
    </div>
  );
}
