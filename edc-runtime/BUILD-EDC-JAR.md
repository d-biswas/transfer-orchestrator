# Building EDC JAR from Source

This guide shows how to build a working Eclipse EDC connector fat JAR with all dependencies.

## Prerequisites

- Java 17+
- Git

## Why EDC Samples?

The EDC main repository's test runtimes don't create fat JARs by default. The **EDC Samples** repository provides working examples with proper shadow JAR configuration that bundles all dependencies.

---

## Method: Using EDC Samples

This creates a **~35MB fat JAR** with all dependencies bundled.

### Step 1: Clone EDC Samples Repository

```bash
cd ~/Projects/Personal
git clone https://github.com/eclipse-edc/Samples.git edc-samples
```

### Step 2: Build the Connector with Shadow JAR

```bash
cd ~/Projects/Personal/edc-samples
./gradlew :transfer:transfer-00-prerequisites:connector:shadowJar -x test
```

**Build time:** 1-2 minutes
**Expected output:** `BUILD SUCCESSFUL`

**Note:** Deprecation warnings are normal and can be ignored.

### Step 3: Locate the Built JAR

The fat JAR is created at:
```
~/Projects/Personal/edc-samples/transfer/transfer-00-prerequisites/connector/build/libs/connector.jar
```

Verify it exists (should be ~35MB):
```bash
ls -lh ~/Projects/Personal/edc-samples/transfer/transfer-00-prerequisites/connector/build/libs/connector.jar
```

Expected output:
```
-rw-rw-r-- 1 user user 35M Dec 13 16:21 connector.jar
```

### Step 4: Copy JAR to Your Project

```bash
cp ~/Projects/Personal/edc-samples/transfer/transfer-00-prerequisites/connector/build/libs/connector.jar \
   ~/Projects/Personal/transfer-orchestrator/edc-runtime/edc-connector.jar
```

**Note:** The JAR is renamed from `connector.jar` to `edc-connector.jar` during the copy.

### Step 5: Verify the Copy

```bash
ls -lh ~/Projects/Personal/transfer-orchestrator/edc-runtime/edc-connector.jar
```

Expected output:
```
-rw-rw-r-- 1 user user 35M Dec 13 16:22 edc-connector.jar
```

### Step 6: Test the JAR (Optional)

```bash
cd ~/Projects/Personal/transfer-orchestrator/edc-runtime
java -jar edc-connector.jar --help
```

You should see EDC starting up with some configuration warnings (expected).

### Step 7: Rebuild Docker Image

Using Gradle task:
```bash
cd ~/Projects/Personal/transfer-orchestrator
./gradlew dockerBuildEdcConnectorImage
```

Or using Docker directly:
```bash
docker build -t edc-connector:0.0.1-SNAPSHOT ./edc-runtime
```

## Done!

Your EDC connector JAR is now ready to use in your docker-compose setup.

### Start Your Environment

```bash
cd ~/Projects/Personal/transfer-orchestrator

# Build all Docker images
./gradlew dockerBuildAll

# Start all services
docker-compose up -d

# Check status
docker-compose ps

# Test EDC health
curl http://localhost:9191/api/check/health  # Consumer EDC
curl http://localhost:8181/api/check/health  # Provider EDC
```

---

## Troubleshooting

### JAR is too small (< 10MB)

This means you built a regular JAR, not a fat JAR. Make sure you:
- Use the EDC Samples repository
- Run the `shadowJar` task (not just `build`)

### "no main manifest attribute" error

The JAR doesn't have proper MANIFEST.MF. Use the EDC Samples method which creates proper executable JARs.

### Configuration errors when running JAR

Normal! Docker-compose will provide all required environment variables. The JAR is working correctly if you see EDC starting to boot.

---

## What's in the JAR?

The EDC Samples connector includes:
- ✅ Control Plane (contract negotiation, transfers)
- ✅ Management API endpoints
- ✅ DSP Protocol support
- ✅ Data Plane components
- ✅ HTTP server (Jetty)
- ✅ In-memory stores
- ✅ All dependencies bundled

Perfect for development and testing!