
# Transfer Orchestration Service

## Project Overview

The **Transfer Orchestration Service** is a Spring Boot microservice that acts as an intelligent orchestration layer on top of **Eclipse Dataspace Connector (EDC)**. It is designed for the **Catena-X automotive supply chain** ecosystem to securely share sensitive data such as production metrics, quality reports, and forecasts with tier-1 and tier-2 suppliers.

The service provides:

- **Policy-aware data transfers**: Evaluates complex policies (time-based, rate limits, geographic, certification, usage) before initiating transfers.
- **Orchestrated transfer workflows**: Manages multi-step transfer lifecycle with retries, error handling, and state tracking.
- **Audit and compliance**: Maintains immutable, append-only logs for each transfer.
- **Analytics and monitoring**: Provides metrics for transfer performance and policy violations.

---

## Features

- Transfer Lifecycle Management: `REQUESTED → POLICY_EVALUATION → APPROVED/DENIED → CONTRACT_NEGOTIATION → NEGOTIATED → TRANSFER_IN_PROGRESS → COMPLETED / FAILED / CANCELLED`
- Flexible Policy Engine supporting AND/OR/NOT composition
- EDC Integration for contract negotiation and transfer execution
- Retry mechanism with exponential backoff
- Kafka-based event handling (optional)
- Observability: Metrics, logging, tracing via Spring Actuator and Prometheus
- Docker Compose setup for local development

---

## Technology Stack

- **Language**: Java 25
- **Framework**: Spring Boot 3.5+
- **Build Tool**: Gradle
- **Database**: PostgreSQL (H2 for local testing)
- **Messaging**: Kafka (optional for event-driven architecture)
- **Monitoring**: Prometheus, Micrometer
- **API Documentation**: OpenAPI / Swagger
- **Testing**: JUnit 5

---

## Project Structure

```
com.company.orchestrator
 ├── api/                # REST controllers
 ├── domain/
 │   ├── model/          # Domain entities and value objects
 │   └── service/        # Business logic
 ├── policy/
 │   ├── engine/         # Policy evaluation core
 │   ├── model/          # Policy definitions
 │   └── service/        # Policy services
 ├── audit/
 │   ├── model/          # Audit entities
 │   └── service/        # Audit services
 └── infrastructure/
     ├── edc/            # EDC client implementations
     ├── persistence/    # JPA repositories, DB adapters
     └── config/         # Spring configuration
```

---

## Quick Start

🚀 **New to this project? Start here:**

1. **[QUICKSTART.md](QUICKSTART.md)** - Get running in 5 minutes
2. **[EDC-SETUP.md](EDC-SETUP.md)** - Complete EDC setup guide with plain Eclipse EDC

---

## Mock EDC Mode (Default)

**🎉 No real EDC connector required!** The application uses a built-in mock EDC **by default**.

The Mock EDC provides:
- ✅ No external EDC dependency - everything runs in-memory
- ✅ Realistic async behavior with configurable delays
- ✅ Automatic callbacks simulating real EDC state transitions
- ✅ Contract negotiation flow: `REQUESTED → OFFERED → AGREED → FINALIZED`
- ✅ Transfer process flow: `INITIAL → PROVISIONING → STARTED → COMPLETED`
- ✅ Perfect for development and testing

### Using the Application (Default = Mock EDC)

**Just run normally - mock is active by default:**
```bash
./gradlew bootRun
```

That's it! No EDC connectors needed. The mock will handle all contract negotiations and transfers.

### Configuration

Mock behavior can be customized in `application-mock-edc.yml` or via environment variables:

```yaml
edc:
  mock:
    enabled: true
    callback-base-url: "http://localhost:8080"

    # Delays (milliseconds) - adjust for faster/slower testing
    negotiation-requested-to-offered-delay-ms: 500
    negotiation-offered-to-agreed-delay-ms: 500
    negotiation-agreed-to-finalized-delay-ms: 500

    transfer-initial-to-provisioning-delay-ms: 300
    transfer-provisioning-to-started-delay-ms: 500
    transfer-started-to-completed-delay-ms: 1000

    # Simulate failures (0.0 = no failures, 0.2 = 20% failure rate)
    failure-rate: 0.0
```

### Switching to Real EDC (Optional)

**Development/Testing (Mock EDC - DEFAULT):**
```bash
# Just run normally - mock is the default
./gradlew bootRun
```

**Production (Real EDC):**
```bash
# Use the 'real-edc' profile to enable actual EDC connectors
./gradlew bootRun --args='--spring.profiles.active=real-edc'

# Or via environment variable
export SPRING_PROFILES_ACTIVE=real-edc
./gradlew bootRun
```

---

## Setup & Running Locally

### Prerequisites

- Java 25
- Gradle
- Docker & Docker Compose
- PostgreSQL (or H2 for testing)
- Kafka & Zookeeper (optional)

### Steps

1. Clone the repository:

```bash
git clone <repo-url>
cd transfer-orchestrator
```

2. Build and start everything:

```bash
# Build the application
./gradlew clean bootJar
docker build -t transfer-orchestrator:0.0.1-SNAPSHOT .

# Start all services (Orchestrator, EDC connectors, Postgres, Kafka, Redis)
docker-compose up -d

# Load sample data into provider EDC
./setup-provider-edc.sh
```

3. Verify services are running:

```bash
docker-compose ps

# Check health endpoints
curl http://localhost:8080/actuator/health      # Orchestrator
curl http://localhost:8181/api/check/health     # Provider EDC
curl http://localhost:9191/api/check/health     # Consumer EDC
```

4. Access services:

- Orchestrator API: http://localhost:8080
- Swagger/OpenAPI: http://localhost:8080/swagger-ui.html
- Kafka UI: http://localhost:8081
- Provider EDC Management API: http://localhost:8181/management
- Consumer EDC Management API: http://localhost:9191/management

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST   | /api/v1/transfers | Initiate a new transfer |
| GET    | /api/v1/transfers/{id} | Get transfer status |
| DELETE | /api/v1/transfers/{id} | Cancel transfer |
| GET    | /api/v1/transfers/{id}/audit | Get audit log for a transfer |
| GET    | /api/v1/transfers | List transfers (paginated) |
| GET    | /api/v1/analytics/transfers | Get transfer analytics |
| POST   | /api/v1/policies/evaluate | Test policy evaluation |

---

## Testing

- Unit tests: `./gradlew test`
- Integration tests (EDC, DB, Kafka mock) can be run via JUnit or test containers.
- Performance/load testing can be added using **Gatling** or **JMeter**.

---

## Observability

- Metrics: Exposed via `/actuator/prometheus`
- Health checks: `/actuator/health`
- Tracing (optional): OpenTelemetry compatible
- Logging: Structured logs via SLF4J/Logback

---

## Development Guidelines

- Follow **clean architecture** principles.
- Keep **controllers thin**; business logic resides in domain services.
- Infrastructure layer handles EDC, DB, and Kafka integration.
- Persist all transfer state transitions and policy evaluation results.
- Write **idempotent operations** to handle retries safely.
- Include **audit logs** for compliance.

---

## Docker Compose Setup

Sample services included:

- `transfer-orchestrator` application
- PostgreSQL
- Kafka + Zookeeper (optional)
- EDC mock container

Run all with:

```bash
docker-compose up -d


