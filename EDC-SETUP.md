# Eclipse EDC Setup Guide

This guide explains how to set up and test the Transfer Orchestrator with plain Eclipse EDC connectors (provider + consumer).

## Architecture Overview

```
┌─────────────────────────┐
│ Transfer Orchestrator   │
│ (Your Spring Boot App)  │
└───────────┬─────────────┘
            │
            │ Calls Management API
            ▼
┌─────────────────────────┐         DSP Protocol         ┌─────────────────────────┐
│   Consumer EDC          │◄────────────────────────────►│   Provider EDC          │
│  (Initiates transfers)  │      Contract Negotiation    │  (Provides data assets) │
│                         │      Transfer Processes      │                         │
│  Port: 9191 (Mgmt API)  │                              │  Port: 8181 (Mgmt API)  │
│  Port: 9192 (DSP)       │                              │  Port: 8182 (DSP)       │
└─────────────────────────┘                              └─────────────────────────┘
```

## Step 1: Build Your Orchestrator

```bash
# Build the Spring Boot application
./gradlew clean bootJar

# Build Docker image
docker build -t transfer-orchestrator:0.0.1-SNAPSHOT .
```

## Step 2: Start the Environment

```bash
# Start all services (Postgres, Redis, Kafka, Consumer EDC, Provider EDC, Orchestrator)
docker-compose up -d

# Check status
docker-compose ps

# Expected output:
# - postgres: healthy
# - redis: running
# - kafka: running
# - consumer-edc: healthy
# - provider-edc: healthy
# - transfer-orchestrator: running
```

## Step 3: Verify EDC Connectors

### Check Provider EDC
```bash
curl http://localhost:8181/api/check/health
# Should return: OK or health status
```

### Check Consumer EDC
```bash
curl http://localhost:9191/api/check/health
# Should return: OK or health status
```

## Step 4: Load Sample Data into Provider EDC

The provider needs assets, policies, and contract definitions.

### Create Asset
```bash
curl -X POST http://localhost:8181/management/v3/assets \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: provider-api-key" \
  -d @edc-config/provider/sample-asset.json

# Expected: {"@type":"IdResponse","@id":"asset-001","createdAt":...}
```

### Create Policy
```bash
curl -X POST http://localhost:8181/management/v2/policydefinitions \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: provider-api-key" \
  -d @edc-config/provider/sample-policy.json

# Expected: {"@type":"IdResponse","@id":"policy-001","createdAt":...}
```

### Create Contract Definition
```bash
curl -X POST http://localhost:8181/management/v2/contractdefinitions \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: provider-api-key" \
  -d @edc-config/provider/sample-contract-definition.json

# Expected: {"@type":"IdResponse","@id":"contract-def-001","createdAt":...}
```

### Verify Assets Loaded
```bash
curl -X POST http://localhost:8181/management/v3/assets/request \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: provider-api-key" \
  -d '{
    "@context": {"@vocab": "https://w3id.org/edc/v0.0.1/ns/"},
    "offset": 0,
    "limit": 50
  }'

# Should show asset-001
```

## Step 5: Test the Full Transfer Flow

### Option A: Via Your Orchestrator REST API

Use your orchestrator's API to initiate a transfer:

```bash
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

# Monitor the transfer status
curl http://localhost:8080/api/v1/transfers/{transferId}
```

### Option B: Direct EDC-to-EDC Test (Bypass Orchestrator)

1. **Query Provider Catalog from Consumer**

```bash
curl -X POST http://localhost:9191/management/v2/catalog/request \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: consumer-api-key" \
  -d '{
    "@context": {
      "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
    },
    "counterPartyAddress": "http://provider-edc:7172/api/v1/dsp",
    "protocol": "dataspace-protocol-http"
  }'

# Should return available assets and contract offers
```

2. **Initiate Contract Negotiation**

```bash
curl -X POST http://localhost:9191/management/v2/contractnegotiations \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: consumer-api-key" \
  -d '{
    "@context": {
      "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
      "odrl": "http://www.w3.org/ns/odrl/2/"
    },
    "@type": "ContractRequest",
    "counterPartyAddress": "http://provider-edc:7172/api/v1/dsp",
    "protocol": "dataspace-protocol-http",
    "policy": {
      "@type": "odrl:Offer",
      "@id": "contract-def-001:asset-001",
      "odrl:permission": [{
        "odrl:action": "use",
        "odrl:constraint": []
      }],
      "odrl:target": {"@id": "asset-001"},
      "odrl:assigner": {"@id": "provider-participant"}
    }
  }'

# Returns: negotiation ID
```

3. **Check Negotiation Status**

```bash
curl -X GET http://localhost:9191/management/v2/contractnegotiations/{negotiationId} \
  -H "X-Api-Key: consumer-api-key"

# Wait for state: FINALIZED
# Note the "contractAgreementId"
```

4. **Initiate Transfer**

```bash
curl -X POST http://localhost:9191/management/v2/transferprocesses \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: consumer-api-key" \
  -d '{
    "@context": {"@vocab": "https://w3id.org/edc/v0.0.1/ns/"},
    "@type": "TransferRequest",
    "contractId": "{contractAgreementId}",
    "assetId": "asset-001",
    "counterPartyAddress": "http://provider-edc:7172/api/v1/dsp",
    "protocol": "dataspace-protocol-http",
    "dataDestination": {
      "@type": "DataAddress",
      "type": "HttpProxy"
    },
    "managedResources": false
  }'

# Returns: transfer process ID
```

5. **Monitor Transfer**

```bash
curl -X GET http://localhost:9191/management/v2/transferprocesses/{transferProcessId} \
  -H "X-Api-Key: consumer-api-key"

# Check state: REQUESTED -> STARTED -> COMPLETED
```

## Step 6: Check Orchestrator Logs

```bash
docker-compose logs -f transfer-orchestrator

# Look for:
# - Contract negotiation initiated
# - Callback received
# - Transfer started
# - Policy evaluations
```

## Port Reference

| Service              | Port  | Purpose                        |
|----------------------|-------|--------------------------------|
| Transfer Orchestrator| 8080  | REST API                       |
| Transfer Orchestrator| 5006  | Java Debug Port (JDWP)         |
| Provider EDC         | 8181  | Management API                 |
| Provider EDC         | 8182  | DSP Protocol Endpoint          |
| Provider EDC         | 8183  | Public Data API                |
| Provider EDC         | 8184  | Control API                    |
| Consumer EDC         | 9191  | Management API                 |
| Consumer EDC         | 9192  | DSP Protocol Endpoint          |
| Consumer EDC         | 9193  | Public Data API                |
| Consumer EDC         | 9194  | Control API                    |
| PostgreSQL           | 5432  | Database                       |
| Redis                | 6379  | Cache                          |
| Kafka                | 9092  | Internal Broker                |
| Kafka                | 29092 | External Broker (localhost)    |
| Kafka UI             | 8081  | Kafka Web UI                   |

## Troubleshooting

### EDC containers fail to start

**Problem**: EDC image not found
```
Error: image eclipse-dataspaceconnector/edc-controlplane-memory:0.7.1 not found
```

**Solution**: The official Eclipse EDC doesn't publish all variants to Docker Hub. You have two options:

1. **Build EDC from source**
   ```bash
   git clone https://github.com/eclipse-edc/Connector.git
   cd Connector
   ./gradlew build
   # Follow EDC build instructions to create Docker images
   ```

2. **Use pre-built MVD images**
   - Minimum Viable Dataspace provides ready-to-use EDC images
   - See: https://github.com/eclipse-edc/MinimumViableDataspace

### Healthcheck fails

**Check logs**:
```bash
docker-compose logs provider-edc
docker-compose logs consumer-edc
```

**Common issues**:
- Port conflicts (check if ports 8181, 9191 are in use)
- Memory issues (EDC needs ~512MB RAM per instance)

### Contract negotiation fails

**Check**:
1. Provider has assets loaded: `curl http://localhost:8181/management/v3/assets/request ...`
2. Policy definitions exist
3. Contract definitions exist
4. DSP endpoints are reachable from containers (not localhost in DSP URLs)

### No callbacks received

**Verify**:
1. Callback URL uses container hostname (e.g., `http://transfer-orchestrator:8080/api/v1/callbacks`)
2. Orchestrator callback controller is implemented
3. Check orchestrator logs for incoming HTTP requests

## Next Steps

1. Implement your policy engine logic (TimeBasedPolicy, RateLimitPolicy, etc.)
2. Add more complex assets to the provider
3. Test transfer cancellation and failure scenarios
4. Add monitoring and metrics collection
5. Implement your audit trail persistence

## Useful Commands

```bash
# View all logs
docker-compose logs -f

# Restart just the orchestrator
docker-compose restart transfer-orchestrator

# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v

# Check EDC connector versions
docker exec provider-edc cat /app/VERSION || echo "No version file"

# Access PostgreSQL
docker exec -it postgres psql -U orchestrator -d orchestrator
```

## References

- [Eclipse EDC Documentation](https://eclipse-edc.github.io/docs/)
- [EDC Management API Spec](https://eclipse-edc.github.io/docs/#/submodule/Connector/docs/developer/management-api)
- [Dataspace Protocol](https://docs.internationaldataspaces.org/ids-knowledgebase/v/dataspace-protocol)