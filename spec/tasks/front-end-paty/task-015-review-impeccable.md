---
id: task-015
status: planned
links:
  - spec/tech-plans/plan-006-frontend.md
  - spec/specs/spec-006-frontend-checkout.md
  - spec/specs/spec-007-merchant-dashboard.md
---

# Review All Screens with Impeccable Skill

Executar o skill `impeccable` em todas as telas implementadas e aplicar as melhorias identificadas.

## Local Context

**Screens to review:**
- `/checkout` — all 5 states: FORM, PROCESSING, THREE_DS, SUCCESS, ERROR
- `/login` — API key entry form
- `/dashboard` — transaction list with data, empty state, loading state
- `/dashboard/transactions/[id]` — detail view for COMPLETED and UNKNOWN transactions

**Key files to check:**
- `frontend/components/checkout/*.tsx`
- `frontend/components/dashboard/*.tsx`
- `frontend/components/shared/*.tsx`
- `frontend/app/globals.css`
- `frontend/tailwind.config.ts`

**Local dependencies:** All components from tasks 001–014 must be complete.

## Scope

1. **Run `impeccable` skill** targeting the frontend application.

2. **Spacing audit** — verify design.json `spacingRules` applied throughout:
   - `betweenSections: 120px` — page-level section gaps
   - `betweenCards: 24px` — card-to-card spacing
   - `betweenInputs: 16px` — form field spacing
   - `betweenLabelAndInput: 8px` — label margin-bottom
   - `betweenHeadingAndText: 16px` — heading to body text gap
   - `betweenTextAndCTA: 32px` — last text element to button gap

3. **Typography audit** — verify design.json sizes applied:
   - `hero: 64px/700` — only if used
   - `h2: 40px/600`
   - `h3: 20px/600` — checkout confirmation heading
   - `body: 16px/400/1.7` — all descriptive text
   - `label: 13px/500` — form labels, security badge
   - `helperText: 12px/400` — error messages, helper text

4. **Color audit** — no hardcoded hex values outside `tailwind.config.ts`; all elements use token classes.

5. **Responsiveness audit** — test at:
   - 375px (iPhone SE): no horizontal scroll, no overlapping elements, checkout card fits screen
   - 768px (tablet): dashboard table vs card view breakpoint
   - 1200px (desktop): maxWidth container centering

6. **Accessibility audit:**
   - Focus rings visible on all interactive elements
   - ARIA labels on all icon-only buttons (CopyButton, Refresh, sort column headers)
   - `<table>` has `<th scope="col">` headers
   - Color contrast ≥ 4.5:1 for all text (StatusBadge colors verified in task-013)
   - Form error messages linked to inputs via `aria-describedby`

7. **State coverage audit:**
   - Empty states present for: no transactions loaded, no filter matches
   - Loading states present for: initial checkout, polling, dashboard table, detail fetch
   - Error states present for: all 4 checkout error types, API fetch failure in detail

8. **Apply all improvements** identified in the review before committing.

## Acceptance Criteria and Tests

- Success: impeccable review completes with no Critical findings.
- Success: All spacing values match design.json `spacingRules` (measure in DevTools).
- Success: Typography scale matches design.json exactly.
- Success: No hardcoded hex colors found by `grep -r "style=" frontend/app frontend/components`.
- Success: All interactive elements keyboard-navigable (Tab through entire checkout form manually).
- Success: Checkout renders at 375px without horizontal scrollbar.
- Success: Dashboard table becomes card list below 768px.
- Failure: Any WCAG AA contrast failure → fix colors or add text shadow.
- Tests: Browser DevTools accessibility audit (Lighthouse); visual inspection at 3 breakpoints.

## Constraints and Negative Instructions

- Do NOT introduce new color values not in `design.json`; fix contrast by adjusting opacity of existing tokens.
- Do NOT add CSS animations beyond what is already specified (design.json `animations.avoid` includes parallax, aggressive motion).
- Do NOT change component APIs during review — only visual and accessibility fixes.
- Improvements must NOT break existing e2e flows verified in tasks 011 and 014.
