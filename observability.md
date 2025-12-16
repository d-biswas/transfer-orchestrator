# Observability Setup

Simple metrics, logging, and health monitoring using Spring Boot Actuator.

## What's Enabled

✅ **Health checks** - Service health status
✅ **Prometheus metrics** - For monitoring and alerting
✅ **Structured logging** - Log4j2 with correlation IDs

## Quick Start

### 1. Check Application Health

```bash
curl http://localhost:8080/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "kafka": { "status": "UP" },
    "redis": { "status": "UP" }
  }
}
```

### 2. View Metrics

```bash
# All available metrics
curl http://localhost:8080/actuator/metrics

# Specific metric
curl http://localhost:8080/actuator/metrics/http.server.requests
```

**Common metrics:**
- `jvm.memory.used` - Memory usage
- `http.server.requests` - HTTP request stats
- `kafka.consumer.fetch.total` - Kafka consumer metrics
- `hikaricp.connections.active` - Database connection pool

### 3. Prometheus Scraping

```bash
curl http://localhost:8080/actuator/prometheus
```

## Available Endpoints

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Application health status |
| `/actuator/metrics` | Available metrics list |
| `/actuator/metrics/{metric}` | Specific metric details |
| `/actuator/prometheus` | Prometheus-format metrics |

## Logging

Logs use Log4j2 and include:
- **Timestamp** - When the event occurred
- **Level** - INFO, WARN, ERROR, DEBUG
- **Thread** - Executing thread name
- **Logger** - Class name
- **Message** - Log message
- **Exception** - Stack trace (if present)

**Example log:**
```
2025-12-16 13:00:00.123  INFO [main] TransferOrchestrator : Transfer initiated: transferId=123
```

## Auto-Metrics

Spring Boot automatically tracks:

### JVM Metrics
- `jvm.memory.used` - Heap/non-heap memory
- `jvm.gc.pause` - Garbage collection pauses
- `jvm.threads.live` - Active thread count

### HTTP Metrics
- `http.server.requests` - Request count, duration, status codes
- `http.server.requests.active` - Concurrent requests

### Database Metrics
- `hikaricp.connections.active` - Active DB connections
- `hikaricp.connections.pending` - Waiting for connection
- `jdbc.connections.max` - Max connection pool size

### Kafka Metrics
- `kafka.consumer.fetch.total` - Messages consumed
- `kafka.producer.record.send.total` - Messages produced
- `kafka.consumer.lag` - Consumer lag

## Kubernetes Health Probes

Use actuator for k8s liveness/readiness:

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
```

## That's It!

This minimal setup ensures:
- Health monitoring
- Performance metrics
- Prometheus integration
- Kubernetes readiness