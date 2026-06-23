---
id: task-016
status: planned
links:
  - spec/tech-plans/plan-006-frontend.md
---

# Create Semantic Commits for All Frontend Work

Organizar e criar commits semânticos por grupo lógico de mudanças usando o skill `create-commit`.

## Local Context

**Branch:** `front-end-paty` (already checked out)

**All modified/created files to commit** (group by scope):

Group 1 — docs:
- `spec/specs/spec-006-frontend-checkout.md`
- `spec/specs/spec-007-merchant-dashboard.md`
- `spec/specs/index.md`

Group 2 — adr:
- `spec/adrs/adr-004-frontend-architecture.md`
- `spec/adrs/index.md`

Group 3 — tech-plan:
- `spec/tech-plans/plan-006-frontend.md`
- `spec/tech-plans/index.md`

Group 4 — tasks:
- `spec/tasks/front-end-paty/task-001-*.md` through `task-016-*.md`
- `spec/tasks/index.md`

Group 5 — scaffold:
- `frontend/package.json`, `frontend/pnpm-lock.yaml`, `frontend/tsconfig.json`, `frontend/next.config.ts`, `frontend/.env.local.example`, `frontend/.gitignore`

Group 6 — design-system:
- `frontend/tailwind.config.ts`, `frontend/app/globals.css`, `frontend/app/layout.tsx`, `frontend/app/providers.tsx`

Group 7 — components:
- `frontend/components/ui/*.tsx` (all shadcn components)
- `frontend/components/shared/*.tsx` (all shared components)

Group 8 — api:
- `frontend/types/payment.ts`, `frontend/types/merchant.ts`
- `frontend/lib/api/client.ts`, `frontend/lib/api/payments.ts`
- `frontend/lib/utils/formatters.ts`, `frontend/lib/utils/idempotency.ts`
- `frontend/hooks/usePayment.ts`

Group 9 — checkout:
- `frontend/components/checkout/*.tsx`
- `frontend/app/(checkout)/checkout/page.tsx`

Group 10 — dashboard:
- `frontend/components/dashboard/*.tsx`
- `frontend/hooks/useTransactions.ts`
- `frontend/app/(dashboard)/login/page.tsx`
- `frontend/app/(dashboard)/dashboard/page.tsx`
- `frontend/app/(dashboard)/dashboard/transactions/[id]/page.tsx`

Group 11 — review fixes:
- Any files modified during task-015 impeccable review

## Scope

1. Run `git status` to confirm current state of all files.
2. Use the `create-commit` skill for each group below, staging only the files in that group:

   ```
   docs(spec): add frontend checkout and merchant dashboard specifications
   docs(adr): add ADR-004 frontend architecture decision record
   docs(tech-plan): add plan-006 frontend implementation tech plan
   docs(tasks): add 16 execution tasks for front-end-paty branch
   feat(scaffold): initialize Next.js 14 frontend with pnpm and TypeScript
   feat(design-system): implement design.json tokens in Tailwind and global CSS
   feat(components): add shadcn/ui base components and shared UI components
   feat(api): add payment API client, TypeScript types, and utility functions
   feat(checkout): implement multi-step checkout flow with 3DS support
   feat(dashboard): implement merchant transaction dashboard with lookup and detail
   fix(ui): apply impeccable review improvements to spacing, a11y, and typography
   ```

3. Verify `git log --oneline -15` shows all commits in correct order.

## Acceptance Criteria and Tests

- Success: 11 commits created, each covering exactly its declared scope.
- Success: `git log --oneline -15` shows all commits in the correct order.
- Success: No `.env.local` file committed (verify with `git show HEAD:frontend/.env.local` returns error).
- Success: Each commit message follows `type(scope): description` format.
- Failure: Any commit contains files from multiple logical groups → split into separate commits.
- Tests: `git log` review; `git diff HEAD~11..HEAD --stat` to confirm file coverage.

## Constraints and Negative Instructions

- Do NOT use `git add .` or `git add -A`; stage only specific files per commit to avoid accidental inclusion of `.env.local` or other sensitive files.
- Do NOT amend or squash commits after creation without explicit user approval.
- Do NOT push to remote; `git push` requires explicit user approval.
- Do NOT skip the `--no-verify` flag unless a hook failure is diagnosed and fixed.
- Each commit must pass `pnpm build` at the time it is created (the build must not be broken mid-series).
