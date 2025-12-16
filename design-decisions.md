# Design Decisions Document

## Transfer Orchestration Service

**Version:** 1.0
**Last Updated:** 2025-12-16
**Status:** Living Document

---

## 1. Policy Engine Design

### Decision: Composable Policy Engine with JSON Configuration

**Status:** ✅ Implemented

**Approach:**

We implemented a **composable policy evaluation engine** that supports:

- **Five core policy types:**
  - `TimeBasedPolicy` - Time window restrictions (e.g., business hours only)
  - `RateLimitPolicy` - Request rate limiting using Redis sliding window
  - `GeographicPolicy` - Region-based access control
  - `CertificationPolicy` - Participant credential validation
  - `UsagePolicy` - Data usage purpose restrictions

- **Policy composition:**
  - `AndPolicy` - All policies must pass
  - `OrPolicy` - At least one policy must pass
  - `NotPolicy` - Inverts policy result

**Rationale:**

- **Flexibility:** Composable policies support complex business rules without code changes
- **Auditability:** JSON configuration provides clear policy definitions
- **Performance:** In-memory evaluation with Redis for distributed rate limiting
- **Fail-Safe:** Rate limiter fails open on Redis errors to prevent cascading failures

**Trade-offs:**

✅ **Pros:**
- Simple to understand and test
- Fast evaluation (in-memory)
- Human-readable violation messages

❌ **Cons:**
- Policies loaded at startup (requires restart for updates)
- Limited policy versioning/history

**Future Enhancements:**

🔮 **Planned:**
- External policy management system (API-driven updates)
- Dynamic policy reload without restart
- Policy versioning and audit trail

---

## 2. State Management

### Decision: Database as Source of Truth with Event-Driven Architecture

**Status:** ✅ Implemented

**Approach:**

We use **PostgreSQL as the authoritative state store** combined with **Kafka for event propagation**:

**State Machine:**
```
REQUESTED (1)
  → POLICY_EVALUATION (2)
  → APPROVED (3) / DENIED (4)
  → CONTRACT_NEGOTIATION (5)
  → NEGOTIATED (6)
  → TRANSFER_IN_PROGRESS (7)
  → COMPLETED (8) / FAILED (9) / CANCELLED (10)
```

**Key Components:**

1. **Transfer State Persistence:**
   - Every state transition persisted to `transfer_requests` table
   - Optimistic locking with `@Version` field prevents concurrent conflicts
   - Indexed on `status`, `consumer_id`, `created_at` for fast queries

2. **State History Tracking:**
   - `transfer_state_history` table records all transitions
   - Enables analytics, debugging, and recovery analysis

3. **Audit Trail:**
   - Immutable `audit_logs` table with JSONB metadata
   - Records actor, event type, message, and context

4. **Event-Driven Propagation:**
   - State changes emit Kafka events (`TransferStatusChanged`)
   - Decouples orchestration from downstream consumers


**Rationale:**

- **Reliability:** Database ensures state survives restarts
- **Consistency:** Optimistic locking prevents race conditions
- **Auditability:** Complete history for compliance and debugging
- **Scalability:** Event-driven architecture enables horizontal scaling

**Trade-offs:**

✅ **Pros:**
- State survives system restarts
- Complete audit trail for compliance

❌ **Cons:**
- Database can become bottleneck at scale
- No automatic retry for stuck transfers

**Future Enhancements:**

🔮 **Planned:**
- Automatic timeout handling for stuck transfers
- Dead letter queue processing for failed events
- Read replicas for query performance

---

## 3. Failure Handling

### Decision: Kafka Retry with DLQ + Optimistic Locking

**Status:** ⚠️ Partially Implemented

**Approach:**

**Currently Implemented:**

1. **Kafka Event Retry:**
   - `@Retryable` annotation with exponential backoff
   - Configuration: 5 max attempts, 5s initial delay, 2x multiplier
   - Manual acknowledgment for fine-grained control

2. **Dead Letter Queue (DLQ):**
   - Non-retriable errors sent to `orchestrator-dlq` topic
   - DLQ handler logs and persists failed events

3. **Database Concurrency:**
   - Optimistic locking prevents concurrent update conflicts
   - `@Transactional` for atomic operations

4. **Graceful Degradation:**
   - Rate limiter fails open on Redis errors
   - Health checks detect component failures

**Missing Components:**

❌ **Not Implemented:**
- Circuit breakers for EDC calls (no Resilience4j integration)
- Transfer-level timeouts (transfers can hang indefinitely)
- Bulkhead pattern for resource isolation

**Rationale:**

- **Kafka Retry:** Handles transient failures automatically
- **DLQ:** Prevents poison messages from blocking processing
- **Optimistic Locking:** Simple concurrency control without distributed locks

**Trade-offs:**

✅ **Pros:**
- Simple retry logic easy to understand
- DLQ prevents message loss
- Optimistic locking performs well at low contention

❌ **Cons:**
- No circuit breakers → cascading failures possible
- No timeouts → stuck transfers not auto-recovered
- Single thread pool → no isolation between transfer types

**Future Enhancements:**

🔮 **Planned:**
- **Add Resilience4j**
- **Transfer Timeouts:** Monitor and auto-cancel stuck transfers
- **Bulkhead Pattern:** Isolate EDC calls to prevent resource exhaustion
- **Jittered Retry:** Add randomization to prevent thundering herd

---

## 4. Scalability

### Decision: Stateless Application + Event-Driven Architecture

**Status:** ⚠️ Single-Instance Design (Horizontal Scaling Planned)

**Current Design:**

**Stateless Application Layer:**
- All state in PostgreSQL and Redis (no in-memory state)
- Kafka consumer groups enable horizontal scaling
- Connection pooling (HikariCP: 10 max connections)

**Async Processing:**
- Event-driven via Kafka decouples components
- Asynchronous EDC callbacks prevent blocking
- Configurable concurrency (`kafka.listener.concurrency=5`)

**Caching:**
- Redis for transfer status lookups
- Distributed rate limiting using Redis sorted sets

**Database Optimization:**
- Indexed columns: `status`, `consumer_id`, `created_at`
- Batch operations (Hibernate `batch_size: 30`)
- Optimistic locking for concurrent updates


**Scaling Strategy for 10,000 Concurrent Transfers:**

🔮 **Phase 1: Vertical Scaling (Target: 2,500 transfers)**
1. Increase DB connection pool (10 → 50)
2. Add DB read replicas for queries
3. Increase Kafka partitions (3 → 12)
4. Increase consumer concurrency (5 → 20)

🔮 **Phase 2: Horizontal Scaling (Target: 10,000 transfers)**
1. **Multi-Instance Deployment:**
   - Deploy 3-5 application instances

2. **Database Sharding:**
   - Shard by `consumer_id` or `transfer_id` hash

3. **Kafka Optimization:**
   - Increase partitions to match instance count
   - Enable compression (LZ4 or Snappy)

4. **Caching Layer:**
   - Redis cluster mode for high availability
   - Cache policy evaluation results

5. **Resource Isolation:**
   - Separate thread pools for EDC operations
   - Bulkhead pattern per transfer type
   - Circuit breakers prevent cascading failures

**Rationale:**

- **Event-Driven:** Natural fit for horizontal scaling
- **Stateless:** Instances can be added/removed dynamically
- **Database-Centric:** Simplifies consistency at cost of throughput

**Trade-offs:**

✅ **Pros:**
- Simple single-instance deployment
- Strong consistency via DB
- Easy to develop and test

❌ **Cons:**
- Database becomes bottleneck at scale
- In-memory policy loading limits scale

**Future Enhancements:**

🔮 **Planned:**
- Multi-instance deployment with distributed locks
- Database read replicas and sharding
- Policy storage in external database
- Kafka partition optimization

---

## 5. Observability

### Decision: Spring Actuator + Prometheus + Structured Audit Logs

**Status:** ✅ Partially Implemented (Distributed Tracing Planned)

**Approach:**

**Metrics Exposure:**

1. **Spring Actuator Endpoints:**
   - `/actuator/health` - Component health (DB, Kafka, Redis)
   - `/actuator/metrics` - Available metrics
   - `/actuator/prometheus` - Prometheus scrape endpoint

2. **Auto-Metrics (via Micrometer):**
   - **JVM:** Memory, GC, threads
   - **HTTP:** Request count, duration, status codes
   - **Database:** HikariCP connection pool metrics
   - **Kafka:** Consumer lag, producer throughput

3. **Health Checks:**
   - Kubernetes liveness/readiness probes
   - Component-level health indicators
   - Custom health check for EDC connectivity

**Logging:**

- **Structured Logging:** Log4j2 with correlation IDs
- **Audit Logs:** Stored in PostgreSQL with JSONB metadata
- **Log Levels:** Configurable per package

**Rationale:**

- **Prometheus:** Industry standard for metrics collection
- **Spring Actuator:** Zero-configuration observability
- **Database Audit Logs:** Immutable compliance trail
- **Correlation IDs:** Enable request tracing

**Trade-offs:**

✅ **Pros:**
- Comprehensive auto-metrics out-of-the-box
- Kubernetes-ready health checks
- Immutable audit trail in database

❌ **Cons:**
- No distributed tracing (OpenTelemetry/Elastic APM)
- No pre-built Grafana dashboards

**Future Enhancements:**

🔮 **Planned:**
- **Elastic APM:** Distributed tracing across services
- **Dashboards:** Grafana dashboards for operations team
- **Log Aggregation:** Loki integration

---

## 6. Data Sovereignty & GAIA-X Compliance

### Decision: Policy-Based Sovereignty with EDC Integration (Partial)

**Status:** ⚠️ Foundation Implemented, Missing for Full GAIA-X Compliance:

**Current Implementation:**

**1. Policy-Based Controls:**
- **GeographicPolicy:** Enforces region-based access restrictions
- **CertificationPolicy:** Validates participant credentials
- **UsagePolicy:** Restricts data usage purposes

**2. Immutable Audit Trail:**
- All transfers logged with actor, timestamp, metadata
- JSONB metadata supports flexible compliance data
- Append-only `audit_logs` table

**3. Compliance Reporting:**
- `/api/audit/transfers/{transferId}` - Transfer audit history
- `/api/analytics/compliance` - Compliance reports by date range

**4. EDC Integration (Mock):**
- Designed for Eclipse Dataspace Connector

**Rationale:**

- **Policy Engine:** Provides foundation for sovereignty enforcement
- **Audit Trail:** Meets compliance reporting requirements
- **EDC Integration:** Path to GAIA-X compatibility via production EDC

**Path to Full Compliance:**

🔮 **To Achieve Full GAIA-X Compliance:**

1. **Use Real EDC** - Replace mock connector with production Eclipse Dataspace Connector
2. **Add GAIA-X Identity** - Integrate official identity provider for verified participants
3. **Switch to ODRL** - Replace custom policies with standard Open Digital Rights Language
4. **Track Data Location** - Add metadata showing where data is stored and enforce residency rules
5. **Get Certified** - Integrate with GAIA-X Federation Services for automated compliance proof

**Trade-offs:**

✅ **Pros:**
- Policy engine provides sovereignty controls
- Audit trail supports compliance reporting

❌ **Cons:**
- Mock EDC not production-ready
- Custom policy language not ODRL-compatible
- No participant credential verification

**Current Compliance Level:**

| Requirement | Status | Gap |
|-------------|--------|-----|
| Access Control | ✅ Implemented | GeographicPolicy enforces regions |
| Audit Trail | ✅ Implemented | Immutable logs in PostgreSQL |
| Usage Control | ⚠️ Partial | UsagePolicy but not ODRL |
| Identity Verification | ❌ Missing | No GAIA-X IdP integration |
| Data Residency | ❌ Missing | No location metadata |
| EDC Compliance | ⚠️ Mock Only | Need production EDC |

---

## Cross-Cutting Decisions

### API Design: REST + Event-Driven

**Synchronous (REST):**
- Transfer initiation: `POST /api/transfers/initiate`
- Status queries: `GET /api/transfers/{id}`
- Cancellation: `POST /api/transfers/{id}/cancel`

**Asynchronous (Kafka Events):**
- State changes: `transfer.status.changed`
- Lifecycle hooks: `transfer.requested`, `transfer.completed`

**Rationale:** REST for client interaction, events for internal orchestration

---

### Database Schema: Relational (PostgreSQL)

**Choice:** PostgreSQL over NoSQL

**Rationale:**
- ACID transactions for state consistency
- Rich querying for analytics
- JSONB for flexible metadata

---

### Testing Strategy: Integration-First

**Approach:**
- Full integration tests with embedded Kafka
- TestContainers for PostgreSQL
- Mock EDC for deterministic testing

**Rationale:** Catch integration issues early, realistic testing

---

## Summary: Implementation Status

| Area | Status | Completeness |
|------|--------|--------------|
| **Policy Engine** | ✅ Implemented | 70%          |
| **State Management** | ✅ Implemented | 95%          |
| **Failure Handling** | ⚠️ Partial | 50%          |
| **Scalability** | ⚠️ Single-Instance | 50%          |
| **Observability** | ✅  Implemented | 70%          |
| **Data Sovereignty** | ⚠️ Foundation | 40%          |

**Overall Readiness:** Production-ready for single-instance deployments with planned enhancements for scale and full GAIA-X compliance.

