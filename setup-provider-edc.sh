#!/bin/bash

# Script to load sample assets, policies, and contract definitions into Provider EDC
# Run this after starting docker-compose

set -e

PROVIDER_MGMT_URL="http://localhost:7173/management"
API_KEY="provider-api-key"
CONFIG_DIR="edc-config/provider"

echo "================================================"
echo "Setting up Provider EDC with Sample Data"
echo "================================================"
echo ""

# Check if provider EDC is reachable (check if management port responds)
echo "1. Checking Provider EDC availability..."
if nc -z localhost 7171 2>/dev/null; then
    echo "✓ Provider EDC management port is reachable"
else
    echo "✗ Provider EDC is not reachable. Make sure docker-compose is running:"
    echo "  docker-compose up -d"
    exit 1
fi
echo ""

# Create Asset
echo "2. Creating sample asset (asset-001)..."
 ASSET_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${PROVIDER_MGMT_URL}/v3/assets \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: ${API_KEY}" \
  -d @${CONFIG_DIR}/sample-asset.json)

HTTP_CODE=$(echo "$ASSET_RESPONSE" | tail -n1)
BODY=$(echo "$ASSET_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 201 ]; then
    echo "✓ Asset created successfully"
    echo "  Response: $BODY"
elif [ "$HTTP_CODE" -eq 409 ]; then
    echo "⚠ Asset already exists (409 Conflict)"
else
    echo "✗ Failed to create asset (HTTP $HTTP_CODE)"
    echo "  Response: $BODY"
    exit 1
fi
echo ""

# Create Policy
echo "3. Creating sample policy (policy-001)..."
POLICY_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${PROVIDER_MGMT_URL}/v3/policydefinitions \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: ${API_KEY}" \
  -d @${CONFIG_DIR}/sample-policy.json)

HTTP_CODE=$(echo "$POLICY_RESPONSE" | tail -n1)
BODY=$(echo "$POLICY_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 201 ]; then
    echo "✓ Policy created successfully"
    echo "  Response: $BODY"
elif [ "$HTTP_CODE" -eq 409 ]; then
    echo "⚠ Policy already exists (409 Conflict)"
else
    echo "✗ Failed to create policy (HTTP $HTTP_CODE)"
    echo "  Response: $BODY"
    exit 1
fi
echo ""

# Create Contract Definition
echo "4. Creating sample contract definition (contract-def-001)..."
CONTRACT_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${PROVIDER_MGMT_URL}/v3/contractdefinitions \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: ${API_KEY}" \
  -d @${CONFIG_DIR}/sample-contract-definition.json)

HTTP_CODE=$(echo "$CONTRACT_RESPONSE" | tail -n1)
BODY=$(echo "$CONTRACT_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 201 ]; then
    echo "✓ Contract definition created successfully"
    echo "  Response: $BODY"
elif [ "$HTTP_CODE" -eq 409 ]; then
    echo "⚠ Contract definition already exists (409 Conflict)"
else
    echo "✗ Failed to create contract definition (HTTP $HTTP_CODE)"
    echo "  Response: $BODY"
    exit 1
fi
echo ""

# Verify by listing assets
echo "5. Verifying: Listing all assets..."
ASSETS=$(curl -s -X POST ${PROVIDER_MGMT_URL}/v3/assets/request \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: ${API_KEY}" \
  -d '{
    "@context": {"@vocab": "https://w3id.org/edc/v0.0.1/ns/"},
    "offset": 0,
    "limit": 50
  }')

echo "$ASSETS" | jq '.' 2>/dev/null || echo "$ASSETS"
echo ""

echo "================================================"
echo "✓ Provider EDC setup complete!"
echo "================================================"
echo ""
echo "Next steps:"
echo "1. Test catalog query from consumer:"
echo "   curl -X POST http://localhost:9191/management/v3/catalog/request \\"
echo "     -H 'Content-Type: application/json' \\"
echo "     -H 'X-Api-Key: consumer-api-key' \\"
echo "     -d '{...}'"
echo ""
echo "2. Or use your Transfer Orchestrator API:"
echo "   curl -X POST http://localhost:8080/api/v1/transfers -d '{...}'"
echo ""
echo "See EDC-SETUP.md for detailed testing instructions."
echo ""