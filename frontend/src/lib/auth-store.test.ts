import { describe, expect, it } from "vitest";
import { buildAuthHeaders, previewJwt, type AuthState } from "@/lib/auth-store";

const state: AuthState = {
  authMode: "none",
  jwt: "jwt-token",
  apiKey: "sgw_local_key_secret",
  apiKeyId: "00000000-0000-4000-8000-000000000001",
  basicUsername: "admin",
  basicPassword: "sentra-admin",
  requestIdPrefix: "sentra-console",
};

describe("auth helpers", () => {
  it("builds only the selected credential header", () => {
    expect(buildAuthHeaders("jwt", state)).toEqual({
      Authorization: "Bearer jwt-token",
    });
    expect(buildAuthHeaders("apiKey", state)).toEqual({
      "X-API-Key": "sgw_local_key_secret",
    });
    expect(buildAuthHeaders("none", state)).toEqual({});
  });

  it("previews JWT roles, scopes, tenant and subject", () => {
    const claims = {
      sub: "sentra-user-omar",
      scope: "profile:read orders:read",
      roles: ["USER_ADMIN"],
      tenant_id: "tenant-demo",
    };
    const token = `x.${Buffer.from(JSON.stringify(claims)).toString("base64url")}.y`;
    const preview = previewJwt(token);

    expect(preview.valid).toBe(true);
    expect(preview.subject).toBe("sentra-user-omar");
    expect(preview.scopes).toContain("profile:read");
    expect(preview.roles).toContain("USER_ADMIN");
    expect(preview.tenantId).toBe("tenant-demo");
  });
});
