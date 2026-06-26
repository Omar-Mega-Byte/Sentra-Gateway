"use client";

import { useSyncExternalStore } from "react";

export type AuthMode = "none" | "jwt" | "apiKey" | "basic";

export type AuthState = {
  authMode: AuthMode;
  jwt: string;
  apiKey: string;
  apiKeyId: string;
  basicUsername: string;
  basicPassword: string;
  requestIdPrefix: string;
};

const STORAGE_KEY = "sentra-console-auth";

const defaultState: AuthState = {
  authMode: "none",
  jwt: "",
  apiKey: "",
  apiKeyId: "",
  basicUsername: "admin",
  basicPassword: "sentra-admin",
  requestIdPrefix: "sentra-console",
};

let snapshot: AuthState = defaultState;
let hydrated = false;
const listeners = new Set<() => void>();

function getSnapshot(): AuthState {
  return snapshot;
}

function getServerSnapshot(): AuthState {
  return defaultState;
}

function hydrateFromStorage() {
  if (hydrated || typeof window === "undefined") {
    return;
  }
  hydrated = true;
  const raw = window.localStorage.getItem(STORAGE_KEY);
  let next = defaultState;
  if (!raw) {
    next = defaultState;
  } else {
    try {
      next = { ...defaultState, ...JSON.parse(raw) };
    } catch {
      next = defaultState;
    }
  }

  if (JSON.stringify(next) !== JSON.stringify(snapshot)) {
    snapshot = next;
    listeners.forEach((listener) => listener());
  }
}

function writeState(next: AuthState) {
  snapshot = next;
  if (typeof window !== "undefined") {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  }
  listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  hydrateFromStorage();
  return () => listeners.delete(listener);
}

export function useAuthStore() {
  const state = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);

  return {
    ...state,
    setAuth: (patch: Partial<AuthState>) => writeState({ ...state, ...patch }),
    clearAuth: () => writeState(defaultState),
  };
}

export function currentAuthState() {
  hydrateFromStorage();
  return snapshot;
}

export function buildAuthHeaders(mode: AuthMode, state: AuthState) {
  const headers: Record<string, string> = {};
  if (mode === "jwt" && state.jwt.trim()) {
    headers.Authorization = `Bearer ${state.jwt.trim()}`;
  }
  if (mode === "apiKey" && state.apiKey.trim()) {
    headers["X-API-Key"] = state.apiKey.trim();
  }
  if (mode === "basic" && state.basicUsername.trim()) {
    const value = `${state.basicUsername}:${state.basicPassword}`;
    headers.Authorization = `Basic ${btoa(value)}`;
  }
  return headers;
}

export function requestId(prefix: string) {
  const cleaned = prefix.trim() || "sentra-console";
  return `${cleaned}-${Date.now().toString(36)}`;
}

export type JwtPreview = {
  subject: string;
  roles: string[];
  scopes: string[];
  tenantId: string;
  claims: Record<string, unknown>;
  valid: boolean;
  error?: string;
};

export function previewJwt(token: string): JwtPreview {
  if (!token.trim()) {
    return {
      subject: "",
      roles: [],
      scopes: [],
      tenantId: "",
      claims: {},
      valid: false,
      error: "empty",
    };
  }
  try {
    const [, payload] = token.split(".");
    const claims = JSON.parse(base64UrlDecode(payload)) as Record<string, unknown>;
    const roles = new Set<string>();
    collectStringList(claims.roles).forEach((role) => roles.add(role));
    collectKeycloakRoles(claims).forEach((role) => roles.add(role));
    const scopes = new Set<string>();
    collectStringList(claims.scope).forEach((scope) => scopes.add(scope));
    collectStringList(claims.scp).forEach((scope) => scopes.add(scope));

    return {
      subject: stringValue(claims.sub),
      roles: Array.from(roles).filter(Boolean).sort(),
      scopes: Array.from(scopes).filter(Boolean).sort(),
      tenantId: stringValue(claims.tenant_id ?? claims.tenant),
      claims,
      valid: true,
    };
  } catch (error) {
    return {
      subject: "",
      roles: [],
      scopes: [],
      tenantId: "",
      claims: {},
      valid: false,
      error: error instanceof Error ? error.message : "invalid",
    };
  }
}

function base64UrlDecode(value: string) {
  const padded = `${value}${"=".repeat((4 - (value.length % 4)) % 4)}`;
  const normalized = padded.replace(/-/g, "+").replace(/_/g, "/");
  return decodeURIComponent(
    Array.from(atob(normalized))
      .map((character) => `%${character.charCodeAt(0).toString(16).padStart(2, "0")}`)
      .join(""),
  );
}

function collectStringList(value: unknown): string[] {
  if (Array.isArray(value)) {
    return value.map(String);
  }
  if (typeof value === "string") {
    return value.split(/[ ,]/).filter(Boolean);
  }
  return [];
}

function collectKeycloakRoles(claims: Record<string, unknown>): string[] {
  const roles: string[] = [];
  const realmAccess = claims.realm_access;
  if (isRecord(realmAccess)) {
    roles.push(...collectStringList(realmAccess.roles));
  }
  const resourceAccess = claims.resource_access;
  if (isRecord(resourceAccess)) {
    Object.values(resourceAccess).forEach((resource) => {
      if (isRecord(resource)) {
        roles.push(...collectStringList(resource.roles));
      }
    });
  }
  return roles;
}

function stringValue(value: unknown) {
  return typeof value === "string" ? value : "";
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
