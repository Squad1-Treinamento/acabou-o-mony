# Nubank DNA — Guia de Estilização Frontend

> Aplica a identidade visual do Nubank (roxo, minimalista, cards flutuantes,
> cantos arredondados) sobre a estrutura existente do Acabou o Mony.

---

## Sumário

1. [DNA Visual](#1-dna-visual)
2. [Design Tokens](#2-design-tokens)
3. [tailwind.config.js — Completo](#3-tailwindconfigjs--completo)
4. [CSS Custom Properties (index.css)](#4-css-custom-properties-indexcss)
5. [Componentes](#5-componentes)
6. [Animações Mínimas](#6-animações-mínimas)
7. [Passo a Passo da Migração](#7-passo-a-passo-da-migração)
8. [Referências](#8-referências)

---

## 1. DNA Visual

| Princípio | Diretriz |
|---|---|
| **Roxo como assinatura** | Usar `nu-purple` com moderação e impacto. CTA primário, hover, destaque. |
| **Simplicidade radical** | Cada elemento precisa merecer seu espaço. Sem bordas pesadas, sem decoração desnecessária. |
| **Cards flutuantes** | `box-shadow` suave para criar profundidade. Superfícies brancas sobre fundo cinza claro. |
| **Cantos arredondados** | `rounded-xl` (12px) em cards, `rounded-full` em botões. |
| **Espaçamento generoso** | `p-6` / `p-8` em cards, `gap-6` entre seções. Respire. |
| **Tipografia limpa** | Inter (fallback system sans-serif). Hierarquia clara com peso e tamanho. |
| **Micro-interações** | Transitions suaves em hover/focus. Cards com leve elevação ao hover. |

---

## 2. Design Tokens

### Cores

| Token | Hex | Uso |
|---|---|---|
| `nu-purple` | `#820AD1` | Primary: botões, links, indicador ativo, header |
| `nu-purple-dark` | `#6A07A8` | Hover de elementos roxos |
| `nu-purple-light` | `#F3E8FF` | Badges de status, backgrounds sutis |
| `nu-bg` | `#F5F5F5` | Fundo da página (off-white quente) |
| `nu-surface` | `#FFFFFF` | Cards, modais, superfícies |
| `nu-text-primary` | `#1A1A1A` | Títulos, labels, valores |
| `nu-text-secondary` | `#6B6B6B` | Descrições, metadados |
| `nu-text-muted` | `#A3A3A3` | Placeholders, hints, timestamps |
| `nu-border` | `#E8E8E8` | Bordas sutis, divisores |
| `nu-success` | `#00A86B` | COMPLETED, sucesso |
| `nu-error` | `#E74C3C` | DECLINED/FAILED, erros |
| `nu-warning` | `#FF9500` | PROCESSING/CHALLENGE_PENDING |

### Tipografia

| Elemento | Tamanho | Peso | Cor |
|---|---|---|---|
| Page title (`h2`) | `text-xl` (20px) | `font-bold` (700) | `text-nu-text-primary` |
| Card title | `text-lg` (18px) | `font-semibold` (600) | `text-nu-text-primary` |
| Body / label | `text-sm` (14px) | `font-medium` (500) | `text-nu-text-secondary` |
| Value / data | `text-sm` (14px) | `font-normal` (400) | `text-nu-text-primary` |
| Muted / hint | `text-xs` (12px) | `font-normal` (400) | `text-nu-text-muted` |
| Code / mono | `text-xs` (12px) | `font-mono` | `text-nu-text-primary` |

### Sombras

| Token | Valor | Uso |
|---|---|---|
| `nu-shadow-sm` | `0 1px 3px rgba(0,0,0,0.06)` | Cards padrão |
| `nu-shadow-md` | `0 4px 12px rgba(0,0,0,0.08)` | Cards em hover, modais |
| `nu-shadow-lg` | `0 8px 24px rgba(0,0,0,0.1)` | Dropdowns, overlays |

### Border Radius

| Token | Valor | Uso |
|---|---|---|
| `nu-radius-sm` | `8px` (`rounded-lg`) | Inputs, badges |
| `nu-radius-md` | `12px` (`rounded-xl`) | Cards, superfícies |
| `nu-radius-full` | `9999px` (`rounded-full`) | Botões, pills |

---

## 3. tailwind.config.js — Completo

Substitua o conteúdo atual por:

```js
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        nu: {
          purple: '#820AD1',
          'purple-dark': '#6A07A8',
          'purple-light': '#F3E8FF',
          bg: '#F5F5F5',
          surface: '#FFFFFF',
          'text-primary': '#1A1A1A',
          'text-secondary': '#6B6B6B',
          'text-muted': '#A3A3A3',
          border: '#E8E8E8',
          success: '#00A86B',
          error: '#E74C3C',
          warning: '#FF9500',
        },
      },
      boxShadow: {
        'nu-sm': '0 1px 3px rgba(0,0,0,0.06)',
        'nu-md': '0 4px 12px rgba(0,0,0,0.08)',
        'nu-lg': '0 8px 24px rgba(0,0,0,0.10)',
        'nu-card': '0 2px 8px rgba(130,10,209,0.08)',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
```

### Passos

1. Substitua `tailwind.config.js`
2. Instale a fonte Inter no `index.html`:

```html
<link rel="preconnect" href="https://fonts.googleapis.com" />
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet" />
```

3. Troque o favicon para um símbolo roxo (ou use um SVG simples com `#820AD1`)

---

## 4. CSS Custom Properties (index.css)

Substitua `src/index.css` por:

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  html {
    font-family: 'Inter', system-ui, -apple-system, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
  }

  body {
    background-color: #F5F5F5;
    color: #1A1A1A;
  }

  * {
    scrollbar-width: thin;
    scrollbar-color: #E8E8E8 transparent;
  }
}

@layer components {
  .nu-card {
    @apply bg-nu-surface rounded-xl shadow-nu-sm p-6;
  }

  .nu-card-hover {
    @apply nu-card transition-all duration-200;
  }

  .nu-card-hover:hover {
    @apply shadow-nu-md -translate-y-0.5;
  }

  .nu-btn-primary {
    @apply w-full py-3 px-6 rounded-full font-semibold text-sm
           bg-nu-purple text-white
           hover:bg-nu-purple-dark
           disabled:bg-gray-200 disabled:text-gray-400 disabled:cursor-not-allowed
           transition-all duration-200;
  }

  .nu-btn-secondary {
    @apply w-full py-3 px-6 rounded-full font-semibold text-sm
           bg-nu-purple-light text-nu-purple
           hover:bg-[#E4D5F5]
           transition-all duration-200;
  }

  .nu-btn-ghost {
    @apply py-2 px-4 rounded-full font-medium text-sm
           text-nu-text-secondary
           hover:bg-gray-100
           transition-colors duration-200;
  }

  .nu-input {
    @apply w-full px-4 py-3 rounded-xl border border-nu-border
           text-sm text-nu-text-primary placeholder:text-nu-text-muted
           bg-nu-surface
           focus:outline-none focus:ring-2 focus:ring-nu-purple/30 focus:border-nu-purple
           transition-all duration-200;
  }

  .nu-label {
    @apply block text-sm font-medium text-nu-text-secondary mb-1.5;
  }

  .nu-badge {
    @apply inline-flex items-center px-3 py-1 rounded-full text-xs font-medium;
  }

  .nu-badge-success {
    @apply nu-badge bg-emerald-50 text-nu-success;
  }

  .nu-badge-error {
    @apply nu-badge bg-red-50 text-nu-error;
  }

  .nu-badge-warning {
    @apply nu-badge bg-orange-50 text-nu-warning;
  }

  .nu-badge-neutral {
    @apply nu-badge bg-gray-50 text-nu-text-secondary;
  }

  .nu-tab {
    @apply py-3 px-1 border-b-2 font-medium text-sm transition-all duration-200;
  }

  .nu-tab-active {
    @apply nu-tab border-nu-purple text-nu-purple;
  }

  .nu-tab-inactive {
    @apply nu-tab border-transparent text-nu-text-muted hover:text-nu-text-secondary;
  }
}
```

---

## 5. Componentes

### 5.1 Header (App.tsx `header`)

**Antes (ML):** fundo `bg-ml-blue`, texto branco, bordas retas.  
**Depois (Nubank):** fundo `bg-nu-surface` com `shadow-nu-sm`, logo roxa, logout minimalista.

Classes a aplicar:

| Elemento | Antes | Depois |
|---|---|---|
| `header` | `bg-ml-blue shadow-sm` | `bg-nu-surface shadow-nu-sm sticky top-0 z-10` |
| `h1` | `text-xl font-bold text-white` | `text-lg font-bold text-nu-purple` |
| `span` (Admin) | `text-sm text-white/80` | `text-sm text-nu-text-secondary` |
| Logout btn | `bg-white/15 text-white hover:bg-white/25 rounded-md` | `nu-btn-ghost` |

### 5.2 Navegação (tabs)

**Antes:** Nav abaixo do header, `bg-ml-surface border-b border-ml-border`.  
**Depois:** Tabs mais clean, sem fundo separado, indicador roxo.

| Elemento | Antes | Depois |
|---|---|---|
| `nav` | `bg-ml-surface border-b border-ml-border` | `bg-nu-surface px-4` + `shadow-nu-sm` (remover border-b) |
| Tab container | `flex space-x-8` | `flex gap-8` |
| Tab ativa | `border-ml-yellow text-ml-text-primary` | `nu-tab-active` |
| Tab inativa | `border-transparent text-ml-text-muted` | `nu-tab-inactive` |
| Main | `container mx-auto px-4 py-6` | `container mx-auto px-4 py-8 max-w-4xl` |

### 5.3 Login (LoginForm.tsx)

**Antes:** Card centralizado, botão amarelo, info box azul.  
**Depois:** Card flutuante com `nu-card-hover` + efeito de entrada sutil. Botão roxo (`nu-btn-primary`). Info box em `nu-purple-light`.

```tsx
// Mudanças de classe:
<div className="min-h-screen bg-nu-bg flex items-center justify-center p-4">
  <div className="nu-card w-full max-w-sm text-center animate-fadeIn">
    <div className="mb-8">
      <h1 className="text-2xl font-bold text-nu-purple mb-1">
        Acabou o Mony
      </h1>
      <p className="text-sm text-nu-text-secondary">
        Payment Gateway — Admin Panel
      </p>
    </div>

    <button
      onClick={handleLogin}
      className="nu-btn-primary mb-6"
    >
      Enter Admin Dashboard
    </button>

    <div className="p-4 bg-nu-purple-light rounded-xl text-left">
      <p className="text-sm font-semibold text-nu-purple mb-2">
        Using credentials
      </p>
      <p className="text-xs text-nu-purple/70">
        Merchant ID:{' '}
        <code className="bg-white/50 px-1.5 py-0.5 rounded text-nu-purple font-mono">
          {ADMIN_MERCHANT_ID}
        </code>
      </p>
      <p className="text-xs text-nu-purple/70 mt-1">
        API Key:{' '}
        <code className="bg-white/50 px-1.5 py-0.5 rounded text-nu-purple font-mono">
          {ADMIN_API_KEY}
        </code>
      </p>
    </div>
  </div>
</div>
```

### 5.4 Payment Form (PaymentForm.tsx)

**Antes:** Card `bg-ml-surface p-6 rounded-lg shadow-sm`, inputs com borda azul no focus.  
**Depois:** Card `nu-card` com inputs `nu-input` e botão `nu-btn-primary`.

| Elemento | Antes | Depois |
|---|---|---|
| Form container | `bg-ml-surface p-6 rounded-lg shadow-sm` | `nu-card` |
| `inputClass` | `w-full px-3 py-2.5 border border-ml-border rounded-md ... focus:ring-ml-blue` | `nu-input` (_py-3, rounded-xl, ring roxo_) |
| `labelClass` | `block text-sm font-medium text-ml-text-secondary mb-1.5` | `nu-label` |
| Submit btn | `bg-ml-yellow text-ml-text-primary` | `nu-btn-primary` |
| Secondary btn | `bg-ml-blue text-white` | `nu-btn-secondary` |
| "New Key" btn | `bg-ml-blue text-white rounded-md` | `nu-btn-secondary text-sm px-4 py-2.5` |
| Error box | `bg-red-50 border border-ml-red/20 rounded-lg` | `bg-red-50 border border-nu-error/20 rounded-xl p-4` |
| Response card | `bg-ml-surface border border-ml-green/20 rounded-lg` | `nu-card border border-nu-success/20` |
| Challenge btn | `bg-ml-orange text-white rounded-md` | `rounded-full bg-nu-warning text-white` |
| Toggle checkbox | `rounded border-ml-border text-ml-blue focus:ring-ml-blue` | `rounded-lg border-nu-border text-nu-purple focus:ring-nu-purple/30` |
| Grid avançado | `grid grid-cols-2 gap-6` | `grid grid-cols-1 md:grid-cols-2 gap-6` |
| Placeholder box | `bg-gray-50 border border-ml-border rounded-lg` | `bg-gray-50 rounded-xl p-4` |

### 5.5 Transaction List (TransactionList.tsx)

**Antes:** Tabela tradicional com `thead bg-gray-50`, bordas `divide-ml-border`.  
**Depois:** Tabela mais clean ou cards (Nubank não usa tabelas com linhas — mas podemos manter tabela minimalista).

| Elemento | Antes | Depois |
|---|---|---|
| Container | `bg-ml-surface rounded-lg shadow-sm overflow-hidden` | `bg-nu-surface rounded-xl shadow-nu-sm overflow-hidden` |
| `thead` | `bg-gray-50` | `bg-nu-bg` |
| `th` | `px-6 py-3 text-xs font-semibold text-ml-text-muted uppercase` | `px-6 py-4 text-xs font-semibold text-nu-text-muted uppercase tracking-wider` |
| `td` | `px-6 py-4 whitespace-nowrap` | `px-6 py-4 whitespace-nowrap` |
| Linhas | `hover:bg-gray-50 cursor-pointer` | `hover:bg-nu-purple-light/30 cursor-pointer transition-colors duration-150` |
| Filter container | `bg-ml-surface rounded-lg shadow-sm p-4` | `bg-nu-surface rounded-xl shadow-nu-sm p-4` |
| Filter select | `border border-ml-border rounded-md focus:ring-ml-blue` | `nu-input max-w-xs` |
| View Details link | `text-ml-blue hover:text-ml-blue-dark` | `text-nu-purple hover:text-nu-purple-dark font-medium` |
| Empty state | `bg-ml-surface rounded-lg shadow-sm p-12` | `nu-card py-12 text-center` |

**Status badges** — trocar as classes:

| Status | Antes | Depois |
|---|---|---|
| COMPLETED | `bg-ml-green/10 text-ml-green` | `nu-badge-success` |
| DECLINED/FAILED | `bg-ml-red/10 text-ml-red` | `nu-badge-error` |
| PROCESSING/CHALLENGE_PENDING | `bg-ml-orange/10 text-ml-orange` | `nu-badge-warning` |
| Outros | `bg-gray-100 text-ml-text-secondary` | `nu-badge-neutral` |

### 5.6 Transaction Details (TransactionDetails.tsx)

**Antes:** Card `bg-ml-surface rounded-lg shadow-sm p-6` com labels uppercase e códigos.  
**Depois:** Card `nu-card` com mais espaçamento e clean.

| Elemento | Antes | Depois |
|---|---|---|
| Container | `max-w-2xl mx-auto` | `max-w-2xl mx-auto` |
| Card | `bg-ml-surface rounded-lg shadow-sm p-6` | `nu-card` |
| Labels | `text-xs font-medium text-ml-text-muted uppercase tracking-wider` | `nu-label text-xs uppercase` (adicionar `tracking-wider`) |
| Code blocks | `bg-gray-50 border border-ml-border px-3 py-2 rounded font-mono` | `bg-nu-bg px-3 py-2.5 rounded-lg font-mono text-sm` |
| Back btn | `text-ml-blue hover:text-ml-blue-dark` | `nu-btn-ghost -ml-2` (ou text-nu-purple) |
| Refresh btn | `bg-ml-blue text-white rounded-md` | `nu-btn-secondary text-sm px-4 py-2` |
| Grid 2-col | `grid grid-cols-2 gap-4` | `grid grid-cols-2 gap-6` |

---

## 6. Animações Mínimas

Adicione ao `index.css` dentro de `@layer base` ou `@layer utilities`:

### 6.1 Fade In (para páginas/cards)

```css
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}

.animate-fadeIn {
  animation: fadeIn 0.3s ease-out;
}
```

### 6.2 Skeleton pulse (para loading)

```css
@keyframes shimmer {
  0%   { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.skeleton {
  background: linear-gradient(90deg, #F0F0F0 25%, #E8E8E8 50%, #F0F0F0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: 8px;
}
```

### 6.3 Scale on tap (botões)

```css
.nu-btn-primary:active:not(:disabled) {
  transform: scale(0.97);
}
```

---

## 7. Passo a Passo da Migração

### Fase 1 — Tokens (5 min)

1. Substitua `tailwind.config.js` pelo conteúdo da [seção 3](#3-tailwindconfigjs--completo)
2. Adicione o link da fonte Inter no `index.html`
3. Substitua `src/index.css` pelo conteúdo da [seção 4](#4-css-custom-properties-indexcss)
4. Remova `src/App.css` (não usado, conteúdo legado do Vite)

### Fase 2 — Header + Nav (5 min)

Edite `App.tsx` trocando as classes conforme [seção 5.1](#51-header-apptsx-header) e [5.2](#52-navegação-tabs).

### Fase 3 — Componentes (20 min cada)

Edite cada componente na ordem:
1. `LoginForm.tsx` → classes Nubank
2. `PaymentForm.tsx` → `inputClass` → `nu-input`, `labelClass` → `nu-label`, botões → `nu-btn-primary`/`nu-btn-secondary`
3. `TransactionList.tsx` → status badges, cores de tabela, filtro
4. `TransactionDetails.tsx` → card, labels, back button

### Fase 4 — Verificação

```bash
npm run type-check
npm run lint
npm run dev  # visual check
```

---

## 8. Referências

- **Nubank Design**: https://nubank.com.br (site oficial, app)
- **Nubank Brand Guidelines** (conhecimento público): roxo `#820AD1`, tipografia Graphik
- **Inter font**: https://rsms.me/inter/
- **Tailwind CSS**: https://tailwindcss.com/docs/configuration

---

## Checklist de Migração

- [ ] `tailwind.config.js` atualizado com cores `nu-*`
- [ ] Fonte Inter carregada no `index.html`
- [ ] `index.css` com classes utilitárias `nu-*`
- [ ] `App.css` removido (ou esvaziado)
- [ ] Header roxo → branco com logo roxa
- [ ] Tabs com indicador roxo
- [ ] Login com botão roxo `rounded-full`
- [ ] Payment form com inputs `rounded-xl` e foco roxo
- [ ] Botões `rounded-full` e roxos
- [ ] Status badges com `nu-badge-*`
- [ ] Tabela com hover roxo claro
- [ ] Cards com `shadow-nu-sm`
- [ ] `npm run dev` — visual ok
- [ ] `npm run type-check` — sem erros
- [ ] `npm run lint` — sem warnings novos
