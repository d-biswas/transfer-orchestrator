# Quick Start Guide

Get your Transfer Orchestrator running with plain Eclipse EDC in 5 minutes.

## TL;DR

```bash
# 1. Build the orchestrator
./gradlew clean bootJar
docker build -t transfer-orchestrator:0.0.1-SNAPSHOT .

# 2. Start everything
docker-compose up -d

# 3. Wait for services to be healthy (~30 seconds)
docker-compose ps

# 4. Load sample data into provider EDC
./setup-provider-edc.sh

# 5. Test it!
curl http://localhost:8080/actuator/health
curl http://localhost:8181/api/check/health  # Provider EDC
curl http://localhost:9191/api/check/health  # Consumer EDC
```

## What You Get

✅ **Transfer Orchestrator** (Spring Boot) - Your app
✅ **Consumer EDC** - Initiates transfers (called by your orchestrator)
✅ **Provider EDC** - Provides data assets
✅ **PostgreSQL** - Database
✅ **Redis** - Caching
✅ **Kafka + Zookeeper** - Event streaming
✅ **Kafka UI** - Monitor Kafka topics at http://localhost:8081

## Port Quick Reference

| Service       | Port | What                        |
|---------------|------|-----------------------------|
| Orchestrator  | 8080 | Your REST API               |
| Provider EDC  | 8181 | Management API              |
| Provider EDC  | 8182 | DSP Protocol                |
| Consumer EDC  | 9191 | Management API              |
| Consumer EDC  | 9192 | DSP Protocol                |
| PostgreSQL    | 5432 | Database                    |
| Kafka UI      | 8081 | Web interface               |

## Test the Full Flow

### Option 1: Use Your Orchestrator API

```bash
# Initiate a transfer via your orchestrator
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "assetId": "asset-001",
    "providerId": "provider-participant",
    "providerUrl": "http://provider-edc:7172/api/v1/dsp",
    "consumerId": "consumer-participant",
    "dataType": "QUALITY",
    "consumerRegion": "EU",
    "consumerCertification": "ISO9001",
    "usagePurpose": "QualityAnalysis"
  }'

# Check transfer status
curl http://localhost:8080/api/v1/transfers/{transferId}
```

### Option 2: Query Catalog Directly

```bash
# Consumer queries provider's catalog
curl -X POST http://localhost:9191/management/v2/catalog/request \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: consumer-api-key" \
  -d '{
    "@context": {"@vocab": "https://w3id.org/edc/v0.0.1/ns/"},
    "counterPartyAddress": "http://provider-edc:7172/api/v1/dsp",
    "protocol": "dataspace-protocol-http"
  }'
```

**Health checks failing?**
```bash
docker-compose logs provider-edc
docker-compose logs consumer-edc
```

**Services not starting?**
```bash
# Clean restart
docker-compose down -v
docker-compose up -d
```

## Need More Details?

📖 **Full Setup Guide**: See [EDC-SETUP.md](EDC-SETUP.md)
📋 **Project Info**: See [README.md](README.md)
🤖 **AI Instructions**: See [CLAUDE.md](CLAUDE.md)

## Common Tasks

**View logs**:
```bash
docker-compose logs -f transfer-orchestrator
docker-compose logs -f provider-edc
docker-compose logs -f consumer-edc
```

**Restart orchestrator**:
```bash
# After code changes
./gradlew bootJar
docker build -t transfer-orchestrator:0.0.1-SNAPSHOT .
docker-compose up -d transfer-orchestrator
```

**Reset everything**:
```bash
docker-compose down -v
docker-compose up -d
./setup-provider-edc.sh
```

**Access database**:
```bash
docker exec -it postgres psql -U orchestrator -d orchestrator
```

**Check Kafka topics**:
- Web UI: http://localhost:8081
- Or CLI: `docker exec -it kafka kafka-topics.sh --list --bootstrap-server localhost:9092`

## Next Steps

1. ✅ Environment is running
2. ✅ Sample asset loaded
3. 🔨 Implement your policy engine (TimeBasedPolicy, RateLimitPolicy, etc.)
4. 🔨 Add more assets to provider
5. 🔨 Test failure scenarios
6. 🔨 Add monitoring/metrics

Happy orchestrating! 🚀