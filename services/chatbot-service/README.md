# Chatbot Service (Python FastAPI)

A minimal FastAPI service that integrates with ChatGPT to assist patients with clinic information and appointment booking via the existing API Gateway.

## Features (MVP)
- `POST /chat`: chat entrypoint; calls OpenAI and optionally invokes tools (e.g., appointment booking via gateway).
- Basic intent extraction and slot-filling scaffold.
- Environment-based configuration and Dockerized deployment.

## Env Vars
- `OPENAI_API_KEY`: API key for OpenAI or Azure OpenAI.
- `OPENAI_BASE_URL` (optional): custom base URL for Azure/OpenAI-compatible providers.
- `OPENAI_MODEL`: default model (e.g., `gpt-4o-mini` or compatible).
- `API_GATEWAY_URL`: base URL to call existing microservices via gateway (e.g., `http://api-gateway:8080`).

## Run locally
```bash
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

## Docker
```bash
docker build -t chatbot-service:local .
docker run -p 8000:8000 --env OPENAI_API_KEY=... --env API_GATEWAY_URL=http://api-gateway:8080 chatbot-service:local
```

## Notes
- Tool/function calling is implemented as server-side handlers that the model can request via structured JSON.
- Add authentication (JWT) when calling gateway endpoints in production.

## Through API Gateway
- Gateway route forwards `http://localhost:8080/chat` to this service (Docker network `chatbot-service:8000`).
- Example request via gateway:
```bash
curl -X POST http://localhost:8080/chat \
	-H "Content-Type: application/json" \
	-d '{"sessionId":"s1","message":"Tôi muốn đặt lịch khám răng ngày 2025-12-10 lúc 09:00"}'
```
