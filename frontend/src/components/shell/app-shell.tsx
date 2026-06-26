"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Activity,
  BookOpen,
  Braces,
  ClipboardList,
  Gauge,
  KeyRound,
  LayoutDashboard,
  LockKeyhole,
  Network,
  Route,
  Shield,
  Signature,
  Users,
  Waypoints,
} from "lucide-react";
import { ReactNode } from "react";
import { useAuthStore } from "@/lib/auth-store";

const navItems = [
  { href: "/", label: "Dashboard", icon: LayoutDashboard },
  { href: "/auth", label: "Authentication", icon: LockKeyhole },
  { href: "/routes", label: "Routes", icon: Route },
  { href: "/clients", label: "API Clients", icon: Users },
  { href: "/api-keys", label: "API Keys", icon: KeyRound },
  { href: "/rate-limits", label: "Rate Limits", icon: Gauge },
  { href: "/ip-rules", label: "IP Rules", icon: Network },
  { href: "/signing", label: "Signing", icon: Signature },
  { href: "/risk-rules", label: "Risk Rules", icon: Shield },
  { href: "/audit", label: "Audit Logs", icon: ClipboardList },
  { href: "/observability", label: "Observability", icon: Activity },
  { href: "/explorer", label: "API Explorer", icon: Braces },
] as const;

const serviceItems = [
  { href: "/services/user", label: "User Service" },
  { href: "/services/order", label: "Order Service" },
  { href: "/services/payment", label: "Payment Service" },
  { href: "/services/notification", label: "Notification Service" },
] as const;

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const { authMode, jwt, apiKey, basicUsername } = useAuthStore();

  return (
    <div className="min-h-screen bg-slate-50">
      <aside className="fixed inset-y-0 left-0 hidden w-64 border-r border-slate-200 bg-white lg:block">
        <div className="flex h-16 items-center border-b border-slate-200 px-5">
          <div className="flex h-9 w-9 items-center justify-center rounded-md border border-slate-200 bg-slate-50">
            <Waypoints className="h-5 w-5 text-slate-800" aria-hidden="true" />
          </div>
          <div className="ml-3 min-w-0">
            <p className="truncate text-sm font-semibold text-slate-950">
              Sentra Gateway
            </p>
            <p className="truncate text-xs text-slate-500">Enterprise API</p>
          </div>
        </div>
        <nav className="h-[calc(100vh-4rem)] overflow-y-auto px-3 py-4">
          <div className="space-y-1">
            {navItems.map((item) => (
              <NavLink
                key={item.href}
                active={pathname === item.href}
                href={item.href}
                label={item.label}
                icon={item.icon}
              />
            ))}
          </div>
          <div className="mt-6 border-t border-slate-200 pt-4">
            <div className="mb-2 flex items-center gap-2 px-3 text-xs font-medium uppercase tracking-wide text-slate-500">
              <BookOpen className="h-3.5 w-3.5" aria-hidden="true" />
              Services
            </div>
            <div className="space-y-1">
              {serviceItems.map((item) => (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`block rounded-md px-3 py-2 text-sm transition ${
                    pathname === item.href
                      ? "bg-slate-900 text-white"
                      : "text-slate-600 hover:bg-slate-100 hover:text-slate-950"
                  }`}
                >
                  {item.label}
                </Link>
              ))}
            </div>
          </div>
        </nav>
      </aside>

      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/95 backdrop-blur">
          <div className="flex min-h-16 items-center justify-between gap-3 px-4 py-3 lg:px-8">
            <div className="min-w-0">
              <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                Console
              </p>
              <h1 className="truncate text-lg font-semibold text-slate-950">
                {titleForPath(pathname)}
              </h1>
            </div>
            <div className="flex flex-wrap justify-end gap-2 text-xs">
              <ShellBadge label="Auth" value={authMode.toUpperCase()} />
              <ShellBadge
                label="JWT"
                value={jwt ? "set" : "empty"}
                tone={jwt ? "ok" : "muted"}
              />
              <ShellBadge
                label="Key"
                value={apiKey ? "set" : "empty"}
                tone={apiKey ? "ok" : "muted"}
              />
              <ShellBadge
                label="Basic"
                value={basicUsername || "empty"}
                tone={basicUsername ? "ok" : "muted"}
              />
            </div>
          </div>
          <div className="flex gap-2 overflow-x-auto border-t border-slate-100 px-4 py-2 lg:hidden">
            {navItems.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className={`shrink-0 rounded-md px-3 py-1.5 text-sm ${
                  pathname === item.href
                    ? "bg-slate-900 text-white"
                    : "bg-white text-slate-600"
                }`}
              >
                {item.label}
              </Link>
            ))}
          </div>
        </header>
        <main className="px-4 py-6 lg:px-8">{children}</main>
      </div>
    </div>
  );
}

function NavLink({
  href,
  label,
  active,
  icon: Icon,
}: {
  href: string;
  label: string;
  active: boolean;
  icon: typeof LayoutDashboard;
}) {
  return (
    <Link
      href={href}
      className={`flex items-center gap-3 rounded-md px-3 py-2 text-sm transition ${
        active
          ? "bg-slate-900 text-white"
          : "text-slate-600 hover:bg-slate-100 hover:text-slate-950"
      }`}
    >
      <Icon className="h-4 w-4" aria-hidden="true" />
      <span>{label}</span>
    </Link>
  );
}

function ShellBadge({
  label,
  value,
  tone = "muted",
}: {
  label: string;
  value: string;
  tone?: "ok" | "muted";
}) {
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-md border px-2 py-1 ${
        tone === "ok"
          ? "border-emerald-200 bg-emerald-50 text-emerald-700"
          : "border-slate-200 bg-slate-50 text-slate-600"
      }`}
    >
      <span className="text-slate-500">{label}</span>
      <span className="font-medium">{value}</span>
    </span>
  );
}

function titleForPath(pathname: string) {
  if (pathname === "/") return "Dashboard";
  const first = pathname.split("/").filter(Boolean).at(-1) ?? "dashboard";
  return first
    .split("-")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}
