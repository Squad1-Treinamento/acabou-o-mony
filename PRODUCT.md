# Product

## Register

product

## Users

**Clientes (checkout):** Consumidores finais que chegam ao fluxo de pagamento enviados por um merchant. Contexto de alta ansiedade — querem finalizar a compra com segurança e rapidez, sem distrações. Dispositivos: mobile-first (375px), mas também desktop.

**Merchants (dashboard):** Responsáveis por acompanhar transações, monitorar status e investigar problemas. Contexto de trabalho — usam laptop/desktop, buscam clareza e densidade de informação sem sobrecarga. Precisam confiar nos dados que veem.

## Product Purpose

Plataforma de pagamentos "Acabou o Mony" — processa pagamentos com cartão, gerencia autenticação 3DS, rastreia status e fornece ao merchant visibilidade completa sobre o ciclo de vida das transações. Sucesso = cliente completa o pagamento em ≤ 3 passos; merchant encontra qualquer transação em ≤ 30 segundos.

## Brand Personality

**Profissional · Limpa · Amigável**

Tom: direto e confiante, sem frieza corporativa. A interface deve parecer que foi feita por pessoas que entendem de pagamentos e se importam com quem está pagando. Segurança comunicada por silêncio e precisão, não por disclaimers visuais.

## Anti-references

- **PayPal antigo / bancão tradicional**: azul-marinho dominante, texto em negrito excessivo, visual de portal bancário dos anos 2000, ícones clipart, formulários densos sem respiro.
- **Mercado Pago / iFood (consumer-app)**: laranja, amarelo ou verde-vibrante; hierarquia de app de delivery; visual de massa com promoções e destaques.
- **Clone de Stripe**: gradientes roxos, motion-heavy, estética de "startup de infraestrutura". Este produto tem identidade própria (verde escuro #0D2B1E).

## Design Principles

1. **Confiança antes de tudo** — cada tela deve fazer o usuário sentir que seu dinheiro está seguro, antes de qualquer outra coisa.
2. **Uma tela, um objetivo** — checkout: completar o pagamento. Dashboard: encontrar a transação. Sem ambiguidade de foco.
3. **Espaço é mensagem** — whitespace generoso comunica que nada está escondido. Elementos comprimidos geram desconfiança em contexto de pagamento.
4. **Hierarquia por peso, não por decoração** — distinção visual via tamanho e peso de fonte e espaçamento; nada de bordas coloridas, gradientes ou badges decorativos.
5. **Estados nunca ficam em branco** — loading, empty, error: toda tela assíncrona tem os três tratados com a mesma atenção visual que o estado de sucesso.

## Accessibility & Inclusion

- WCAG AA obrigatório: contraste ≥ 4.5:1 para texto normal, ≥ 3:1 para texto grande (≥18px ou bold ≥14px).
- Focus rings visíveis em todos os elementos interativos (não remover outline por padrão).
- Animações devem respeitar `prefers-reduced-motion`.
- Formulários com labels explícitas (não placeholder como único label).
- Tabela do dashboard: `<thead>` semântico, células com scope correto.
