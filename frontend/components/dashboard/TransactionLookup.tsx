"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";

interface TransactionLookupProps {
  onSearch: (ids: string[]) => void;
}

function parseIds(raw: string): string[] {
  return [
    ...new Set(
      raw
        .split(/[\n,;]+/)
        .map((s) => s.trim())
        .filter(Boolean)
    ),
  ];
}

export function TransactionLookup({ onSearch }: TransactionLookupProps) {
  const [raw, setRaw] = useState("");

  function handleSearch() {
    const ids = parseIds(raw);
    onSearch(ids);
  }

  return (
    <div className="space-y-2 mb-6">
      <Label htmlFor="tx-ids">IDs de transação</Label>
      <textarea
        id="tx-ids"
        rows={3}
        placeholder="Cole um ou mais IDs separados por vírgula ou nova linha..."
        value={raw}
        onChange={(e) => setRaw(e.target.value)}
        className="w-full rounded-input border border-border bg-white px-3 py-2 text-sm text-text-primary placeholder:text-text-secondary resize-none focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary"
      />
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
