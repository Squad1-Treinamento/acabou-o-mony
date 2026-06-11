---
id: spec-002
status: active
links:
  - spec/specs/index.md
  - spec/adrs/adr-002-cache-and-idempotency-layer.md
  - spec/tech-plans/plan-002-cache-and-idempotency-layer.md
  - spec/user-stories/us-001-transaction-processing.md
  - spec/user-stories/us-002-scalability.md
  - ARCHITECTURE.md
---

# Cache e Idempotency Layer com Redis

## Context and Primary Objective

O gateway de pagamento "Acabou o Mony" precisa otimizar a performance e garantir idempotência de transações para atender ao SLA de <1s durante picos de carga em Live Commerce. Esta spec define os requisitos funcionais e critérios de aceitação para a implementação de uma camada de cache e idempotência usando Redis.

**Primary Objective:** Reduzir latência de consultas repetidas e prevenir cobranças duplicadas sem comprometer a confiabilidade do sistema.

**Scope:**
- ✅ Preparação de infraestrutura Docker (Redis standalone com persistência)
- ✅ Configuração de variáveis de ambiente e TTLs
- ✅ Especificação de dados cacheáveis e não-cacheáveis
- ✅ Definição de estratégia de idempotência
- ✅ Implementação de circuit breaker e fallback
- ❌ Implementação de código Java (será feita em fase posterior)
- ❌ Migração de dados existentes (não aplicável no MVP)

## Functional Requirements (Behavior)

### FR-1: Infraestrutura Redis

**FR-1.1: Docker Compose Configuration**
- O sistema deve provisionar um container Redis 7 Alpine via `docker-compose.yml`
- Redis deve expor porta `6379` para conexões locais
- Redis deve ter persistência AOF (Append-Only File) habilitada
- Redis deve ter health check configurado (`redis-cli ping` a cada 10s)
- Redis deve ter limite de memória de **512MB** com política `allkeys-lru`

**FR-1.2: Volume Persistente**
- Redis deve usar volume Docker nomeado `redis-data` montado em `/data`
- Dados devem persistir entre restarts do container
- Volume deve ser criado automaticamente pelo Docker Compose

**FR-1.3: Network Isolation**
- Redis deve estar na mesma rede Docker que 3DS Engine e Core Service (futuro)
- Redis não deve ser exposto publicamente (apenas localhost em dev)

### FR-2: Configuração de Variáveis de Ambiente

**FR-2.1: Arquivo .env.example**
- O sistema deve fornecer `.env.example` com todas as variáveis de ambiente necessárias
- Variáveis devem incluir: conexão Redis, TTLs, circuit breaker, criptografia
- Valores padrão devem ser adequados para ambiente de desenvolvimento

**FR-2.2: Variáveis Obrigatórias**
```env
# Redis Connection
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0
REDIS_TIMEOUT_MS=5000

# Redis Pool (Lettuce)
REDIS_POOL_MAX_ACTIVE=8
REDIS_POOL_MAX_IDLE=8
REDIS_POOL_MIN_IDLE=0

# Cache TTL (seconds)
CACHE_TTL_MERCHANT_CONFIG=86400      # 24h
CACHE_TTL_RISK_THRESHOLDS=7200       # 2h
CACHE_TTL_BIN_LOOKUP=604800          # 7 days
CACHE_TTL_TRANSACTION_HISTORY=60     # 1 min
CACHE_TTL_IDEMPOTENCY_KEY=86400      # 24h

# Cache Behavior
CACHE_ENABLED=true
CACHE_FALLBACK_TO_DB_ON_ERROR=true

# Cache Encryption (for future use)
CACHE_ENCRYPT_SENSITIVE_DATA=false
CACHE_ENCRYPTION_KEY=

# Circuit Breaker
CACHE_CIRCUIT_BREAKER_ENABLED=true
CACHE_CIRCUIT_BREAKER_FAILURE_THRESHOLD=3
CACHE_CIRCUIT_BREAKER_WAIT_DURATION_SECONDS=30
CACHE_CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE=10
```

**FR-2.3: Validação de Variáveis**
- Aplicação deve validar variáveis obrigatórias na inicialização
- Se `CACHE_ENCRYPT_SENSITIVE_DATA=true`, `CACHE_ENCRYPTION_KEY` deve estar definida
- TTLs devem ser números positivos

### FR-3: Dados Cacheáveis

**FR-3.1: Merchant Configuration**
- **Namespace:** `core:merchant:{merchant_id}`
- **TTL:** 24 horas (86400s)
- **Conteúdo:** Nome, endereço, configurações de negócio (não-sensíveis)
- **Invalidação:** `@CacheEvict` em operações de atualização de merchant

**FR-3.2: Risk Thresholds**
- **Namespace:** `core:risk:{merchant_id}`
- **TTL:** 2 horas (7200s)
- **Conteúdo:** Regras de fraude, limites de valor, score thresholds
- **Invalidação:** `@CacheEvict` em operações de atualização de regras

**FR-3.3: BIN Lookup Results**
- **Namespace:** `core:bin:{bin}`
- **TTL:** 7 dias (604800s)
- **Conteúdo:** Banco emissor, ACS URL, país, tipo de cartão
- **Invalidação:** Manual (dados semi-estáticos)

**FR-3.4: Transaction History Metadata**
- **Namespace:** `core:tx_history:{merchant_id}:{card_token}`
- **TTL:** 1 minuto (60s)
- **Conteúdo:** IDs de transações, timestamps, status (sem dados de cartão)
- **Invalidação:** Automática por TTL curto

**FR-3.5: Idempotency Keys**
- **Namespace:** `core:idempotency:{idempotency_key}`
- **TTL:** 24 horas (86400s)
- **Conteúdo:** Resultado completo da transação (JSON)
- **Invalidação:** Automática por TTL

### FR-4: Dados NÃO Cacheáveis (Sensíveis)

**FR-4.1: Dados Proibidos de Cache**
O sistema **NUNCA** deve cachear os seguintes dados:
- ❌ PAN (Primary Account Number) - já tokenizado, nunca armazenado
- ❌ CVV - descartado após validação
- ❌ Card Token Metadata (masked PAN, brand, exp_date) - sempre buscar do banco
- ❌ Merchant API Keys - sempre buscar do KMS/Vault
- ❌ JWT Secrets - sempre buscar do KMS/Vault
- ❌ Dados de autenticação (passwords, tokens de acesso)

**FR-4.2: Validação de Segurança**
- Code review deve verificar que dados sensíveis não são cacheados
- Testes de segurança devem validar que chaves Redis não contêm PII

### FR-5: Idempotência de Transações

**FR-5.1: Header Obrigatório**
- Endpoints de mutação devem aceitar header `Idempotency-Key`
- Formato: `req_{uuid}` (ex: `req_550e8400e29b41d4a716446655440000`)
- Tamanho máximo: 256 caracteres
- Validação: Deve começar com `req_` e conter apenas caracteres alfanuméricos e hífens

**FR-5.2: Comportamento de Idempotência**
1. Cliente envia `POST /api/v1/payments` com `Idempotency-Key: req_abc123`
2. Core Service verifica `core:idempotency:req_abc123` no Redis
3. **Se chave existir:**
   - Retorna resultado cacheado (HTTP 200 com mesmo body da primeira requisição)
   - Adiciona header `X-Idempotent-Replayed: true`
   - Não processa transação novamente
4. **Se chave não existir:**
   - Processa transação normalmente
   - Salva resultado no Redis com TTL 24h
   - Retorna resultado (HTTP 200/201)
   - Adiciona header `X-Idempotent-Replayed: false`

**FR-5.3: Endpoints com Idempotência**
- `POST /api/v1/payments` (criação de transação)
- `POST /api/v1/refunds` (estornos)
- `POST /api/v1/payments/{id}/capture` (captura de pré-autorização)
- `POST /api/v1/payments/3ds-callback` (callback do 3DS Engine)

**FR-5.4: Tratamento de Erros**
- Se transação falhar (ex: cartão recusado), o erro também deve ser cacheado
- Requisições subsequentes com mesma `Idempotency-Key` devem retornar o mesmo erro
- Erros de validação (HTTP 400) não devem ser cacheados

### FR-6: Circuit Breaker e Fallback

**FR-6.1: Estados do Circuit Breaker**
- **CLOSED (normal):** Redis funcionando, cache ativo
- **OPEN (falha):** Após 3 falhas consecutivas do Redis
  - Todas as requisições vão direto para PostgreSQL (sem tentar Redis)
  - Log: `WARN - Redis circuit breaker OPEN, falling back to database`
- **HALF_OPEN (teste):** Após 30s no estado OPEN
  - Permite 1 requisição de teste ao Redis
  - Se sucesso → volta para CLOSED
  - Se falha → volta para OPEN por mais 30s

**FR-6.2: Fallback Behavior**
- Redis indisponível → Aplicação continua funcionando (busca no PostgreSQL)
- Log de `WARN` (não `ERROR`) para não gerar alertas falsos
- Cliente **não** recebe erro HTTP (degradação silenciosa)
- Não adiciona header `X-Cache-Status` (evitar exposição de infraestrutura)

**FR-6.3: Recuperação Automática**
- Circuit breaker deve tentar reconectar automaticamente
- Não requer intervenção manual para recuperação
- Logs devem indicar transição de estados (OPEN → HALF_OPEN → CLOSED)

### FR-7: Criptografia de Dados Sensíveis (Preparação)

**FR-7.1: Infraestrutura de Criptografia**
- Criar classe `AESEncryptor` com métodos `encrypt()` e `decrypt()`
- Usar algoritmo AES-256-GCM (Galois/Counter Mode)
- Chave de criptografia via variável `CACHE_ENCRYPTION_KEY` (base64, 256 bits)

**FR-7.2: Flag de Habilitação**
- `CACHE_ENCRYPT_SENSITIVE_DATA=false` (desabilitado por padrão)
- Se `true`, validar que `CACHE_ENCRYPTION_KEY` está definida
- Se chave inválida, aplicação deve falhar na inicialização (fail-fast)

**FR-7.3: Uso Futuro**
- Infraestrutura deve estar pronta, mas **não** usada no MVP
- Documentação deve indicar como habilitar criptografia se necessário
- Testes unitários devem validar encrypt/decrypt

### FR-8: Documentação de Setup

**FR-8.1: Arquivo docs/redis-setup.md**
- Documentar comandos de inicialização do Redis
- Documentar comandos de validação (health check, conexão)
- Documentar comandos de debug (listar chaves, inspecionar valores)
- Documentar comandos de limpeza (flush database, remover volumes)

**FR-8.2: Exemplos de Uso**
- Incluir exemplos de `docker-compose up`
- Incluir exemplos de `redis-cli` para inspeção manual
- Incluir troubleshooting de problemas comuns

## Acceptance Criteria (BDD)

### AC-1: Infraestrutura Redis (P0 - Crítico)

**AC-1.1: Container Redis Inicializa com Sucesso**
```gherkin
Given o arquivo docker-compose.yml está configurado
When executo "docker-compose up -d redis"
Then o container "redis" deve estar no estado "running"
And o health check deve retornar "healthy" em até 30 segundos
And o comando "docker exec redis redis-cli ping" deve retornar "PONG"
```

**AC-1.2: Persistência de Dados**
```gherkin
Given o Redis está rodando
When salvo uma chave "test:key" com valor "test_value"
And reinicio o container Redis com "docker-compose restart redis"
Then a chave "test:key" deve ainda existir
And o valor deve ser "test_value"
```

**AC-1.3: Limite de Memória**
```gherkin
Given o Redis está configurado com maxmemory 512MB
When o uso de memória atinge 512MB
Then o Redis deve começar a evictar chaves usando política allkeys-lru
And o Redis não deve crashar ou rejeitar escritas
```

### AC-2: Variáveis de Ambiente (P0 - Crítico)

**AC-2.1: Arquivo .env.example Existe**
```gherkin
Given o repositório foi clonado
When verifico a raiz do projeto
Then o arquivo ".env.example" deve existir
And deve conter todas as variáveis de REDIS_*, CACHE_*, CACHE_CIRCUIT_BREAKER_*
```

**AC-2.2: Validação de Variáveis Obrigatórias**
```gherkin
Given a aplicação está configurada para iniciar
When a variável REDIS_HOST não está definida
Then a aplicação deve falhar na inicialização
And deve exibir mensagem de erro clara indicando a variável faltante
```

**AC-2.3: Validação de Criptografia**
```gherkin
Given CACHE_ENCRYPT_SENSITIVE_DATA=true
When CACHE_ENCRYPTION_KEY não está definida
Then a aplicação deve falhar na inicialização
And deve exibir mensagem "CACHE_ENCRYPTION_KEY is required when encryption is enabled"
```

### AC-3: Cache de Merchant Configuration (P0 - Crítico)

**AC-3.1: Cache Hit**
```gherkin
Given o merchant "m_123" existe no banco de dados
And o merchant foi consultado anteriormente (está no cache)
When consulto o merchant "m_123" novamente
Then a resposta deve vir do Redis (cache hit)
And a latência deve ser < 10ms
And o banco de dados não deve ser consultado
```

**AC-3.2: Cache Miss**
```gherkin
Given o merchant "m_456" existe no banco de dados
And o merchant NÃO está no cache
When consulto o merchant "m_456"
Then a resposta deve vir do banco de dados (cache miss)
And o resultado deve ser salvo no Redis com TTL 24h
And a chave Redis deve ser "core:merchant:m_456"
```

**AC-3.3: Invalidação de Cache**
```gherkin
Given o merchant "m_123" está no cache
When atualizo as configurações do merchant "m_123"
Then a chave "core:merchant:m_123" deve ser removida do Redis
And a próxima consulta deve buscar do banco de dados
And o novo valor deve ser cacheado
```

### AC-4: Idempotência de Transações (P0 - Crítico)

**AC-4.1: Primeira Requisição**
```gherkin
Given não existe transação com Idempotency-Key "req_abc123"
When envio POST /api/v1/payments com Idempotency-Key "req_abc123"
Then a transação deve ser processada normalmente
And o resultado deve ser salvo em "core:idempotency:req_abc123" com TTL 24h
And o header "X-Idempotent-Replayed" deve ser "false"
```

**AC-4.2: Requisição Duplicada (Sucesso)**
```gherkin
Given existe transação aprovada com Idempotency-Key "req_abc123"
When envio POST /api/v1/payments com Idempotency-Key "req_abc123" novamente
Then a transação NÃO deve ser processada novamente
And o resultado cacheado deve ser retornado
And o header "X-Idempotent-Replayed" deve ser "true"
And o status HTTP deve ser o mesmo da primeira requisição (200)
```

**AC-4.3: Requisição Duplicada (Erro)**
```gherkin
Given existe transação recusada com Idempotency-Key "req_xyz789"
When envio POST /api/v1/payments com Idempotency-Key "req_xyz789" novamente
Then o erro cacheado deve ser retornado
And o header "X-Idempotent-Replayed" deve ser "true"
And o status HTTP deve ser o mesmo da primeira requisição (ex: 402)
```

**AC-4.4: Validação de Formato**
```gherkin
Given envio POST /api/v1/payments com Idempotency-Key "invalid_format"
When o formato não é "req_{uuid}"
Then a requisição deve ser rejeitada com HTTP 400
And a mensagem de erro deve indicar "Idempotency-Key must start with 'req_'"
```

### AC-5: Circuit Breaker e Fallback (P0 - Crítico)

**AC-5.1: Fallback em Caso de Falha do Redis**
```gherkin
Given o Redis está indisponível (container parado)
When consulto o merchant "m_123"
Then a aplicação deve buscar do banco de dados
And a requisição deve ser bem-sucedida (HTTP 200)
And o log deve conter "WARN - Redis circuit breaker OPEN, falling back to database"
And o cliente NÃO deve receber erro
```

**AC-5.2: Circuit Breaker Abre Após 3 Falhas**
```gherkin
Given o Redis está indisponível
When ocorrem 3 falhas consecutivas de conexão ao Redis
Then o circuit breaker deve mudar para estado OPEN
And as próximas requisições devem ir direto para o banco (sem tentar Redis)
And o log deve conter "Circuit breaker transitioned to OPEN"
```

**AC-5.3: Circuit Breaker Recupera Automaticamente**
```gherkin
Given o circuit breaker está no estado OPEN
And o Redis volta a funcionar
When passam 30 segundos
Then o circuit breaker deve mudar para estado HALF_OPEN
And deve permitir 1 requisição de teste ao Redis
When a requisição de teste é bem-sucedida
Then o circuit breaker deve mudar para estado CLOSED
And o cache deve voltar a funcionar normalmente
```

### AC-6: Cache de Risk Thresholds (P1 - Importante)

**AC-6.1: Cache com TTL de 2 Horas**
```gherkin
Given as regras de risco do merchant "m_123" estão no cache
When passam 2 horas (7200 segundos)
Then a chave "core:risk:m_123" deve expirar automaticamente
And a próxima consulta deve buscar do banco de dados
```

**AC-6.2: Invalidação Manual**
```gherkin
Given as regras de risco do merchant "m_123" estão no cache
When o merchant atualiza suas regras de fraude
Then a chave "core:risk:m_123" deve ser removida do Redis
And a próxima consulta deve retornar as novas regras
```

### AC-7: Cache de BIN Lookup (P1 - Importante)

**AC-7.1: Cache com TTL de 7 Dias**
```gherkin
Given o BIN "411111" foi consultado e cacheado
When passam 7 dias (604800 segundos)
Then a chave "core:bin:411111" deve expirar automaticamente
And a próxima consulta deve buscar do serviço de BIN lookup
```

**AC-7.2: Dados Semi-Estáticos**
```gherkin
Given o BIN "411111" está no cache com ACS URL "https://acs.bank.com"
When o banco emissor muda o ACS URL
Then o cache deve continuar retornando o valor antigo até o TTL expirar
And não há invalidação automática (dados mudam raramente)
```

### AC-8: Dados Sensíveis NÃO Cacheados (P0 - Crítico)

**AC-8.1: PAN Nunca é Cacheado**
```gherkin
Given uma transação com PAN "4111111111111111"
When a transação é processada
Then o Redis NÃO deve conter a chave com o PAN completo
And apenas o card_token deve ser usado
```

**AC-8.2: CVV Nunca é Cacheado**
```gherkin
Given uma transação com CVV "123"
When a transação é processada
Then o Redis NÃO deve conter o CVV em nenhuma chave
And o CVV deve ser descartado após validação
```

**AC-8.3: Merchant API Keys Nunca são Cacheadas**
```gherkin
Given um merchant com API Key "sk_live_abc123"
When o merchant é autenticado
Then a API Key NÃO deve ser salva no Redis
And deve ser sempre buscada do KMS/Vault
```

### AC-9: Documentação de Setup (P0 - Crítico)

**AC-9.1: Arquivo docs/redis-setup.md Existe**
```gherkin
Given o repositório foi clonado
When verifico o diretório "docs/"
Then o arquivo "redis-setup.md" deve existir
And deve conter seções: "Setup", "Validation", "Debugging", "Troubleshooting"
```

**AC-9.2: Comandos de Setup Funcionam**
```gherkin
Given sigo as instruções em docs/redis-setup.md
When executo os comandos de setup
Then o Redis deve inicializar com sucesso
And os health checks devem passar
And a aplicação deve conectar ao Redis
```

### AC-10: Criptografia (Preparação) (P2 - Desejável)

**AC-10.1: Classe AESEncryptor Existe**
```gherkin
Given o código foi implementado
When verifico o pacote de criptografia
Then a classe "AESEncryptor" deve existir
And deve ter métodos "encrypt(String plaintext)" e "decrypt(String ciphertext)"
```

**AC-10.2: Encrypt/Decrypt Funciona**
```gherkin
Given a chave de criptografia está configurada
When criptografo o texto "sensitive_data"
And descriptografo o resultado
Then o texto original "sensitive_data" deve ser recuperado
```

**AC-10.3: Flag Desabilitada por Padrão**
```gherkin
Given a aplicação está configurada com valores padrão
When verifico a variável CACHE_ENCRYPT_SENSITIVE_DATA
Then o valor deve ser "false"
And nenhum dado deve ser criptografado no cache
```

## Tech Stack and Constraints

- **Redis:** Versão 7 Alpine (Docker image `redis:7-alpine`)
- **Persistência:** AOF (Append-Only File) com `appendonly yes`
- **Memória:** Limite de 512MB com política `allkeys-lru`
- **Serialização:** JSON (Jackson) via `Jackson2JsonRedisSerializer`
- **Circuit Breaker:** Resilience4j (a ser implementado no Core Service)
- **Criptografia:** AES-256-GCM (infraestrutura preparada, não usada no MVP)
- **Docker Compose:** Versão 3.8+
- **Health Check:** `redis-cli ping` a cada 10s, timeout 3s, 3 retries

## Out of Scope

- ❌ Implementação de código Java (será feita em fase posterior)
- ❌ Redis Cluster (usar standalone no MVP)
- ❌ Replicação Redis (master-slave)
- ❌ Protobuf para serialização (usar JSON no MVP)
- ❌ Cache warming (pré-carregar dados na inicialização)
- ❌ Métricas de observabilidade (Prometheus, Grafana)
- ❌ Migração de dados existentes (não aplicável no MVP)
- ❌ Cache de dados sensíveis (sempre buscar do banco/KMS)

## Dependencies

- `docker-compose.yml` deve ser criado/atualizado
- `.env.example` deve ser criado
- `docs/redis-setup.md` deve ser criado
- `spec/adrs/adr-002-cache-and-idempotency-layer.md` deve existir
- `spec/tech-plans/plan-002-cache-and-idempotency-layer.md` deve existir

## References

- [ADR-002: Cache e Idempotency Layer](../adrs/adr-002-cache-and-idempotency-layer.md)
- [Tech Plan 002: Cache Implementation](../tech-plans/plan-002-cache-and-idempotency-layer.md)
- [US-001: Transaction Processing](../user-stories/us-001-transaction-processing.md)
- [US-002: Scalability](../user-stories/us-002-scalability.md)
- [ARCHITECTURE.md](../../ARCHITECTURE.md)
