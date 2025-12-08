import os
import json
from typing import Optional, Dict, Any

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import httpx

# Basic config via environment variables
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
OPENAI_BASE_URL = "https://api.openai.com/v1"
OPENAI_MODEL = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
API_GATEWAY_URL = os.getenv("API_GATEWAY_URL", "http://api-gateway:8080")

if not OPENAI_API_KEY:
    print("[WARN] OPENAI_API_KEY is not set; /chat will fail until provided.")

app = FastAPI(title="Chatbot Service", version="0.1.0")


class ChatRequest(BaseModel):
    sessionId: Optional[str] = None
    message: str
    metadata: Optional[Dict[str, Any]] = None


class ChatResponse(BaseModel):
    sessionId: Optional[str] = None
    reply: str
    toolCall: Optional[Dict[str, Any]] = None


async def call_openai(messages: list[Dict[str, Any]]) -> Dict[str, Any]:
    headers = {
        "Authorization": f"Bearer {OPENAI_API_KEY}",
        "Content-Type": "application/json",
    }
    base_url = OPENAI_BASE_URL or "https://api.openai.com/v1"
    url = f"{base_url}/chat/completions"

    payload = {
        "model": OPENAI_MODEL,
        "messages": messages,
        # Simple function-calling schema (tool) for appointment creation
        "tools": [
            {
                "type": "function",
                "function": {
                    "name": "appointment_create",
                    "description": "Create an appointment via API Gateway",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "service": {"type": "string"},
                            "date": {"type": "string", "description": "YYYY-MM-DD"},
                            "time": {"type": "string", "description": "HH:MM"},
                            "patientName": {"type": "string"},
                            "contactPhone": {"type": "string"},
                            "notes": {"type": "string"},
                        },
                        "required": ["service", "date", "time", "patientName", "contactPhone"],
                    },
                },
            }
        ],
        "tool_choice": "auto",
    }

    async with httpx.AsyncClient(timeout=30) as client:
        r = await client.post(url, headers=headers, json=payload)
        r.raise_for_status()
        return r.json()


async def call_gateway_create_appointment(args: Dict[str, Any]) -> Dict[str, Any]:
    # Adjust path to match your gateway route for appointment creation
    # Example: POST /appointments
    url = f"{API_GATEWAY_URL}/appointments"
    async with httpx.AsyncClient(timeout=30) as client:
        r = await client.post(url, json=args)
        r.raise_for_status()
        return r.json()


@app.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    if not OPENAI_API_KEY:
        raise HTTPException(status_code=500, detail="OPENAI_API_KEY not configured")

    system_prompt = (
        "Bạn là trợ lý phòng khám, hỗ trợ bệnh nhân bằng tiếng Việt. "
        "Không chẩn đoán y khoa; hướng dẫn liên hệ bác sĩ khi cần. "
        "Chỉ thu thập thông tin tối thiểu để đặt lịch và xin sự đồng ý trước khi thu thập. "
        "Nếu đủ thông tin, hãy gọi function 'appointment_create'."
    )

    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": req.message},
    ]

    try:
        completion = await call_openai(messages)
    except httpx.HTTPStatusError as e:
        raise HTTPException(status_code=e.response.status_code, detail=e.response.text)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

    choice = completion.get("choices", [{}])[0]
    message = choice.get("message", {})
    reply_text = message.get("content") or ""

    tool_calls = message.get("tool_calls") or []
    tool_result: Optional[Dict[str, Any]] = None

    # Handle at most one tool call in MVP
    if tool_calls:
        tool = tool_calls[0]
        if tool.get("type") == "function":
            fn = tool.get("function", {})
            if fn.get("name") == "appointment_create":
                try:
                    args = json.loads(fn.get("arguments", "{}"))
                except Exception:
                    args = {}
                try:
                    result = await call_gateway_create_appointment(args)
                    tool_result = {"name": "appointment_create", "result": result}
                except httpx.HTTPStatusError as e:
                    tool_result = {"name": "appointment_create", "error": e.response.text}
                except Exception as e:
                    tool_result = {"name": "appointment_create", "error": str(e)}

    return ChatResponse(sessionId=req.sessionId, reply=reply_text, toolCall=tool_result)


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.get("/intents")
async def intents():
    return {
        "intents": [
            {"name": "clinic.info", "desc": "Thông tin phòng khám, giờ, địa chỉ, dịch vụ"},
            {"name": "appointment.book", "desc": "Đặt lịch khám"},
            {"name": "pricing.info", "desc": "Hỏi phí và dịch vụ"},
            {"name": "record.lookup", "desc": "Tra cứu hồ sơ (yêu cầu xác thực)"},
        ]
    }


class AppointmentCreateRequest(BaseModel):
    service: str
    date: str
    time: str
    patientName: str
    contactPhone: str
    notes: Optional[str] = None


@app.post("/tools/appointment.create")
async def tool_appointment_create(req: AppointmentCreateRequest):
    try:
        result = await call_gateway_create_appointment(req.model_dump())
        return {"ok": True, "result": result}
    except httpx.HTTPStatusError as e:
        raise HTTPException(status_code=e.response.status_code, detail=e.response.text)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
