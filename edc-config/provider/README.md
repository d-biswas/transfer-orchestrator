# Provider EDC Configuration

This directory contains sample assets, policies, and contract definitions for the Provider EDC.

## Loading Sample Data

After starting the Docker Compose stack, load these resources into the provider EDC using the Management API:

### 1. Create Asset

```bash
curl -X POST http://localhost:8181/management/v3/assets \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: provider-api-key" \
  -d @edc-config/provider/sample-asset.json
```

### 2. Create Policy Definition

```bash
curl -X POST http://localhost:8181/management/v2/policydefinitions \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: provider-api-key" \
  -d @edc-config/provider/sample-policy.json
```

### 3. Create Contract Definition

```bash
curl -X POST http://localhost:8181/management/v2/contractdefinitions \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: provider-api-key" \
  -d @edc-config/provider/sample-contract-definition.json
```

## Verify Resources

List all assets:
```bash
curl -X POST http://localhost:8181/management/v3/assets/request \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: provider-api-key" \
  -d '{
    "@context": {
      "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
    },
    "offset": 0,
    "limit": 50
  }'
```

List all contract definitions:
```bash
curl -X POST http://localhost:8181/management/v2/contractdefinitions/request \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: provider-api-key" \
  -d '{
    "@context": {
      "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
    },
    "offset": 0,
    "limit": 50
  }'
```