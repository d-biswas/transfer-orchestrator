# EDC Setup Journey - Transfer Orchestrator Project

This document chronicles the attempts to set up Eclipse Dataspace Connector (EDC) for the Transfer Orchestrator project, documenting challenges encountered and solutions attempted.

---

## Table of Contents
1. [Attempt 1: Tractus-X EDC Runtime-Memory](#attempt-1-tractus-x-edc-runtime-memory)
2. [Attempt 2: Eclipse EDC Runtime (Manual JAR Build)](#attempt-2-eclipse-edc-runtime-manual-jar-build)
3. [Attempt 3: Eclipse EDC Template-Basic](#attempt-3-eclipse-edc-template-basic)
4. [Attempt 4: MinimumViableDataspace (MVD)](#attempt-4-minimumviabledataspace-mvd)
5. [Key Learnings](#key-learnings)
6. [Current Status](#current-status)
7. [Recommendations](#recommendations)

---

## Attempt 1: Tractus-X EDC Runtime-Memory

### Approach
Attempted to use the Tractus-X EDC runtime-memory variant (`tractusx/edc-runtime-memory`), which is designed for in-memory testing without persistent storage.

### Repository
`https://github.com/eclipse-tractusx/tractusx-edc`

### Issues Encountered

#### Issue: BDRS (Business Partner Data Registry Service) Requirements
**Error:**
```
Missing identity and membership verification configuration
BDRS (Business Partner Data Registry Service) not configured
```

**Root Cause:**
- Tractus-X EDC includes additional Catena-X/automotive-specific requirements
- Requires BDRS for business partner verification
- Requires membership credentials validation
- More complex than standard EDC Identity Trust

**What is BDRS:**
- Business Partner Data Registry Service
- Part of Catena-X dataspace infrastructure
- Verifies business partner identity and membership status
- Required for automotive supply chain data sharing

**Configuration Requirements:**
```yaml
# Tractus-X specific requirements
TX_EDC_IAM_IATP_BDRS_SERVER_URL: "https://bdrs-server-url"
TX_EDC_IAM_IATP_TRUST_ANCHORS: "did:web:trusted-issuer"
EDC_IAM_ISSUER_ID: "did:web:participant-id"
# ... plus standard Identity Trust configuration
```

**Why This Failed:**
- BDRS service not available for local testing
- Tractus-X targets production Catena-X scenarios
- Too heavyweight for local development/testing

### Outcome
❌ **Unsuccessful** - Too complex for local testing, moved to next approach

**Key Learning:** Tractus-X EDC is production-focused with Catena-X-specific requirements, not suitable for simple local development.

---

### Alternative Attempted: Tractus-X Mock-Connector

**Approach:**
After runtime-memory failed, attempted to use Tractus-X Mock-Connector as a simpler alternative for local testing.

**Discovery:**
- Found that mock-connector is **no longer maintained**
- Repository marked as **deprecated**
- No active support or updates

**Outcome:**
❌ **Unsuccessful** - Deprecated/unmaintained, not viable for development

---

## Attempt 2: Eclipse EDC Runtime (Shadow JAR Build)

### Approach
Built EDC runtime as a shadow JAR (fat JAR) from Eclipse EDC Samples repository.

### Repository
`https://github.com/eclipse-edc/Samples.git`

### Build Process
```bash
cd ~/Projects/Personal/edc-samples
./gradlew :transfer:transfer-00-prerequisites:connector:shadowJar -x test
```

**Result:** Successfully created ~35MB fat JAR with all dependencies bundled

### What Worked
✅ Build successful
✅ Created executable fat JAR (`connector.jar`)
✅ JAR contains Control Plane, Data Plane, Management API, DSP Protocol
✅ Successfully integrated into Docker setup
✅ Management APIs responding

### Issues Encountered

#### Issue: Contract Negotiation Fails - Callback Timeout
**Problem:**
- Contract negotiation initiated successfully
- System stuck waiting for EDC callback
- Callbacks never received from EDC connector
- Transfer state remains in NEGOTIATION

**Similar to Template-Basic Issue:**
Likely same root cause - missing IAM/Identity Trust components in sample-based connector

### Limitations
- Based on EDC Samples, not main repository
- Uses in-memory stores only
- Limited customization compared to Template-Basic
- Sample configuration designed for basic use cases
- No proper IAM/Identity Trust setup

### Outcome
❌ **Unsuccessful** - JAR built successfully but contract negotiation failed (callback timeout), moved to Template-Basic for better IAM support

**See:** `edc-runtime/BUILD-EDC-JAR.md` for detailed build instructions

---

## Attempt 3: Eclipse EDC Template-Basic

### Repository
`https://github.com/eclipse-edc/Template-Basic.git`

### Initial Setup

**Build Configuration:**
```bash
cd Template-Basic
./gradlew clean :runtimes:controlplane:build
./gradlew :runtimes:controlplane:dockerize
```

### Issues Encountered & Solutions

#### Issue 1: Java Version Incompatibility
**Error:**
```
java.lang.IllegalArgumentException: 25.0.1
at org.jetbrains.kotlin.com.intellij.util.lang.JavaVersion.parse
```

**Root Cause:** Gradle Kotlin plugin doesn't recognize Java 25

**Solution:**
```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew build
```

✅ **Fixed** - Use Java 21 for builds

---

#### Issue 2: STS Configuration Missing
**Error:**
```
ERROR: No setting found for key edc.iam.sts.oauth.token.url
```

**Root Cause:** Template-Basic includes `identity-trust-sts-remote-client` which requires STS service

**Solution:**
Modified `runtimes/controlplane/build.gradle.kts`:
```kotlin
dependencies {
    runtimeOnly(libs.edc.bom.controlplane) {
        // Exclude STS remote client to avoid IAM configuration requirements
        exclude(group = "org.eclipse.edc", module = "identity-trust-sts-remote-client")
    }
}
```

✅ **Fixed** - EDC starts without STS errors

---

#### Issue 3: Missing Required Services (After STS Exclusion)
**Error:**
```
EdcInjectionException: The following injected fields were not provided:
- Field "identityService" of type [IdentityService]
- Field "audienceResolver" of type [AudienceResolver]
```

**Root Cause:** Excluding STS removed providers for required services

**Solution:**
Reverted to only excluding STS remote client (kept Identity Trust core modules)

✅ **Fixed** - EDC starts successfully

---

#### Issue 4: Incorrect DSP Protocol Path
**Error:** 404 errors when accessing protocol endpoints

**Problem:**
```java
providerUrl("http://provider-edc:8282/api/v1/dsp")  // ❌ WRONG
```

**Solution:**
```java
providerUrl("http://provider-edc:8282/protocol")  // ✅ CORRECT
```

Template-Basic uses `/protocol` as the DSP endpoint, not `/api/v1/dsp`

---

#### Issue 5: Contract Negotiation Fails - Empty Scope
**Error:**
```
WARNING: No TokenDecorator was registered. The 'scope' field will be empty
Unable to obtain credentials: Scope string invalid: input string was null or empty
```

**Root Cause:**
- EDC Identity Trust requires OAuth2 scopes for authentication
- Without STS, there's no TokenDecorator to generate scopes
- Contract negotiations fail because scope field is null/empty

**Attempted Solutions:**
1. ❌ Mock OAuth2 environment variables - Didn't help
2. ❌ Exclude all Identity Trust modules - Broke required services
3. ❌ Add stub IAM configuration - Still no TokenDecorator

**Current Status:** ❌ **UNRESOLVED**

---

## Attempt 4: MinimumViableDataspace (MVD)

### Repository
`https://github.com/eclipse-edc/MinimumViableDataspace.git`

### Rationale
MVD is designed for local testing/development - expected simpler IAM setup

### Build Process
```bash
cd MinimumViableDataspace
./gradlew build
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :launchers:controlplane:dockerize :launchers:dataplane:dockerize
```

✅ **Build Successful** - Created `controlplane:latest` (v0.15.0)

### Discovery
**MVD uses FULL Identity Trust stack:**
- Identity Hub
- STS (Secure Token Service)
- Verifiable Credentials
- DID (Decentralized Identifiers)

**Conclusion:**
❌ **Not Simpler** - MVD embraces full Catena-X identity model, not a workaround

---

## Key Learnings

### 1. EDC Version Matters
- **EDC v0.10+**: Requires Identity Trust / IAM
- **EDC v0.8 and earlier**: Simpler, no mandatory IAM
- **Trade-off**: Security/compliance vs simplicity

### 2. Docker Networking
- **Always use container internal ports** when services communicate inside Docker network
- **Host ports are only for external access** from host machine
- Format: `HOST:CONTAINER` in port mappings

### 3. DSP Protocol Paths
- Template-Basic: `/protocol`
- **Not** `/api/v1/dsp` or `/protocol/api/v1/dsp`
- Check actual EDC logs for listening contexts

### 4. Identity Trust is Non-Optional (Modern EDC)
The TokenDecorator pattern is required for:
```
Consumer → OAuth Scope → Provider Authentication → Contract Negotiation
```

Without it, negotiations fail at the authentication step.

### 5. EDC Flavors Comparison

| Flavor | Version | IAM Required | Best For |
|--------|---------|--------------|----------|
| Template-Basic | v0.10+ | Yes (STS or custom) | Production with IAM |
| MVD | v0.15+ | Yes (full stack) | Catena-X scenarios |
| Tractus-X | Various | Depends | Automotive dataspace |
| Older EDC | <v0.8 | No | Simple testing |

---

## Current Status

### ✅ What's Working

1. **Transfer Orchestrator Application**
   - Policy evaluation engine
   - State machine (REQUESTED → APPROVED → NEGOTIATION → ...)
   - Kafka event publishing/handling
   - PostgreSQL persistence
   - Redis caching
   - Audit logging
   - REST APIs

2. **EDC Management APIs**
   - Both connectors (provider/consumer) healthy
   - Assets can be created/queried
   - Policies can be created/queried
   - Contract definitions can be created/queried
   - Health checks passing

3. **Code Quality**
   - All port mappings corrected
   - Exception handling fixed
   - Test configuration complete
   - Build passing

### ❌ What's Not Working

1. **Contract Negotiation**
   - Fails due to missing OAuth scopes
   - Requires TokenDecorator implementation
   - Needs either:
     - Proper STS service, OR
     - Custom TokenDecorator extension

2. **End-to-End Data Transfer**
   - Blocked by contract negotiation failure
   - Cannot test full transfer flow
   - Callbacks not being triggered

---

## Recommendations

### Option 1: Implement TokenDecorator Extension ⭐ **Recommended for Production**

**Pros:**
- Enables full EDC functionality
- Production-ready approach
- Aligns with Catena-X standards

**Cons:**
- Requires EDC extension development
- More complex setup

**Implementation:**
```java
// Create custom extension in Template-Basic
@Extension("Stub TokenDecorator")
public class StubTokenDecoratorExtension implements ServiceExtension {
    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(TokenDecorator.class, new StubTokenDecorator());
    }
}
```

### Option 2: Use Older EDC Version

**Pros:**
- No IAM complexity
- Simple local testing

**Cons:**
- Not production-ready
- Missing modern features
- Security limitations

### Option 3: Mock EDC Responses for Testing ⭐ **Recommended for Development**

**Pros:**
- Test orchestrator logic independently
- Fast iteration
- No EDC dependency issues

**Cons:**
- Not end-to-end testing
- Doesn't validate EDC integration

**Implementation:**
```java
// Mock EDC contract negotiation response
@Profile("test")
@Component
public class MockEdcClient implements EdcConnectorClient {
    public ContractNegotiationResult negotiateContract(ContractOffer offer) {
        return ContractNegotiationResult.success("mock-negotiation-id", "FINALIZED");
    }
}
```

### Option 4: Full IAM Setup with STS

**Pros:**
- Production-grade
- Full feature set
- Proper security

**Cons:**
- Complex setup (STS, Identity Hub, etc.)
- More infrastructure required

---

## Final Summary

### Journey Timeline
1. Tractus-X Runtime-Memory → BDRS requirements too complex
2. Tractus-X Mock-Connector → Deprecated/unmaintained
3. EDC Samples Shadow JAR → Build successful but contract negotiation failed (callback timeout)
4. **Template-Basic** → Got furthest, blocked by IAM/TokenDecorator
5. MVD → Discovered it requires even more IAM

### Key Blocker
**Modern EDC (v0.10+) tightly integrates Identity Trust for security/compliance.** This is by design for Catena-X/Dataspace scenarios but creates complexity for local testing.

### Path Forward
For the Transfer Orchestrator project:

**Short-term (Development/Testing):**
- Use mock EDC responses
- Test orchestrator logic independently
- Focus on policy evaluation, state management, events

**Long-term (Production):**
- Implement proper TokenDecorator extension
- Or set up full STS/IAM infrastructure
- Align with Catena-X requirements

---

## Appendix: Useful Commands

### Build Template-Basic
```bash
cd /path/to/Template-Basic
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew clean :runtimes:controlplane:build
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :runtimes:controlplane:dockerize
```

### Check EDC Health
```bash
# Provider EDC
curl http://localhost:7171/check/health

# Consumer EDC
curl http://localhost:9193/check/health
```

### Query Assets (Management API)
```bash
curl -X POST http://localhost:7173/management/v3/assets/request \
  -H "X-Api-Key: provider-api-key" \
  -H "Content-Type: application/json" \
  -d '{"@context": {"@vocab": "https://w3id.org/edc/v0.0.1/ns/"}, "@type": "QuerySpec"}'
```

### Check Contract Negotiation Logs
```bash
docker logs consumer-edc 2>&1 | grep -i "negotiation\|scope"
```

---

**Document Version:** 1.0
**Last Updated:** 2025-12-13
**Project:** Transfer Orchestrator
**EDC Target Version:** v0.10.1 (Template-Basic)