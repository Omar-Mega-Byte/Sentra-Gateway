"use client";

import { Copy, Check } from "lucide-react";
import { useState } from "react";
import { Button, Card } from "@/components/ui/primitives";

export function JsonPanel({
  title,
  value,
  dense = false,
}: {
  title: string;
  value: unknown;
  dense?: boolean;
}) {
  const [copied, setCopied] = useState(false);
  const text =
    typeof value === "string" ? value : JSON.stringify(value ?? null, null, 2);

  return (
    <Card>
      <div className="flex items-center justify-between gap-3 border-b border-slate-100 px-4 py-2">
        <h3 className="text-sm font-semibold text-slate-950">{title}</h3>
        <Button
          type="button"
          variant="ghost"
          className="h-8 px-2"
          onClick={async () => {
            await navigator.clipboard.writeText(text);
            setCopied(true);
            window.setTimeout(() => setCopied(false), 1200);
          }}
          title="Copy"
        >
          {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
        </Button>
      </div>
      <pre
        className={`overflow-auto whitespace-pre-wrap break-words p-4 text-xs leading-5 text-slate-700 ${
          dense ? "max-h-64" : "max-h-[30rem]"
        }`}
      >
        {text}
      </pre>
    </Card>
  );
}
