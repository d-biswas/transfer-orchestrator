# Consumer EDC Configuration

This directory is for consumer EDC configuration files (if needed in the future).

The consumer EDC is used by your Transfer Orchestrator to:
- Initiate contract negotiations with the provider
- Start transfer processes
- Receive callbacks about transfer state changes

## Consumer EDC Endpoints

- Management API: http://localhost:9191/management
- DSP Protocol: http://localhost:9192/api/v1/dsp
- Public API: http://localhost:9193/public

## API Key

The consumer EDC uses API key authentication:
```
X-Api-Key: consumer-api-key
```

## Testing Consumer EDC

Check health:
```bash
curl http://localhost:9191/api/check/health
```

List active negotiations (via your orchestrator or directly):
```bash
curl -X POST http://localhost:9191/management/v2/contractnegotiations/request \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: consumer-api-key" \
  -d '{
    "@context": {
      "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
    },
    "offset": 0,
    "limit": 50
  }'
```