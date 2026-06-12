# Codebase Map — Acabou o Mony

## Visão Geral

Plataforma de pagamentos focada em **Live Commerce** e **Conversational Commerce**, com stack Java 21 + Spring Boot 3.x reativo, Nginx, Redis, PostgreSQL e Mercado Pago como adquirente.

---

## Estrutura de Diretórios

| Caminho | Descrição |
|---|---|
| `3ds-engine/` | Microserviço de autenticação 3DS 2.x / MFA (Spring Boot WebFlux) |
| `docker/nginx/` | Config do Nginx (reverse proxy + TLS + rate limiting) |
| `spec/` | Documentos SDD (specs, ADRs, user stories, tech plans, tasks) |
| `tests/` | Testes Python de integração/infra (pytest) |
| `meu_ambiente/` | Virtualenv Python local |
| `.github/workflows/` | GitHub Actions CI |

---

## 3DS Engine (Java 21 + Spring Boot 3.3.5)

### Pacotes

| Pacote | Arquivos | Função |
|---|---|---|
| `config/` | `RedisConfig.java`, `SecurityConfig.java` | Config Redis reativo + Spring Security |
| `controller/` | `LandingPageController.java` | Landing page (`GET /`) |
| `dto/` | `CallbackRequest.java`, `MfaVerifyRequest.java`, `MfaVerifyResponse.java` | DTOs de requisição/resposta |
| `model/` | `AuthResult.java`, `ChallengeSession.java`, `ErrorResponse.java` | Modelos de domínio |
| `repository/` | `ChallengeSessionRepository.java` | Repositório Redis para sessões de desafio |
| `security/` | `JwtTokenProvider.java`, `JwtClaims.java` | Geração/validação de JWT |
| `service/` | `ChallengeSessionService.java`, `AuthVerificationService.java`, `CallbackNotifier.java`, `AuditLogger.java` | Lógica de negócio |
| `exception/` | `ChallengeExpiredException.java`, `InvalidTokenException.java`, `ThreeDsException.java` | Exceções customizadas |
| `handler/` | `GlobalErrorHandler.java` | Tratamento global de erros |

### Testes (11 classes)

- **Unitários:** `JwtTokenProviderTest`, `ChallengeSessionServiceTest`, `AuthVerificationServiceTest`, `CallbackNotifierTest`
- **Auditoria:** `ChallengeSessionServiceAuditTest`, `AuthVerificationServiceAuditTest`
- **Integração:** `ThreeDsChallengeControllerIntegrationTest`, `ChallengeSessionRepositoryTest`
- **Config:** `TestRedisConfig`, `LandingPageControllerTest`

---

## Infraestrutura

| Componente | Tecnologia | Função |
|---|---|---|
| **Proxy/Edge** | Nginx + ngrok | TLS termination, rate limiting, exposição pública |
| **Cache/Sessão** | Redis | Sessões de desafio 3DS, idempotência |
| **Banco** | PostgreSQL 16 (via R2DBC) | Ledger de transações (Core Service, fora deste repo) |
| **Orquestração** | Docker Compose | 3 contêineres: nginx, ngrok, (app futura) |
| **CI** | GitHub Actions | Testes Python de infra com ngrok |

---

## Documentação SDD (`spec/`)

| Pasta | Conteúdo |
|---|---|
| `vision.md` | Visão e boundaries do projeto |
| `user-stories/` | US-001 (processamento), US-002 (escalabilidade), US-003 (segurança), US-004 (integração live) |
| `specs/` | Spec-001 (entry point ngrok+nginx), Spec-001 (3ds engine) |
| `adrs/` | ADR-001 (ngrok como API gateway + nginx reverse proxy), ADR-001 (3ds engine microservice) |
| `tech-plans/` | Plan-001 (entry point), Plan-001 (3ds auth engine) |
| `tasks/` | 9 tasks do auth-engine-core (scaffold → Redis → JWT → endpoints → testes → auditoria) |

---

## Testes Python (`tests/`)

| Arquivo | Descrição |
|---|---|
| `test_entrypoint.py` | Testes de healthcheck e roteamento do entry point |
| `test_entrypoint_integration.py` | Testes de integração com Docker + ngrok |
| `test_load_balancing.py` | Testes de balanceamento de carga |

---

## Stack Resumida

- **Runtime:** Java 21, Spring Boot 3.3.5, Spring WebFlux (Netty)
- **Banco/Cache:** PostgreSQL 16 (R2DBC), Redis (Spring Data Redis Reactive)
- **Segurança:** Spring Security, JWT (jjwt 0.12.6), 3DS 2.x
- **Testes:** JUnit 5, Mockito, Testcontainers, Reactor Test
- **Infra:** Nginx, ngrok, Docker, Docker Compose, GitHub Actions
