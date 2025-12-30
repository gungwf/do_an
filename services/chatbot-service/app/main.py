import os
import json
from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
from typing import Optional, Dict, Any
import httpx

from chat_utils import (
    handle_booking_intent, 
    handle_lookup_intent, 
    get_doctor_list,
    call_gateway_create_appointment
)

# Config OpenAI
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
OPENAI_MODEL = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
OPENAI_BASE_URL = os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1")

app = FastAPI(title="Chatbot Service", version="0.2.0")

class ChatRequest(BaseModel):
    sessionId: Optional[str] = None
    message: str
    metadata: Optional[Dict[str, Any]] = None

class ChatResponse(BaseModel):
    sessionId: Optional[str] = None
    reply: str
    toolCall: Optional[Dict[str, Any]] = None

# --- HÀM GỌI OPENAI ---
async def call_openai(messages: list) -> Dict[str, Any]:
    headers = {
        "Authorization": f"Bearer {OPENAI_API_KEY}",
        "Content-Type": "application/json",
    }
    payload = {
        "model": OPENAI_MODEL,
        "messages": messages,
        "tools": [{
            "type": "function",
            "function": {
                "name": "appointment_create",
                "description": "Create appointment",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "doctorId": {"type": "string"},
                        "appointmentTime": {"type": "string", "description": "ISO 8601 format"}
                    },
                    "required": ["doctorId", "appointmentTime"]
                }
            }
        }]
    }
    async with httpx.AsyncClient(timeout=30) as client:
        r = await client.post(f"{OPENAI_BASE_URL}/chat/completions", headers=headers, json=payload)
        r.raise_for_status()
        return r.json()

# --- API ENDPOINT CHÍNH ---
@app.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest, request: Request):
    if not OPENAI_API_KEY:
        raise HTTPException(status_code=500, detail="Missing OPENAI_API_KEY")

    user_msg = req.message.strip().lower()
    
    auth_header = request.headers.get("authorization")
    print(f"[MAIN DEBUG] Auth Header gốc: {auth_header}")
    token = auth_header[7:] if auth_header and auth_header.lower().startswith("bearer ") else ""
    print(f"[MAIN DEBUG] Token đã cắt: '{token[:10]}...'")
    # ---------------------------------------------------------
    # 1. XỬ LÝ INTENT: DANH SÁCH BÁC SĨ
    # ---------------------------------------------------------
    if "danh sách bác sĩ" in user_msg or "list bác sĩ" in user_msg:
        docs = await get_doctor_list(token)
        if docs:
            reply = "<b>Đội ngũ bác sĩ:</b><br>" + "<br>".join(f"- {d.get('fullName')}" for d in docs)
        else:
            reply = "Hiện chưa có danh sách bác sĩ."
        return ChatResponse(sessionId=req.sessionId, reply=reply)

    # ---------------------------------------------------------
    # 2. XỬ LÝ INTENT: ĐẶT LỊCH
    # ---------------------------------------------------------
    book_reply, book_tool = await handle_booking_intent(user_msg, token, req.sessionId)
    if book_reply:
        return ChatResponse(sessionId=req.sessionId, reply=book_reply, toolCall=book_tool)
    # ---------------------------------------------------------
    # 3. XỬ LÝ INTENT: TRA CỨU (Lịch sử, Thuốc...)
    # ---------------------------------------------------------
    lookup_reply = await handle_lookup_intent(user_msg, token)
    if lookup_reply:
        return ChatResponse(sessionId=req.sessionId, reply=lookup_reply)

    # ---------------------------------------------------------
    # 4. FALLBACK: GỌI OPENAI (Nếu không bắt được logic trên)
    # ---------------------------------------------------------
    system_prompt = (
        "Bạn là trợ lý phòng khám. Trả lời ngắn gọn tiếng Việt. "
        "Nếu user muốn đặt lịch, hãy hỏi: Tên bác sĩ, Ngày, Giờ, Triệu chứng. "
        "Đừng tự bịa thông tin y tế."
    )
    messages = [{"role": "system", "content": system_prompt}, {"role": "user", "content": req.message}]

    try:
        completion = await call_openai(messages)
        choice = completion["choices"][0]["message"]
        reply_text = choice.get("content") or ""
        
        # Xử lý nếu OpenAI quyết định gọi Tool (Trường hợp dự phòng)
        tool_calls = choice.get("tool_calls")
        tool_res = None
        if tool_calls:
            args = json.loads(tool_calls[0]["function"]["arguments"])
            try:
                # Gọi lại helper tạo lịch
                res = await call_gateway_create_appointment(args, token)
                reply_text = "AI đã hỗ trợ đặt lịch thành công!"
                tool_res = {"name": "appointment_create", "result": res}
            except Exception as e:
                reply_text = f"AI gặp lỗi khi đặt lịch: {e}"

        return ChatResponse(sessionId=req.sessionId, reply=reply_text, toolCall=tool_res)

    except Exception as e:
        return ChatResponse(sessionId=req.sessionId, reply=f"Lỗi hệ thống AI: {str(e)}")