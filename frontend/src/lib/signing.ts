export type SigningInput = {
  method: string;
  path: string;
  query?: string;
  body?: string;
  timestamp: string;
  nonce: string;
  keyId: string;
  secret: string;
};

export type SigningResult = {
  canonical: string;
  signature: string;
  headers: Record<string, string>;
};

const encoder = new TextEncoder();

export async function signSentraRequest(
  input: SigningInput,
): Promise<SigningResult> {
  const canonical = await canonicalString(input);
  const signature = await hmacBase64Url(input.secret, canonical);
  return {
    canonical,
    signature,
    headers: {
      "X-Sentra-Key-Id": input.keyId,
      "X-Sentra-Timestamp": input.timestamp,
      "X-Sentra-Nonce": input.nonce,
      "X-Sentra-Signature": signature,
    },
  };
}

export async function canonicalString(input: Omit<SigningInput, "secret">) {
  return [
    input.method.toUpperCase(),
    normalizePath(input.path),
    canonicalQuery(input.query ?? ""),
    await sha256Hex(input.body ?? ""),
    input.timestamp,
    input.nonce,
    input.keyId,
  ].join("\n");
}

export function canonicalQuery(rawQuery: string) {
  const query = rawQuery.startsWith("?") ? rawQuery.slice(1) : rawQuery;
  if (!query) return "";
  return query
    .split("&")
    .map((pair) => {
      const [name, value = ""] = pair.split("=", 2);
      return {
        name: encodeQueryValue(decodeQueryValue(name)),
        value: encodeQueryValue(decodeQueryValue(value)),
      };
    })
    .sort((left, right) =>
      left.name === right.name
        ? left.value.localeCompare(right.value)
        : left.name.localeCompare(right.name),
    )
    .map((part) => `${part.name}=${part.value}`)
    .join("&");
}

export function normalizePath(rawPath: string) {
  const path = rawPath.trim() || "/";
  const segments: string[] = [];
  path.split("/").forEach((segment) => {
    if (!segment || segment === ".") return;
    if (segment === "..") {
      segments.pop();
      return;
    }
    segments.push(segment);
  });
  return `/${segments.join("/")}`;
}

export function nonce() {
  const values = new Uint8Array(18);
  globalThis.crypto.getRandomValues(values);
  return Array.from(values, (value) => value.toString(16).padStart(2, "0")).join("");
}

export function epochSeconds() {
  return Math.floor(Date.now() / 1000).toString();
}

async function sha256Hex(value: string) {
  const digest = await globalThis.crypto.subtle.digest("SHA-256", encoder.encode(value));
  return Array.from(new Uint8Array(digest), (byte) =>
    byte.toString(16).padStart(2, "0"),
  ).join("");
}

async function hmacBase64Url(secret: string, value: string) {
  const key = await globalThis.crypto.subtle.importKey(
    "raw",
    encoder.encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await globalThis.crypto.subtle.sign(
    "HMAC",
    key,
    encoder.encode(value),
  );
  return bytesToBase64Url(new Uint8Array(signature));
}

function bytesToBase64Url(bytes: Uint8Array) {
  let binary = "";
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function decodeQueryValue(value: string) {
  return decodeURIComponent(value.replace(/\+/g, " "));
}

function encodeQueryValue(value: string) {
  return encodeURIComponent(value).replace(/%7E/g, "~");
}
