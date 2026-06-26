import { createHmac } from "node:crypto";
import { describe, expect, it } from "vitest";
import { canonicalQuery, canonicalString, normalizePath, signSentraRequest } from "@/lib/signing";

describe("Sentra signing helpers", () => {
  it("matches the backend canonical request shape", async () => {
    const canonical = await canonicalString({
      method: "post",
      path: "/api/v1/partner/../partner/payments",
      query: "b=2&a=hello%20world&a=alpha",
      body: "",
      timestamp: "1710000000",
      nonce: "nonce-123456789012",
      keyId: "00000000-0000-4000-8000-000000000001",
    });

    expect(canonical).toBe(
      [
        "POST",
        "/api/v1/partner/payments",
        "a=alpha&a=hello%20world&b=2",
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        "1710000000",
        "nonce-123456789012",
        "00000000-0000-4000-8000-000000000001",
      ].join("\n"),
    );
  });

  it("creates HMAC SHA-256 base64url signatures with the API key as secret", async () => {
    const input = {
      method: "POST",
      path: "/api/v1/partner/payments",
      body: "{\"amount\":\"1.00\"}",
      timestamp: "1710000000",
      nonce: "nonce-123456789012",
      keyId: "00000000-0000-4000-8000-000000000001",
      secret: "sgw_local_prefix_secret",
    };
    const result = await signSentraRequest(input);
    const expected = createHmac("sha256", input.secret)
      .update(result.canonical)
      .digest("base64url");

    expect(result.signature).toBe(expected);
    expect(result.headers["X-Sentra-Signature"]).toBe(expected);
  });

  it("normalizes paths and queries like the gateway verifier", () => {
    expect(normalizePath("/a/b/../c")).toBe("/a/c");
    expect(canonicalQuery("z=last&name=Omar+Hassan&name=A")).toBe(
      "name=A&name=Omar%20Hassan&z=last",
    );
  });
});
