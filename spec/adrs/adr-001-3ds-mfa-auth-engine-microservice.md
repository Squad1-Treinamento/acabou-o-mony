---
id: adr-001
status: Aceita
links:
  - spec/adrs/index.md
  - spec/specs/spec-001-3ds-mfa-auth-engine.md
  - spec/tech-plans/plan-001-3ds-mfa-auth-engine.md
  - ARCHITECTURE.md
---
# ADR-001 - Microsservico 3DS / MFA Auth Engine

## Status

Aceita

## Contexto

O gateway de pagamento precisa de autenticacao step-up (3D Secure 2.x) para transacoes de alto risco, alto valor ou primeiro uso do cartao. Inserir essa logica no Core Payment Processing Service existente quebraria o SLA de <1s para transacoes padrao, pois o fluxo 3DS envolve espera do usuario, redirect para o banco emissor e callback assincrono.

A arquitetura atual (ARCHITECTURE.md) preve que o Core Service seja enxuto e reativo. Incluir o processamento 3DS diretamente no Core adicionaria complexidade estado-dependente e arriscaria degradacao de performance em pico de carga.

```
Merchant / Cardholder Bank
        |
        | 3DS challenge result (via redirect)
        v
+-----------------------+    Redis Protocol     +-------------------+
|   3DS / MFA Auth      | <-------------------> |   Redis Cluster   |
|       Engine          |                       | (session state,   |
| (Spring WebFlux,      |                       |  auth results)    |
|  Java 21, Netty)      |                       +-------------------+
+----------+------------+                             ^
           |                                          |
           | Internal HTTP/2 callback                 | Shared Redis
           | (async, non-blocking)                    |
           v                                          |
+----------------------+   Redis Protocol             |
|   Core Payment       | -----------------------------+
|   Processing Service |
| (Spring WebFlux,     |
|  Java 21, Netty)     |
+----------------------+
```

## Decisao

Criar um microsservico separado chamado **3DS / MFA Auth Engine** com as seguintes caracteristicas:

1. **Microservico isolado** — sem acesso ao banco relacional (PostgreSQL). Toda comunicacao com o Core Service ocorre via Redis compartilhado e callback HTTP/2 interno.
2. **Persistencia exclusiva em Redis** — sessoes de desafio (`3ds:session:{challenge_id}`, HASH com TTL de 600s) e resultados de autenticacao (`3ds:auth:{challenge_id}`, STRING com TTL de 24h) sao armazenados no Redis Cluster compartilhado com o Core.
3. **Callback assincrono** — apos validar o token MFA, o Engine escreve o resultado no Redis e dispara um POST HTTP/2 nao-bloqueante para o Core (`/api/v1/payments/3ds-callback`). Se o callback falhar, o Core pode ler o resultado diretamente do Redis como fallback.
4. **JWT HS256** — cada desafio carrega um JWT assinado com `challenge_id`, `transaction_id`, `merchant_id`, `amount`, `exp`, `iat`. O JWT e validado em toda requisicao de verificacao MFA.
5. **Stack reativa** — Java 21, Spring Boot 3.x com WebFlux e Netty, Spring Data Redis Reactive, Project Reactor. Sem bloqueio em nenhum ponto do fluxo.
6. **Idempotencia por challenge_id** — antes de processar, o Engine verifica se `3ds:auth:{challenge_id}` ja existe. Se existir, retorna o resultado em cache sem reprocessar.
7. **Configuracao externalizada** — TTLs, thresholds de risco, segredo JWT e URL de callback sao definidos via `application.yml` / variaveis de ambiente.
8. **Sem dependencia de Mercado Pago** — o Engine nao chama APIs externas de acquirer. Essa responsabilidade e exclusiva do Core Service.

## Consequencias

Positivas:
- Isolamento de responsabilidade: o Core Service mantem SLA de <1s para transacoes padrao, sem ser impactado por espera de redirect bancario.
- Escalabilidade independente: o Engine pode escalar horizontalmente conforme a demanda de autenticacao 3DS, sem competir por recursos com o Core.
- Stack reativa e homogenea: mesma base (Java 21, WebFlux, Netty) do Core Service, facilitando manutencao e compartilhamento de praticas.
- Estado efemero em Redis: sem poluicao do banco relacional com sessoes temporarias de desafio.
- Resiliiencia a falha de callback: o Core consegue recuperar o resultado via Redis, eliminando dependencia sincrona entre os servicos.
- Idempotencia nativa: a chave `3ds:auth:{challenge_id}` com TTL de 24h previne processamento duplicado sem infraestrutura adicional.

Negativas:
- Complexidade operacional: adiciona um microsservico novo para gerenciar (deploy, monitoria, log aggregation).
- Dependencia de Redis Cluster: ambos os servicos compartilham o mesmo Redis, criando um ponto de contencao potencial e acoplamento de infraestrutura.
- Duas fontes de verdade temporarias: o estado da transicao 3DS existe em Redis e no callback HTTP — coordenacao eventualmente consistente pode gerar janelas de inconsistencia curtas (mitigadas pelo fallback de polling).
- Overhead de rede: o callback HTTP/2 entre servicos adiciona latencia (embora nao-bloqueante) vs. uma chamada local no mesmo processo.
- Complexidade de debugging: fluxo assincrono com callback + Redis polling torna o rastreamento de transacoes 3DS mais custoso que um fluxo sincrono.
