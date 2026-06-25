# Task 01: Project Setup

**Status**: Not Started  
**Estimated Time**: 2 hours

## Goal

Initialize Vite + React + TypeScript project with Tailwind CSS and Axios.

## Acceptance Criteria

- [ ] Vite project created in `frontend/` directory
- [ ] TypeScript strict mode enabled
- [ ] Tailwind CSS configured
- [ ] Axios installed
- [ ] Project structure created
- [ ] Dev server runs on `http://localhost:5173`

## Implementation Steps

### 1. Create Vite Project

```bash
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install
```

### 2. Install Dependencies

```bash
npm install axios
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
```

### 3. Configure Tailwind

**`tailwind.config.js`**:
```javascript
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {},
  },
  plugins: [],
}
```

**`src/index.css`**:
```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

### 4. Configure Vite Proxy

**`vite.config.ts`**:
```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

### 5. Update TypeScript Config

**`tsconfig.json`**:
```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

### 6. Create Project Structure

```bash
mkdir -p src/{components,context,services,types}
```

### 7. Update package.json Scripts

```json
{
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "lint": "eslint . --ext ts,tsx --report-unused-disable-directives --max-warnings 0",
    "type-check": "tsc --noEmit"
  }
}
```

### 8. Create Basic App Structure

**`src/App.tsx`**:
```typescript
function App() {
  return (
    <div className="min-h-screen bg-gray-100">
      <div className="container mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold text-gray-900">
          Acabou o Mony - Payment Gateway
        </h1>
        <p className="mt-2 text-gray-600">
          Frontend is ready!
        </p>
      </div>
    </div>
  )
}

export default App
```

## Validation

```bash
# Type checking
npm run type-check

# Linting
npm run lint

# Start dev server
npm run dev
# Should open http://localhost:5173
# Should show "Acabou o Mony - Payment Gateway"

# Build
npm run build
# Should complete without errors
```

## Files Created

- `frontend/package.json`
- `frontend/vite.config.ts`
- `frontend/tsconfig.json`
- `frontend/tailwind.config.js`
- `frontend/postcss.config.js`
- `frontend/src/App.tsx`
- `frontend/src/main.tsx`
- `frontend/src/index.css`
- `frontend/src/components/` (empty)
- `frontend/src/context/` (empty)
- `frontend/src/services/` (empty)
- `frontend/src/types/` (empty)

## Next Task

`02-auth-context.md` - Create authentication context for mock login
