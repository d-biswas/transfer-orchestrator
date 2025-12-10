
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

1. Configure database in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/orchestrator
    username: orchestrator
    password: orchestrator
```

1. Start required containers (Postgres, Kafka, EDC mock) using Docker Compose:

```bash
docker-compose up
```

1. Build and run the application:

```bash
./gradlew clean bootRun
```

1. Access API documentation (Swagger/OpenAPI):

```
http://localhost:8080/swagger-ui.html
```

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


