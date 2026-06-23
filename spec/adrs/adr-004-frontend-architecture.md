---
id: adr-004
status: Aceita
links:
  - spec/adrs/index.md
  - spec/specs/spec-006-frontend-checkout.md
  - spec/specs/spec-007-merchant-dashboard.md
  - spec/tech-plans/index.md
---
# ADR-004 - Arquitetura do Frontend: Next.js 14, TailwindCSS, shadcn/ui e TanStack Query

## Status
Aceita

## Contexto

O backend da plataforma Acabou o Mony (core-payment e 3ds-engine) está 100% implementado. A próxima etapa é construir o frontend do zero: um fluxo de checkout para o cliente final (spec-006) e um dashboard de gerenciamento de transações para o lojista (spec-007).

Não existe nenhum código frontend no repositório. As decisões de design estão consolidadas em `design.json` (tokens de cor, tipografia, espaçamento, motion). O frontend precisa integrar com o Nginx na porta 8080 e suportar o padrão de polling de status de pagamento (estados `CHALLENGE_PENDING` e `UNKNOWN` requerem refetch periódico).

## Decisao

O frontend é construído como uma aplicação **Next.js 14 (App Router)** localizada em `frontend/` na raiz do repositório, com o seguinte stack:

| Camada | Tecnologia | Justificativa |
|---|---|---|
| Framework | Next.js 14 App Router | Roteamento por arquivo, RSC para o dashboard, client components para checkout interativo e polling |
| Linguagem | TypeScript strict | O domínio de pagamentos tem tipos precisos (PaymentStatus, PaymentResponseDTO); strict mode elimina erros de null/undefined em lógica de polling |
| Estilização | TailwindCSS | Os tokens de `design.json` são mapeados para o tema do Tailwind em `tailwind.config.ts`, mantendo decisões visuais em ponto único |
| Componentes | shadcn/ui | Primitivos acessíveis e sem opinião visual (Button, Input, Badge, Table, Skeleton) — fáceis de sobrescrever com os tokens do design.json |
| Estado de servidor | TanStack Query | Gerencia polling declarativo via `refetchInterval`, deduplicação de requests concorrentes e auto-refresh do dashboard |
| Formulários | React Hook Form + Zod | Schemas Zod espelham as regras de validação do backend (card_token_id 1–100 chars, currency 3-char ISO, amount ≥ 1) |
| Package manager | pnpm | Isolamento estrito de dependências, performance superior ao npm, eficiência em disco |

**Integração com infraestrutura existente:** o frontend aponta para `http://localhost:8080` (Nginx) via variável de ambiente `NEXT_PUBLIC_API_URL`. Em produção/Docker, o Nginx serve como proxy entre o frontend e os microserviços.

**Alternativas rejeitadas:**
- **Vite + React SPA** — sem SSR, sem roteamento nativo, exigiria biblioteca adicional (React Router). Rejeitado.
- **Create React App** — depreciado desde 2023. Rejeitado.
- **Redux Toolkit** — sobrecarga desnecessária; TanStack Query cobre todo o estado de servidor, React state cobre o estado local do checkout. Rejeitado.
- **Remix** — viável, mas sem experiência prévia da equipe e o App Router cobre os mesmos casos de uso. Rejeitado.
- **Axios** — o cliente de API tem dois endpoints e um padrão de autenticação simples; fetch nativo com wrapper é suficiente. Rejeitado.

## Consequencias

Positivas:
- Tipagem forte em todo o domínio de pagamento reduz erros em estados críticos (polling, 3DS redirect, idempotência).
- Tokens de `design.json` aplicados via Tailwind garantem consistência visual sem estilo ad hoc.
- shadcn/ui entrega acessibilidade (ARIA, foco, contraste) nos componentes base sem overhead de manutenção.
- Polling de status de pagamento é declarativo e testável via `refetchInterval` do TanStack Query.
- Estrutura `frontend/` isolada dos serviços Java facilita CI independente e build separado.

Negativas:
- A fronteira RSC/client component do App Router exige atenção — componentes com hooks (`useState`, `useQuery`) devem ser explicitamente marcados com `"use client"`.
- pnpm requer Node ≥ 18 no ambiente de desenvolvimento e CI.
- `GET /api/v1/payments/{id}` (endpoint de polling) precisa ser confirmado no backend antes da implementação do hook de polling; se ausente, é necessária solução alternativa.
