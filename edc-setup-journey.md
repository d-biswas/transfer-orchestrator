# EDC Setup Journey - Transfer Orchestrator Project

This document tracks my attempts to set up Eclipse Dataspace Connector (EDC) as a beginner.

---

## Attempt 1: Tractus-X EDC Runtime-Memory

**What I Tried:**
- Used Tractus-X EDC runtime-memory for local testing
- Repo: `https://github.com/eclipse-tractusx/tractusx-edc`

**Discovery:**
- Required BDRS (Business Partner Data Registry Service) - not available locally
- Tractus-X is built for production Catena-X scenarios, not simple testing
- Too complex for learning and local development

**Conclusion:** ❌ Probably not suitable for local testing

---

## Attempt 2: Tractus-X Mock-Connector

**What I Tried:**
- Switched to mock-connector as a simpler alternative

**Discovery:**
- Repository is deprecated and no longer maintained
- No active support or updates available

**Conclusion:** ❌ Dead project, moved on

---

## Attempt 3: Eclipse EDC Samples (Shadow JAR)

**What I Tried:**
- Built EDC runtime as a fat JAR from Eclipse EDC Samples
- Repo: `https://github.com/eclipse-edc/Samples.git`
- Successfully created ~35MB JAR with all components

**Discovery:**
- Build worked perfectly
- Management APIs were responding
- Contract negotiation started but got stuck waiting for callbacks
- Callbacks from EDC never arrived

**Conclusion:** ❌ Build success but runtime failure - contract negotiation timeout

---

## Attempt 4: Eclipse EDC Template-Basic

**What I Tried:**
- Used Template-Basic as official EDC starter template
- Repo: `https://github.com/eclipse-edc/Template-Basic.git`

**Discovery:**
- Needed to exclude STS (Secure Token Service) to avoid IAM setup
- EDC started successfully after removing STS dependencies
- Contract negotiation failed with "scope string invalid" error
- Root cause: Modern EDC requires OAuth2 scopes, but no TokenDecorator without STS

**Conclusion:** ⚠️ Got furthest but blocked by identity/authentication requirements

---

## Attempt 5: MinimumViableDataspace (MVD)

**What I Tried:**
- Tried MVD thinking it would be simpler for local testing
- Repo: `https://github.com/eclipse-edc/MinimumViableDataspace.git`

**Discovery:**
- MVD actually requires MORE identity components:
  - Identity Hub
  - STS (Secure Token Service)
  - Verifiable Credentials
  - DID (Decentralized Identifiers)
- Not a simplified version, but full Catena-X identity model

**Conclusion:** ❌ More complex than expected, not suitable for beginners

---

## Key Learnings

### 1. Modern EDC Requires Identity/Authentication
- EDC version 0.10+ requires Identity Trust components
- Can't skip IAM (Identity & Access Management) in newer versions
- For simple testing, might need older EDC versions (<v0.8)

### 2. EDC Has Different Flavors
- **Template-Basic**: Official starter, needs IAM setup
- **MVD**: Full Catena-X setup with complete identity stack
- **Tractus-X**: Production automotive dataspace, a bit complex
- **Older EDC**: Simpler but outdated

### 4. Protocol Paths Vary by Version
- Always check EDC logs to find the correct API paths
- Don't assume paths from documentation match your version
- Template-Basic uses `/protocol`, not `/api/v1/dsp`

### 5. Real EDC Setup Is Complex for Local Testing
- Identity Trust, OAuth scopes, token services all required
- Better to start with mock EDC for learning orchestration
- Focus on business logic first, real EDC integration later

---

## Final Takeaway

**For building a Transfer Orchestrator:**
- Use mock EDC implementation for development and testing
- Learn orchestration patterns without EDC complexity