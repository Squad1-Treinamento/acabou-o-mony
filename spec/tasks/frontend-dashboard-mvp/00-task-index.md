# Frontend Dashboard MVP - Task Index

**Branch**: `frontend-dashboard-mvp`  
**Status**: Ready to implement  
**Total Estimated Time**: 4-6 hours

---

## Quick Start

1. **Read**: `README.md` (overview and context)
2. **Follow**: Tasks 01-06 in order
3. **Validate**: Run tests after each task
4. **Deploy**: Test complete flow before merging

---

## Task List

### Task 01: Create Dashboard Component (2-3 hours)
**File**: `01-dashboard-component.md`

Build the main Dashboard with:
- 4 KPI cards (Revenue, Transactions, Success Rate, Average)
- Recent transactions table (last 5)
- Quick actions section
- Metrics calculation from API data

**Dependencies**: None (uses existing API client)  
**Output**: `Dashboard.tsx`, `KPICard.tsx`

---

### Task 02: Update Navigation Structure (1 hour)
**File**: `02-navigation-structure.md`

Reorganize navigation to separate merchant features from developer tools:
- Add Dashboard tab (default)
- Keep Transactions tab
- Move Create Payment to Developer Tools submenu
- Add collapsible Developer Tools section

**Dependencies**: Task 01  
**Output**: Modified `App.tsx`

---

### Task 03: Add Warning Banners to Developer Tools (30 min)
**File**: `03-developer-tools-warnings.md`

Add warning banners to indicate developer-only features:
- Create reusable DevToolsWarning component
- Add to Create Payment page
- Add to Idempotency Test page

**Dependencies**: Task 02  
**Output**: `DevToolsWarning.tsx`, modified `PaymentForm.tsx`

---

### Task 04: Extract Idempotency Test Component (1 hour)
**File**: `04-idempotency-test-extraction.md`

Separate idempotency testing from PaymentForm:
- Extract advanced mode to standalone component
- Simplify PaymentForm (remove advanced mode)
- Create IdempotencyTest with side-by-side comparison

**Dependencies**: Task 03  
**Output**: `IdempotencyTest.tsx`, simplified `PaymentForm.tsx`

---

### Task 05: Add Helper Functions for Metrics (30 min)
**File**: `05-metrics-utilities.md`

Create utility functions for dashboard calculations:
- Filter transactions by date
- Calculate revenue, success rate, average
- Calculate trends (today vs yesterday)
- Format currency and relative time

**Dependencies**: None (standalone utilities)  
**Output**: `utils/metrics.ts`

---

### Task 06: Update Default Landing Page (15 min)
**File**: `06-default-landing-page.md`

Change default tab from "Create Payment" to "Dashboard":
- Update initial state in App.tsx
- Test login flow
- Verify Dashboard appears first

**Dependencies**: Task 01, Task 02  
**Output**: Modified `App.tsx` (one line change)

---

## Recommended Implementation Order

### Option A: Linear (Safest)
Follow tasks 01 → 02 → 03 → 04 → 05 → 06 in order.

**Pros**: Clear dependencies, easy to track progress  
**Cons**: Can't parallelize work

### Option B: Parallel (Faster)
1. **First**: Task 05 (metrics utilities - no dependencies)
2. **Then**: Task 01 (Dashboard - uses Task 05)
3. **Then**: Task 02 (Navigation - uses Task 01)
4. **Then**: Task 03 + Task 04 (Developer tools - independent)
5. **Finally**: Task 06 (Default page - uses Task 01 + 02)

**Pros**: Faster completion  
**Cons**: Requires careful coordination

### Option C: MVP-First (Recommended)
1. **Phase 1 (Core)**: Task 05 → Task 01 → Task 02 → Task 06
   - Result: Working dashboard as default page
2. **Phase 2 (Polish)**: Task 03 → Task 04
   - Result: Developer tools cleaned up

**Pros**: Get core value quickly, polish later  
**Cons**: Developer tools temporarily messy

---

## Validation After Each Task

```bash
# Type checking
npm run type-check

# Linting
npm run lint

# Development server
npm run dev

# Manual testing in browser
# - Check console for errors
# - Test functionality
# - Verify acceptance criteria
```

---

## Complete Acceptance Criteria

### Dashboard
- [ ] Dashboard is default landing page after login
- [ ] Shows 4 KPI cards with correct data
- [ ] KPIs calculate from real transaction data
- [ ] Shows trend indicators (up/down arrows)
- [ ] Shows last 5 recent transactions
- [ ] "View All" button navigates to Transactions
- [ ] Loading state while fetching data
- [ ] Error handling if API fails

### Navigation
- [ ] Dashboard tab is active by default
- [ ] Transactions tab works as before
- [ ] Developer Tools is collapsible (collapsed by default)
- [ ] Clicking Developer Tools expands submenu
- [ ] Submenu shows "Create Payment" and "Idempotency Test"
- [ ] Active tab is visually highlighted

### Developer Tools
- [ ] Warning banner appears on Create Payment
- [ ] Warning banner appears on Idempotency Test
- [ ] PaymentForm simplified (no advanced mode)
- [ ] IdempotencyTest is separate component
- [ ] Both tools function as before

### Metrics
- [ ] Revenue calculated correctly
- [ ] Success rate calculated correctly
- [ ] Trends compare today vs yesterday
- [ ] Empty state handled (no transactions)

---

## File Structure After Completion

```
frontend/src/
├── components/
│   ├── Dashboard.tsx              # NEW
│   ├── KPICard.tsx                # NEW
│   ├── DevToolsWarning.tsx        # NEW
│   ├── IdempotencyTest.tsx        # NEW
│   ├── LoginForm.tsx              # UNCHANGED
│   ├── PaymentForm.tsx            # MODIFIED
│   ├── TransactionList.tsx        # UNCHANGED
│   └── TransactionDetails.tsx     # UNCHANGED
├── context/
│   └── AuthContext.tsx            # UNCHANGED
├── services/
│   └── api.ts                     # UNCHANGED
├── types/
│   ├── auth.ts                    # UNCHANGED
│   └── payment.ts                 # UNCHANGED
├── utils/
│   ├── uuid.ts                    # UNCHANGED
│   └── metrics.ts                 # NEW
├── App.tsx                        # MODIFIED
├── index.css                      # UNCHANGED
└── main.tsx                       # UNCHANGED
```

---

## Common Issues & Solutions

### Issue: Dashboard shows "Loading..." forever
**Solution**: Check API client is configured, backend is running, CORS is enabled

### Issue: KPIs show 0 or NaN
**Solution**: Check transaction data format, verify metrics.ts calculations

### Issue: Navigation doesn't highlight active tab
**Solution**: Check activeTab state, verify className conditions

### Issue: Developer Tools submenu doesn't appear
**Solution**: Check showDevTools state, verify conditional rendering

### Issue: TypeScript errors in Dashboard
**Solution**: Check TransactionDetails type matches API response

---

## Testing Checklist

### Smoke Tests (Quick validation)
- [ ] Login works
- [ ] Dashboard appears
- [ ] KPIs show numbers
- [ ] Recent transactions display
- [ ] Navigation works
- [ ] Developer Tools expand/collapse
- [ ] No console errors

### Full Tests (Complete validation)
- [ ] All acceptance criteria met
- [ ] All edge cases handled
- [ ] All error states work
- [ ] All loading states work
- [ ] Responsive on mobile
- [ ] Accessible (keyboard navigation)

---

## Deployment Checklist

Before merging to main:
- [ ] All tasks completed
- [ ] All acceptance criteria met
- [ ] All tests passing
- [ ] No console errors
- [ ] No TypeScript errors
- [ ] Code reviewed (if team project)
- [ ] Documentation updated
- [ ] Screenshots taken (for portfolio)

---

## Next Steps After MVP

Once this MVP is complete, consider:

1. **Charts**: Add revenue chart (Chart.js or Recharts)
2. **Date Filters**: Add date range picker
3. **Export**: Add CSV export functionality
4. **Real-time**: Add WebSocket for live updates
5. **Settings**: Add API key management page
6. **Customers**: Add customer management page
7. **Refunds**: Add refund initiation UI
8. **Webhooks**: Add webhook configuration UI
9. **Analytics**: Add advanced analytics dashboard
10. **Tests**: Add unit and E2E tests

---

## Support

- **Main README**: `spec/tasks/frontend-dashboard-mvp/README.md`
- **Spec**: `spec/specs/frontend-web-app.md`
- **Backend Specs**: `spec/specs/spec-001-*.md` through `spec-005-*.md`
- **Original Tasks**: `spec/tasks/frontend-mvp/`

---

**Ready to start?** Begin with Task 01 or Task 05 (if you want to do utilities first).
