---
id: task-001
status: planned
links:
  - spec/tech-plans/plan-006-frontend.md
  - spec/adrs/adr-004-frontend-architecture.md
---

# Scaffold Next.js 14 Project

Inicializar o projeto frontend em `frontend/` na raiz do repositório com Next.js 14, TypeScript, Tailwind, shadcn/ui e dependências base.

## Local Context

**Files to create:**
- `frontend/` (directory at repo root)
- `frontend/package.json`
- `frontend/tsconfig.json`
- `frontend/next.config.ts`
- `frontend/.env.local.example`
- `frontend/.gitignore` entry for `.env.local`

**Commands to run (inside `frontend/`):**
```
pnpm create next-app@latest . --typescript --tailwind --eslint --app --src-dir=no --import-alias="@/*" --no-git
pnpm add @tanstack/react-query react-hook-form zod lucide-react
pnpm dlx shadcn@latest init
```
shadcn init options: style=default, base color=slate, CSS variables=yes.

## Scope

1. Create `frontend/` directory at repo root (sibling to `core-payment/` and `3ds-engine/`).
2. Run `pnpm create next-app` with flags above inside `frontend/`.
3. Install additional dependencies: `@tanstack/react-query`, `react-hook-form`, `zod`, `lucide-react`.
4. Run `pnpm dlx shadcn@latest init` with options: default style, slate base color, CSS variables enabled.
5. Create `frontend/.env.local.example`:
   ```
   NEXT_PUBLIC_API_URL=http://localhost:8080
   NEXT_PUBLIC_CHECKOUT_RETURN_URL=http://localhost:3000/checkout
   ```
6. Add `.env.local` to `frontend/.gitignore` (do not commit real env values).
7. Verify `pnpm dev` starts on `:3000` and the default Next.js page loads.

## Acceptance Criteria and Tests

- Success: `pnpm dev` starts without errors; `http://localhost:3000` responds with a page.
- Success: `pnpm build` completes without TypeScript errors.
- Success: `frontend/package.json` lists all required dependencies.
- Success: `.env.local.example` exists with both env vars.
- Failure: any TypeScript compilation error on fresh scaffold → fix before proceeding.
- Tests: none required for scaffold; manual browser verification is sufficient.

## Constraints and Negative Instructions

- Do NOT use `npm` or `yarn`; use `pnpm` exclusively.
- Do NOT use `--src-dir` (no `src/` wrapper); files live directly in `frontend/`.
- Do NOT commit `.env.local` (contains secrets in real environments).
- Do NOT install `axios`; use native `fetch`.
- Do NOT install Redux, Zustand, or any global state library.
- Do NOT modify any file outside `frontend/`.
