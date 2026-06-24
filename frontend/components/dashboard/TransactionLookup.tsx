"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";

interface TransactionLookupProps {
  onSearch: (ids: string[]) => void;
}

const MAX_IDS = 200;
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function parseIds(raw: string): { ids: string[]; tooMany: boolean } {
  const all = [
    ...new Set(
      raw
        .split(/[\n,;]+/)
        .map((s) => s.trim())
        .filter((s) => UUID_RE.test(s))
    ),
  ];
  return { ids: all.slice(0, MAX_IDS), tooMany: all.length > MAX_IDS };
}

export function TransactionLookup({ onSearch }: TransactionLookupProps) {
  const [raw, setRaw] = useState("");
  const [warning, setWarning] = useState<string | null>(null);

  function handleSearch() {
    const { ids, tooMany } = parseIds(raw);
    setWarning(tooMany ? `Limite de ${MAX_IDS} IDs atingido. Os primeiros ${MAX_IDS} foram carregados.` : null);
    onSearch(ids);
  }

  return (
    <div className="space-y-2 mb-6">
      <Label htmlFor="tx-ids">IDs de transação</Label>
      <textarea
        id="tx-ids"
        rows={3}
        placeholder="Cole um ou mais IDs (UUID) separados por vírgula ou nova linha..."
        value={raw}
        onChange={(e) => setRaw(e.target.value)}
        className="w-full rounded-input border border-border bg-white px-3 py-2 text-sm text-text-primary placeholder:text-text-secondary resize-none focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary"
      />
      {warning && <p className="text-xs text-warning">{warning}</p>}
      <Button
        onClick={handleSearch}
        disabled={!raw.trim()}
        className="bg-primary hover:bg-primary/90 text-white rounded-btn"
      >
        Buscar
      </Button>
    </div>
  );
}
