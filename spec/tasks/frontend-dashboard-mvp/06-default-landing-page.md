# Task 06: Update Default Landing Page

**Status**: Ready to implement  
**Estimated Time**: 15 minutes  
**Dependencies**: Task 01 (Dashboard component), Task 02 (Navigation structure)

---

## Goal

Change the default landing page from "Create Payment" to "Dashboard" after login. This makes the application feel like a real merchant platform where merchants see their business overview first.

---

## Context

**Current Behavior:**
- User logs in → sees "Create Payment" form
- This feels like a testing tool

**New Behavior:**
- User logs in → sees Dashboard with KPIs
- This feels like a merchant platform (like Stripe, Mercado Pago)

---

## What You're Changing

### Before
```typescript
const [activeTab, setActiveTab] = useState<Tab>('payment'); // OLD
```

### After
```typescript
const [activeTab, setActiveTab] = useState<Tab>('dashboard'); // NEW
```

---

## Files to Modify

### 1. `frontend/src/App.tsx`

Update the initial state of `activeTab`.

#### Changes Required:

**Line to Change:**
```typescript
// OLD
const [activeTab, setActiveTab] = useState<Tab>('payment');

// NEW
const [activeTab, setActiveTab] = useState<Tab>('dashboard');
```

That's it! Just one line change.

---

## Why This Matters

### User Experience Flow

**Before (Testing Tool):**
```
Login → Create Payment Form
         ↓
         "How do I see my transactions?"
         ↓
         Click "Transactions" tab
```

**After (Merchant Platform):**
```
Login → Dashboard (KPIs, Recent Transactions)
         ↓
         "Great! I can see my business at a glance"
         ↓
         Click "View All" to see more transactions
```

### First Impressions

The first screen a user sees sets expectations:
- **Create Payment first** = "This is a testing tool"
- **Dashboard first** = "This is a business platform"

---

## Acceptance Criteria

- [ ] After login, Dashboard is the active tab
- [ ] Dashboard content displays (KPIs, recent transactions)
- [ ] Dashboard tab is visually highlighted in navigation
- [ ] User can still navigate to other tabs
- [ ] No console errors
- [ ] No TypeScript errors

---

## Testing Checklist

### Manual Testing

1. **Login Flow**
   - [ ] Open application
   - [ ] Enter mock credentials
   - [ ] Click "Login"
   - [ ] **Verify**: Dashboard appears (not Create Payment)
   - [ ] **Verify**: Dashboard tab is highlighted in navigation

2. **Navigation**
   - [ ] Click "Transactions" tab
   - [ ] **Verify**: Transactions page appears
   - [ ] Click "Dashboard" tab
   - [ ] **Verify**: Dashboard appears again

3. **Developer Tools**
   - [ ] Click "Developer Tools" button
   - [ ] **Verify**: Submenu expands
   - [ ] Click "Create Payment"
   - [ ] **Verify**: Create Payment form appears
   - [ ] Click "Dashboard" tab
   - [ ] **Verify**: Returns to Dashboard

4. **Logout/Login**
   - [ ] Click "Logout"
   - [ ] Login again
   - [ ] **Verify**: Dashboard appears (not last visited page)

---

## Validation Commands

```bash
# Type checking
npm run type-check

# Linting
npm run lint

# Development server
npm run dev
```

---

## Additional Considerations

### State Persistence (Out of Scope for MVP)

In a production app, you might want to remember the last visited tab:

```typescript
// Save to localStorage
localStorage.setItem('lastTab', activeTab);

// Load on mount
const [activeTab, setActiveTab] = useState<Tab>(
  (localStorage.getItem('lastTab') as Tab) || 'dashboard'
);
```

**For MVP**: Always default to Dashboard (simpler, more predictable)

### Deep Linking (Out of Scope for MVP)

In a production app with React Router, you might want:
- `/dashboard` → Dashboard
- `/transactions` → Transactions
- `/transactions/:id` → Transaction Details
- `/dev-tools/create-payment` → Create Payment

**For MVP**: Single-page app with state-based navigation (simpler)

---

## Notes

- This is a one-line change but has significant UX impact
- Dashboard should be fully functional before making this change
- If Dashboard has bugs, users will see them immediately on login
- Test Dashboard thoroughly before changing default tab

---

## Rollback Plan

If Dashboard has issues after deployment:

```typescript
// Temporarily revert to old behavior
const [activeTab, setActiveTab] = useState<Tab>('transactions'); // Safe fallback
```

Or add a feature flag:

```typescript
const DEFAULT_TAB = import.meta.env.VITE_DEFAULT_TAB || 'dashboard';
const [activeTab, setActiveTab] = useState<Tab>(DEFAULT_TAB as Tab);
```

---

## Success Metrics (for future tracking)

If you add analytics later, track:
- **Bounce Rate**: Do users immediately leave after seeing Dashboard?
- **Navigation Patterns**: Do users explore other tabs from Dashboard?
- **Time on Dashboard**: How long do users spend on Dashboard?
- **Click-through Rate**: Do users click "View All Transactions"?

---

**Status**: Final task in MVP  
**Next Steps**: Test entire flow, validate all acceptance criteria, deploy
