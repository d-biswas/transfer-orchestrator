# Transfer Orchestration Service

A Spring Boot microservice that acts as an intelligent orchestration layer on top of **Eclipse Dataspace Connector (EDC)** for the Catena-X automotive supply chain ecosystem.

## Features

- **Policy-aware transfers** - Time-based, rate limits, geographic, certification policies
- **Transfer lifecycle management** - Complete state machine for transfer requests
- **Event-driven architecture** - Kafka-based async processing
- **Audit & compliance** - Immutable audit logs for all transfers
- **Observability** - Health checks, Prometheus metrics, structured logging
- **Mock EDC integration** - Built-in mock for development (no external EDC needed)

---

## Technology Stack

- **Java 25** + Spring Boot 3.5.6
- **PostgreSQL** - Primary database
- **Kafka** - Event streaming
- **Redis** - Caching
- **Gradle** - Build tool
- **Docker** - Containerization

---

## Setup Instructions

### Prerequisites

- Java 25 (or compatible JDK)
- Docker & Docker Compose
- Gradle 9.2.1 (or use wrapper)

### Quick Start

```bash
# Clone repository
git clone https://github.com/d-biswas/transfer-orchestrator
cd transfer-orchestrator

# Build Docker image
./gradlew dockerBuildImage

# Start infrastructure (Orchestrator, Postgres, Kafka, Redis)
docker-compose up -d
```

### Verify Setup

```bash
# Check health
curl http://localhost:8080/actuator/health

# View API docs
open http://localhost:8080/swagger-ui.html
```

---

## How to Run the Application

### Option 1: Local Development

```bash
./gradlew bootRun
```

### Option 2: Build and Run JAR

```bash
./gradlew clean bootJar
java -jar build/libs/transfer-orchestrator-0.0.1-SNAPSHOT.jar
```

### Option 3: Docker (Quick)

```bash
# Build Docker image
./gradlew clean dockerBuildImage

# Run with docker-compose (includes all dependencies)
docker-compose up -d
```

## How to Run Tests

### Run All Tests

```bash
./gradlew test
```

### Run Specific Test Class

```bash
./gradlew test --tests TransferOrchestratorIntegrationTest
```


### Test Categories

- **Unit tests** - Service logic, policy evaluation
- **Integration tests** - Full transfer lifecycle with embedded Kafka/DB
- **API tests** - Controller endpoints

---

## Available Endpoints

### Transfer Management
- `POST /api/v1/transfers` - Initiate transfer
- `GET /api/v1/transfers/{id}` - Get transfer status
- `GET /api/v1/transfers` - List transfers (paginated)
- `DELETE /api/v1/transfers/{id}` - Cancel transfer

### Audit & Analytics
- `GET /api/v1/transfers/{id}/audit` - Get audit log
- `GET /api/v1/analytics/transfers` - Transfer analytics
- `GET /api/v1/analytics/compliance-report` - Compliance report

### Policy
- `POST /api/v1/policies/evaluate` - Test policy evaluation

### Monitoring
- `GET /actuator/health` - Health check
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/prometheus` - Prometheus metrics

### Documentation
- `GET /swagger-ui.html` - Swagger UI
- `GET /v3/api-docs` - OpenAPI spec (JSON)

---

## Observability

### Health Checks

```bash
curl http://localhost:8080/actuator/health
```

### Metrics

```bash
# All metrics
curl http://localhost:8080/actuator/metrics

# Specific metric
curl http://localhost:8080/actuator/metrics/http.server.requests

# Prometheus format
curl http://localhost:8080/actuator/prometheus
```

See [OBSERVABILITY.md](OBSERVABILITY.md) for detailed monitoring setup.

---

## Known Limitations

1. **Mock EDC Only** - Uses built-in mock EDC connector for development. Real EDC integration requires additional configuration.

2. **Single Instance** - Not designed for horizontal scaling yet. Kafka consumer group and database locking need enhancement for multi-instance deployment.

3. **In-Memory Policy Storage** - Policies are configured in code. External policy management system not implemented.

4. **Basic Policy Engine** - Supports simple policy types. Complex AND/OR/NOT composition partially implemented.

5. **No Authentication** - API endpoints are not secured. Add Spring Security for production use.


## Architecture

### Transfer State Machine

```
REQUESTED
  → POLICY_EVALUATION
  → APPROVED / DENIED
  → CONTRACT_NEGOTIATION
  → NEGOTIATED
  → TRANSFER_IN_PROGRESS
  → COMPLETED / FAILED / CANCELLED
```

### Package Structure

```
com.company.orchestrator
 ├── api/                 # REST controllers
 ├── domain/
 │   ├── model/           # Entities, value objects
 │   └── service/         # Business logic
 ├── policy/
 │   ├── engine/          # Policy evaluation
 │   ├── model/           # Policy definitions
 │   └── service/         # Policy services
 ├── audit/
 │   ├── model/           # Audit entities
 │   └── service/         # Audit services
 └── infrastructure/
     ├── edc/             # EDC client
     ├── persistence/     # JPA repositories
     ├── events/          # Kafka handlers
     └── config/          # Spring configuration
```

---

## Additional Documentation

- [OBSERVABILITY.md](OBSERVABILITY.md) - Monitoring & metrics

---

## License

[Add your license here]

---