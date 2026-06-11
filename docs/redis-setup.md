# Redis Setup and Validation Guide

This document provides step-by-step instructions for setting up, validating, debugging, and troubleshooting Redis for the Acabou o Mony payment gateway.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Setup](#setup)
3. [Validation](#validation)
4. [Debugging](#debugging)
5. [Troubleshooting](#troubleshooting)
6. [Production Considerations](#production-considerations)

---

## Prerequisites

- **Docker:** Version 20.10+ ([Install Docker](https://docs.docker.com/get-docker/))
- **Docker Compose:** Version 2.0+ (included with Docker Desktop)
- **Redis CLI:** (optional,  - Linux/Mac: `brew install redis` or `apt-get install redis-tools`
  - Windows: Use Docker exec (see cfor manual inspection)
 ommands below)

---

## Setup

### 1. Configure Environment Variables

Copy the example environment file and configure your values:

```bash
cp .env.example .env
```

**Minimum required variables for Redis:**
```env
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0
REDIS_TIMEOUT_MS=5000
```

### 2. Start Redis Container

Start Redis standalone with persistence and health check:

```bash
docker-compose up -d redis
```

**Expected output:**
```
[+] Running 2/2
 ✔ Network acabou-o-mony_acabou-o-mony-network  Created
 ✔ Container redis                               Started
```

### 3. Verify Container is Running

```bash
docker ps | grep redis
```

**Expected output:**
```
CONTAINER ID   IMAGE           STATUS                   PORTS
abc123def456   redis:7-alpine  Up 10 seconds (healthy)  0.0.0.0:6379->6379/tcp
```

---

## Validation

### 1. Health Check

Verify Redis health check is passing:

```bash
docker inspect redis --format='{{.State.Health.Status}}'
```

**Expected output:**
```
healthy
```

### 2. Ping Test

Test Redis connectivity:

```bash
docker exec redis redis-cli ping
```

**Expected output:**
```
PONG
```

### 3. Verify Persistence Configuration

Check that AOF (Append-Only File) persistence is enabled:

```bash
docker exec redis redis-cli CONFIG GET appendonly
```

**Expected output:**
```
1) "appendonly"
2) "yes"
```

### 4. Verify Memory Limit

Check that maxmemory is set to 512MB:

```bash
docker exec redis redis-cli CONFIG GET maxmemory
```

**Expected output:**
```
1) "maxmemory"
2) "536870912"
```

*Note: 536870912 bytes = 512MB*

### 5. Verify Eviction Policy

Check that eviction policy is `allkeys-lru`:

```bash
docker exec redis redis-cl
#i CONFIG GET maxmemory-policy
```

**Expected ## 6. Test Writoutput:**
```
1) "maxmemory-policy"
2) "allkeys-lru"
```
e and Read

Write a test key and verify it persists:

```bash
# Write test key
docker exec redis redis-cli SET test:key "test_value"

# Read test key
docker exec redis redis-cli GET test:key

# Delete test key
docker exec redis redis-cli DEL test:key
```

**Expected output:**
```
OK
test_value
(integer) 1
```

### 7. Test Persistence After Restart

Verify data persists after container restart:

```bash
# Write a key
docker exec redis redis-cli SET persist:test "should_survive_restart"

# Restart container
docker-compose restart redis

# Wait for health check (10 seconds)
sleep 15

# Verify key still exists
docker exec redis redis-cli GET persist:test

# Cleanup
docker exec redis redis-cli DEL persist:test
```

**Expected output:**
```
OK
should_survive_restart
(integer) 1
```

---

## Debugging

### 1. View All Keys

List all keys in Redis (use with caution in production):

```bash
docker exec redis redis-cli KEYS "*"
```

**Example output:**
```
1) "3ds:session:ch_001"
2) "3ds:auth:ch_001"
3) "core:merchant:m_123"
4) "core:idempotency:req_abc123"
```

### 2. View Keys by Namespace

List keys for a specific service:

```bash
# 3DS Engine keys
docker exec redis redis-cli KEYS "3ds:*"

# Core Service keys
docker exec redis redis-cli KEYS "core:*"

# Merchant configuration keys
docker exec redis redis-cli KEYS "core:merchant:*"

# Idempotency keys
docker exec redis redis-cli KEYS "core:idempotency:*"
```

### 3. Inspect Key Value

View the value of a specific key:

```bash
docker exec redis redis-cli GET "core:merchant:m_123"
```

**Example output (JSON):**
```json
{"merchant_id":"m_123","name":"Ana's Store","email":"ana@store.com"}
```

### 4. Check Key TTL

View remaining time-to-live for a key:

```bash
docker exec redis redis-cli TTL "core:merchant:m_123"
```

**Example output:**
```
(integer) 86395
```

*Note: TTL in seconds. -1 means no expiry, -2 means key doesn't exist*

### 5. View Redis Memory Usage

Check current memory usage:

```bash
docker exec redis redis-cli INFO memory | grep used_memory_human
```

**Example output:**

```bash
docker exec redis redis-cli INFO stats
```

**Key m```
used_memory_human:2.50M
```

### 6. View Redis Statistics

View detailed Redis statistics:
 `total_connections_received`: Total connections since startup
- `total_commands_processed`: Total commands processed
- `keyspace_hits`: Cache hitetrics:**
-s
- `keyspace_misses`: Cache misses
- `evicted_keys`: Keys evicted due to maxmemory limit

### 7. Monitor Real-Time Commands

Watch Redis commands in real-time (useful for debugging):

```bash
docker exec redis redis-cli MONITOR
```

**Example output:**
```
OK
1717234567.123456 [0 172.18.0.3:54321] "GET" "core:merchant:m_123"
1717234567.234567 [0 172.18.0.3:54322] "SET" "core:merchant:m_456" "{...}" "EX" "86400"
```

*Press Ctrl+C to stop monitoring*

### 8. View Container Logs

View Redis container logs:

```bash
# Follow logs in real-time
docker logs -f redis

# View last 100 lines
docker logs --tail 100 redis

# View logs with timestamps
docker logs -t redis
```

### 9. Interactive Redis CLI

Enter interactive Redis CLI session:

```bash
docker exec -it redis redis-cli
```

**Example session:**
```
127.0.0.1:6379> PING
PONG
127.0.0.1:6379> KEYS "core:*"
1) "core:merchant:m_123"
127.0.0.1:6379> GET "core:merchant:m_123"
"{\"merchant_id\":\"m_123\",\"name\":\"Ana's Store\"}"
127.0.0.1:6379> EXIT
```

---

## Troubleshooting

### Problem: Container Fails to Start

**Symptoms:**
```bash
docker ps | grep redis
# No output
```

**Solution:**
```bash
# Check container logs
docker logs redis

# Common issues:
# 1. Port 6379 already in use
sudo lsof -i :6379  # Check what's using the port
docker-compose down  # Stop all containers
docker-compose up -d redis

# 2. Volume permission issues
docker volume rm acabou-o-mony_redis-data
docker-compose up -d redis
```

---

### Problem: Health Check Failing

**Symptoms:**
```bash
docker inspect redis --format='{{.State.Health.Status}}'
# Output: unhealthy
```

**Solution:**
```bash
# Check health check logs
docker inspect redis --format='{{json .State.Health}}' | jq

# Manually test ping
docker exec redis redis-cli ping

# If ping fails, restart container
docker-compose restart redis
```

---

### Problem: Connection Timeout from Application

**Symptoms:**
```
ERROR - Redis connection timeout: io.lettuce.core.RedisCommandTimeoutException
```

**Solution:**
```bash
# 1. Verify Redis is running
docker ps | grep redis

# 2. Verify network connectivity
docker exec 3ds-engine ping redis

# 3. Check Redis is listening on correct port
docker exec redis netstat -tuln | grep 6379

# 4. Verify environment variables
docker exec 3ds-engine env | grep REDIS

# 5. Increase timeout in .env
REDIS_TIMEOUT_MS=10000  # Increase to 10 seconds
docker-compose restart 3ds-engine
```

---

### Problem: Out of Memory (OOM)

**Symptoms:**
```
ERROR - Redis OOM command not allowed when used memory > 'maxmemory'
```

**Solution:**
```bash
# 1. Check current memory usage
docker exec redis redis-cli INFO memory | grep used_memory_human

# 2. Check evicty 51ion policy
docker exec redis redis-cli CONFIG GET maxmemory-policy

# 3. Increase maxmemory limit (if needed)
2mb tdocker exec redis redis-cli CONFIG SET maxmemory 1gb

# 4. Or update docker-compose.yml and restart
# Change: --maxmemoro --maxmemory 1gb
docker-compose down
docker-compose up -d redis

# 5. Manually evict keys if needed
docker exec redis redis-cli FLUSHDB  # WARNING: Deletes all keys in current DB
```

---

### Problem: Data Not Persisting After Restart

**Symptoms:**
```bash
# Key exists before restart
docker exec redis redis-cli GET test:key
# Output: "test_value"

# After restart, key is gone
docker-compose restart redis
docker exec redis redis-cli GET test:key
# Output: (nil)
```

**Solution:**
```bash
# 1. Verify AOF is enabled
docker exec redis redis-cli CONFIG GET appendonly
# Should be "yes"

# 2. Check AOF file exists
docker exec redis ls -lh /data
# Should show appendonly.aof

# 3. Check for AOF errors in logs
docker logs redis | grep AOF

# 4. If AOF is corrupted, rebuild it
docker exec redis redis-check-aof --fix /data/appendonly.aof
docker-compose restart redis
```

---

### Problem: High Latency

**Symptoms:**
```
WARN - Redis operation took 500ms (expected < 10ms)
```

**Solution:**
```bash
# 1. Check Redis CPU usage
docker stats redis

# 2. Check slow queries
docker exec redis redis-cli SLOWLOG GET 10

# 3. Check number of keys
docker exec redis redis-cli DBSIZE

# 4. Check for long-running commands
docker exec redis redis-cli CLIENT LIST

# 5. Optimize key patterns (avoid KEYS * in production)
# Use SCAN instead of KEYS for iteration
```

---

### Problem: Circuit Breaker Stuck in OPEN State

**Symptoms:**
```
WARN - Redis circuit breaker OPEN, falling back to database
```

**Solution:**
```bash
# 1. Verify Redis is actually healthy
docker exec redis redis-cli ping

# 2. Check application logs for connection errors
docker logs 3ds-engine | grep -i redis

# 3. Restart application to reset circuit breaker
docker-compose restart 3ds-engine

# 4. If Redis was down, it should auto-recover after WAIT_DURATION_SECONDS (30s)
# Monitor logs for "Circuit breaker transitioned to CLOSED"
```

---

## Cleanup

### Remove All Keys

**WARNING: This deletes all data in Redis**

```bash
# Delete all keys in current database (DB 0)
docker exec redis redis-cli FLUSHDB

# Delete all keys in all databases
docker exec redis redis-cli FLUSHALL
```

### Stop Redis Container

```bash
docker-compose stop redis
```

### Remove Redis Container and Volume

**WARNING: This deletes all persisted data**

```bash
docker-compose downcation

Update ` redis
docker volume rm acabou-o-mony_redis-data
```

---

## Production Considerations

### 1. Enable Password Authentidocker-compose.yml`:

```yaml
redis:
  command: redis-server --appendonly yes --maxmemory 512mb --maxmemory-policy allkeys-lru --requirepass ${REDIS_PASSWORD}
```

Update `.env`:
```env
REDIS_PASSWORD=strong_password_here
```

Connect with password:
```bash
docker exec redis redis-cli -a ${REDIS_PASSWORD} ping
```

### 2. Enable TLS/SSL

Use `rediss://` protocol for encrypted connections:

```env
REDIS_HOST=rediss://redis
REDIS_TLS_ENABLED=true
```

### 3. Use Redis Climage: redis:7-alpine
  command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD}

redis-replicuster (High Availability)

For production, consider Redis Cluster with replication:

```yaml
redis-master:
  a:
  image: redis:7-alpine
  command: redis-server --replicaof redis-master 6379 --requirepass ${REDIS_PASSWORD}
```

### 4. Monitor Redis Metrics

Integrate with Prometheus and Grafana:

```yaml
redis-exporter:
  image: oliver006/redis_exporter:latest
  environment:
    - REDIS_ADDR=redis:6379
    - REDIS_PASSWORD=${REDIS_PASSWORD}
  ports:
    - "9121:9121"
```

### 5. Backup Strategy

Automated backups of AOF file:

```bash
# Backup AOF file
docker exec redis redis-cli BGSAVE

# Copy AOF to host
docker cp redis:/data/appendonly.aof ./backups/redis-$(date +%Y%m%d).aof

# Restore from backup
docker cp ./backups/redis-20260601.aof redis:/data/appendonly.aof
docker-compose restart redis
```

---

## References

- [Redis Official Documentation](https://redis.io/docs/)
- [Redis Persistence (Attps://redis.io/docs/management/opOF)](https://redis.io/docs/management/persistence/)
- [Redis Memory Optimization](hio/docs/management/security/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [ADR-002: Cache and Idempotency Layer](../spec/adrs/adr-002-cache-and-idempotency-layer.md)
- [Spec-002: Cache and Idempotency Layer](../spec/specs/spec-002-cache-and-idempotency-layer.md)

---

timi
dzation/memory-optimization/)
- [Redis Securiocker exec redis redis-cli GET "key_name"

# Check TTL
docker exec redis redis-cli TTL "key_name"

# Memory usage
docker exec redis redis-cli INFO memory | grep used_memory_human

# Interactive CLI
docker exec -it redis redis-cli

# Remove all data (WARNING)
docker exec redis redis-cli FLUSHALL
```

---

**Last Updated:** 2026-06-01  
**Version:** 1.0.0  
**Maintainer:** Acabou o Mony Engineering Team
