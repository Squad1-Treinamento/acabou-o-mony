---
id: task-002
status: planned
links:
  - spec/tech-plans/plan-006-frontend.md
  - spec/specs/spec-006-frontend-checkout.md
---

# Implement Design System Tokens in Tailwind and Global CSS

Mapear todos os tokens de `design.json` para o tema Tailwind, declarar CSS custom properties em `globals.css` e configurar a fonte Inter no root layout.

## Local Context

**Files to modify:**
- `frontend/tailwind.config.ts`
- `frontend/app/globals.css`
- `frontend/app/layout.tsx`

**Source of truth:** `design.json` at repo root (read before editing).

**Local dependencies:**
- `next/font/google` (Inter — built into Next.js, no extra install)

## Scope

1. Read `design.json` in full to extract all token values before editing any file.
2. Update `frontend/tailwind.config.ts` — extend `theme.extend` with:
   - **colors:** `primary=#0D2B1E`, `primaryHover=#123726`, `background=#F5F6F7`, `surface=#FFFFFF`, `surfaceSecondary=#FAFAFA`, `success=#1F8F53`, `successSoft=#EAF7F0`, `error=#DC2626`, `warning=#D97706`, `textPrimary=#111827`, `textSecondary=#6B7280`, `border=#E5E7EB`, `divider=#F0F2F4`, `inputBackground=#FFFFFF`, `inputBorder=#DADDE2`
   - **borderRadius:** `card: '16px'`, `btn: '10px'`, `input: '8px'`
   - **fontFamily:** `sans: ['Inter', 'Geist', 'SF Pro Display', 'system-ui']`
   - **height:** `btn: '52px'`, `input: '48px'`
3. Update `frontend/app/globals.css`:
   - Add `@tailwind base; @tailwind components; @tailwind utilities;` directives.
   - Declare CSS custom properties under `:root` mirroring design.json color tokens (e.g., `--color-primary: #0D2B1E`).
4. Update `frontend/app/layout.tsx`:
   - Import Inter via `next/font/google` with `subsets: ['latin']`.
   - Apply `inter.className` to `<html>`.
   - Set `lang="pt-BR"` on `<html>`.
   - Apply `className="bg-background text-textPrimary"` to `<body>`.

## Acceptance Criteria and Tests

- Success: `pnpm build` passes with no Tailwind or TypeScript errors.
- Success: A test div with `className="bg-primary text-surface"` renders dark green background with white text in the browser.
- Success: Inter font loads (visible in browser DevTools → Network → Fonts).
- Success: All 14 color tokens, 3 borderRadius tokens, fontFamily, and height tokens are present in `tailwind.config.ts`.
- Failure: Any hardcoded hex value outside `tailwind.config.ts` → move to config.
- Tests: Manual browser check; no automated tests required for this task.

## Constraints and Negative Instructions

- Do NOT use inline `style={{}}` attributes anywhere.
- Do NOT add CSS that isn't derived from `design.json`; no one-off values.
- Do NOT use `@apply` with arbitrary values; use only the extended Tailwind theme.
- Do NOT change `next.config.ts` in this task.
- Do NOT modify files outside `frontend/`.
