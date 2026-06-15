---
id: adr-003
status: Aceita
links:
  - spec/adrs/index.md
  - spec/specs/spec-004-cache-and-idempotency-layer.md
  - spec/tech-plans/plan-004-cache-and-idempotency-layer.md
  - ARCHITECTURE.md
---
# ADR-003 - Cache e Idempotency Layer com Redis

## Status

Aceita

## Contexto

O gateway de pagamento "Acabou o Mony" precisa otimizar a performance e garantir idempotência de transações para atender ao SLA de <1s e suportar picos de carga em Live Commerce. Atualmente:

- O **3DS Engine** já usa Redis para sessões (`3ds:session:{id}`) e resultados de autenticação (`3ds:auth:{id}`)
- O **Core Payment Service** (a ser implementado) precisará cachear dados de negócio e implementar idempotência
- Não existe uma estratégia unificada de cache, TTL e fallback para falhas do Redis

Sem cache, cada requisição ao Core Service resultaria em múltiplas consultas ao PostgreSQL:
- Merchant Configuration (validação de credenciais, limites)
- Risk Thresholds (regras de fraude)
- BIN Lookup (resolução de ACS URL para 3DS)
- Transaction History (detecção de padrões de fraude)

Sem idempotência, transações duplicadas (retry de rede, double-click do usuário) poderiam resultar em cobranças duplicadas, violando PCI-DSS e gerando chargebacks.

## Decisão

Implementar uma **Cache e Idempotency Layer** com as seguintes características:

### 1. Arquitetura de Cache

**Padrão:** Cache-Aside (Lazy Loading)
- A aplicação busca primeiro no Redis
- Em caso de cache miss, busca no PostgreSQL, salva no Redis com TTL e retorna
- Em caso de falha do Redis, fallback silencioso para PostgreSQL

**Escopo por Serviço:**
- **3DS Engine:** Mantém uso atual (sessões + auth results). **Não** adiciona cache de dados de negócio.
- **Core Payment Service:** Implementa cache de dados de negócio e idempotência de transações.

**Infraestrutura:**
- Redis Standalone (não Cluster) compartilhado entre 3DS Engine e Core Service
- Namespaces isolados por serviço (`3ds:*` e `core:*`)
- Persistência AOF (Append-Only File) habilitada
- Limite de memória: **512MB** com política `allkeys-lru` (Least Recently Used)

### 2. Dados Cacheáveis e TTLs

| Tipo de Dado | Namespace | TTL | Justificativa |
|---|---|---|---|
| Merchant Configuration | `core:merchant:{merchant_id}` | 24h | Dados semi-estáticos (nome, endereço, configurações) |
| Risk Thresholds | `core:risk:{merchant_id}` | 2h | Regras de fraude podem mudar em resposta a ataques |
| BIN Lookup Results | `core:bin:{bin}` | 7 dias | Dados de bancos emissores mudam raramente |
| Transaction History Metadata | `core:tx_history:{merchant_id}:{card_token}` | 1 min | Dados em tempo real para detecção de fraude |
| Idempotency Keys | `core:idempotency:{idempotency_key}` | 24h | Padrão da indústria (Stripe, Adyen) |

**Dados NÃO Cacheáveis (Sensíveis):**
- ❌ PAN (Primary Account Number) - já tokenizado, nunca armazenado
- ❌ CVV - descartado após validação
- ❌ Card Token Metadata (masked PAN, brand, exp_date) - sempre buscar do banco
- ❌ Merchant API Keys - sempre buscar do KMS/Vault
- ❌ JWT Secrets - sempre buscar do KMS/Vault

### 3. Serialização

**Formato:** JSON (Jackson)
- ✅ Interoperabilidade com outras linguagens
- ✅ Fácil debug e inspeção visual no Redis CLI
- ✅ Já configurado no 3DS Engine (`Jackson2JsonRedisSerializer`)
- ⚠️ Maior uso de memória vs Protobuf (aceitável dado o limite de 512MB)

**Decisão:** Não usar Protobuf no MVP. Reavaliar se houver gargalo de memória comprovado.

### 4. Idempotência de Transações

**Escopo:** Idempotência de transação completa (Cenário A)
- Header obrigatório: `Idempotency-Key: req_{uuid}`
- Formato: Prefixo `req_` + UUID v4 (ex: `req_550e8400e29b41d4a716446655440000`)
- Tamanho máximo: 256 caracteres
- TTL: 24 horas

**Comportamento:**
1. Cliente envia `POST /api/v1/payments` com `Idempotency-Key: req_abc123`
2. Core Service verifica `core:idempotency:req_abc123` no Redis
3. **Se existir:** Retorna resultado cacheado (mesmo que transação já finalizada)
4. **Se não existir:** Processa transação, salva resultado no Redis com TTL 24h, retorna

**Endpoints com Idempotência:**
- `POST /api/v1/payments` (criação de transação)
- `POST /api/v1/refunds` (estornos)
- `POST /api/v1/payments/{id}/capture` (captura de pré-autorização)
- `POST /api/v1/payments/3ds-callback` (callback do 3DS Engine)

### 5. Implementação de Cache

**Abordagem:** Anotações Spring Cache (`@Cacheable`, `@CachePut`, `@CacheEvict`)
- ✅ Menos código boilerplate
- ✅ Declarativo e fácil de manter
- ✅ Suporte nativo a TTL e key generation

**Exceção:** Lógica complexa (ex: cache condicional, múltiplas chaves) usa `ReactiveRedisTemplate` manual.

**Invalidação de Cache:** Síncrona com `@CacheEvict`
```java
@CacheEvict(value = "merchants", key = "#merchantId")
public Mono<Merchant> updateMerchant(String merchantId, Merchant data) {
    return merchantRepository.save(data);
}
```

### 6. Resiliência e Fallback

**Circuit Breaker:** Resilience4j com os seguintes parâmetros:
```yaml
CACHE_CIRCUIT_BREAKER_ENABLED=true
CACHE_CIRCUIT_BREAKER_FAILURE_THRESHOLD=3        # Abre após 3 falhas consecutivas
CACHE_CIRCUIT_BREAKER_WAIT_DURATION_SECONDS=30   # Aguarda 30s antes de tentar reconectar
CACHE_CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE=10     # Janela de 10 requisições
```

**Estados do Circuit Breaker:**
1. **CLOSED (normal):** Redis funcionando, cache ativo
2. **OPEN (falha):** Após 3 falhas consecutivas
   - Log: `WARN - Redis circuit breaker OPEN, falling back to database`
   - Todas as requisições vão direto para PostgreSQL (sem tentar Redis)
   - Cliente **não** recebe erro (degradação silenciosa)
3. **HALF_OPEN (teste):** Após 30s, permite 1 requisição de teste
   - Se sucesso → volta para CLOSED
   - Se falha → volta para OPEN por mais 30s

**Fallback Behavior:**
- Redis indisponível → Log de `WARN` (não `ERROR`)
- Aplicação continua funcionando normalmente (busca no PostgreSQL)
- Não retorna erro HTTP para o cliente
- Não adiciona header `X-Cache-Status` (degradação silenciosa)

### 7. Criptografia de Dados Sensíveis

**Decisão:** Preparar infraestrutura de criptografia AES-256-GCM, mas **não cachear dados sensíveis** no MVP.

**Implementação:**
- Criar classe `AESEncryptor` com métodos `encrypt()` e `decrypt()`
- Configurar chave via variável de ambiente `CACHE_ENCRYPTION_KEY` (base64)
- Flag `CACHE_ENCRYPT_SENSITIVE_DATA=false` (desabilitado por padrão)

**Uso Futuro:** Se for necessário cachear dados sensíveis (ex: card metadata para UX), habilitar criptografia e usar TTL curto (5 min).

### 8. Configuração e Deployment

**Docker Compose:**
- Redis 7 Alpine com persistência AOF
- Health check: `redis-cli ping` a cada 10s
- Volume persistente: `redis-data:/data`
- Limite de memória: 512MB com `maxmemory-policy allkeys-lru`

**Variáveis de Ambiente:**
- Todas as configurações (host, port, TTLs, circuit breaker) via `.env`
- Arquivo `.env.example` com valores padrão para desenvolvimento
- Secrets (passwords, encryption keys) via variáveis de ambiente em produção

## Consequências

### Positivas

✅ **Performance:** Redução de 70-90% na latência de consultas repetidas (cache hit)
✅ **Escalabilidade:** Menor carga no PostgreSQL, permitindo mais throughput
✅ **Idempotência:** Prevenção de cobranças duplicadas sem lógica complexa no banco
✅ **Resiliência:** Circuit breaker garante degradação graceful em caso de falha do Redis
✅ **Simplicidade:** Anotações Spring Cache reduzem código boilerplate
✅ **Segurança:** Dados sensíveis não são cacheados, reduzindo superfície de ataque
✅ **Observabilidade:** Logs estruturados de cache hit/miss/fallback facilitam debug
✅ **Flexibilidade:** TTLs configuráveis por tipo de dado via variáveis de ambiente

### Negativas

⚠️ **Complexidade Operacional:** Adiciona Redis como dependência crítica (mitigado por fallback)
⚠️ **Consistência Eventual:** Cache pode ficar desatualizado até o TTL expirar (mitigado por `@CacheEvict`)
⚠️ **Uso de Memória:** JSON usa mais memória que Protobuf (aceitável com 512MB)
⚠️ **Debugging:** Cache pode mascarar bugs de lógica de negócio (mitigado por logs)
⚠️ **Overhead de Rede:** Comunicação com Redis adiciona latência (~1-5ms)
⚠️ **Single Point of Failure:** Redis standalone sem replicação (aceitável para MVP)

### Riscos e Mitigações

| Risco | Impacto | Mitigação |
|---|---|---|
| Redis fica indisponível | Performance degradada | Circuit breaker + fallback para PostgreSQL |
| Cache desatualizado | Dados incorretos exibidos | TTLs curtos + `@CacheEvict` em operações de escrita |
| Memória Redis esgotada | Eviction de chaves importantes | Política `allkeys-lru` + monitoramento de uso |
| Chave de criptografia vazada | Exposição de dados sensíveis | Não cachear dados sensíveis no MVP |
| Latência de rede Redis | SLA de <1s comprometido | Redis no mesmo datacenter + timeout de 5s |

## Alternativas Consideradas

### Alternativa 1: Redis Cluster (Rejeitada)
- **Prós:** Alta disponibilidade, sharding automático
- **Contras:** Complexidade operacional, overhead de coordenação
- **Decisão:** Usar Redis Standalone no MVP, migrar para Cluster se necessário

### Alternativa 2: Protobuf para Serialização (Rejeitada)
- **Prós:** 80% menos memória, serialização mais rápida
- **Contras:** Complexidade de schemas `.proto`, debug difícil
- **Decisão:** Usar JSON no MVP, reavaliar se houver gargalo de memória

### Alternativa 3: Cache Write-Through (Rejeitada)
- **Prós:** Cache sempre atualizado
- **Contras:** Latência de escrita aumenta, complexidade de transações
- **Decisão:** Usar Cache-Aside com `@CacheEvict` para invalidação

### Alternativa 4: Cachear Dados Sensíveis com Criptografia (Rejeitada)
- **Prós:** Melhor performance para consultas de card metadata
- **Contras:** Risco de segurança, complexidade de key management
- **Decisão:** Não cachear dados sensíveis no MVP, sempre buscar do banco/KMS

### Alternativa 5: Idempotência no Banco de Dados (Rejeitada)
- **Prós:** Não depende de Redis
- **Contras:** Maior carga no PostgreSQL, lock contention
- **Decisão:** Usar Redis para idempotência (sub-millisecond lookup)

## Referências

- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Redis Persistence (AOF)](https://redis.io/docs/management/persistence/)
- [Resilience4j Circuit Breaker](https://resilience4j.readme.io/docs/circuitbreaker)
- [Stripe API Idempotency](https://stripe.com/docs/api/idempotent_requests)
- [PCI-DSS Requirement 3.4 (Data Protection)](https://www.pcisecuritystandards.org/)
