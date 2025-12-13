import os
import json
from typing import Optional, Dict, Any

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import httpx

# Basic config via environment variables
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
OPENAI_BASE_URL = os.getenv("OPENAI_BASE_URL")
OPENAI_MODEL = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
API_GATEWAY_URL = os.getenv("API_GATEWAY_URL", "http://localhost:8080")

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

# Helper: Search doctor by name, return list of doctor objects
async def search_doctor_by_name(token: str, name: str) -> list:
    url = f"{API_GATEWAY_URL}/users/doctors/search"
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    payload = {"fullName": name, "page": 0, "size": 5}
    async with httpx.AsyncClient(timeout=30) as client:
        r = await client.post(url, headers=headers, json=payload)
        r.raise_for_status()
        data = r.json()
        # data should be a Page object with 'content' as list
        return data.get("content", []) if isinstance(data, dict) else []

# Helper: Get doctor profile by id
async def get_doctor_profile_by_id(token: str, doctor_id: str) -> dict:
    url = f"{API_GATEWAY_URL}/doctor-profiles/{doctor_id}"
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    async with httpx.AsyncClient(timeout=30) as client:
        r = await client.get(url, headers=headers)
        r.raise_for_status()
        return r.json()
# Helper: Call API Gateway to get doctor profile (me) with Authorization
async def get_doctor_profile_with_token(token: str) -> dict:
    url = f"{API_GATEWAY_URL}/doctor-profiles/me"
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    async with httpx.AsyncClient(timeout=30) as client:
        r = await client.get(url, headers=headers)
        r.raise_for_status()
        return r.json()
# ...existing code...

# Helper: Call API Gateway to get doctor list with Authorization
async def get_doctor_list_with_token(token: str) -> list:
    url = f"{API_GATEWAY_URL}/users/doctors/simple"
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    async with httpx.AsyncClient(timeout=30) as client:
        r = await client.get(url, headers=headers)
        r.raise_for_status()
        return r.json()


from fastapi import Request

@app.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest, request: Request):
    if not OPENAI_API_KEY:
        raise HTTPException(status_code=500, detail="OPENAI_API_KEY not configured")

    # Simple intent detection for doctor list & doctor detail
    user_msg = req.message.strip().lower()
    auth_header = request.headers.get("authorization")
    token = ""
    if auth_header and auth_header.lower().startswith("bearer "):
        token = auth_header[7:]

    # Intent: danh sách bác sĩ
    if "danh sách bác sĩ" in user_msg or "xem danh sách bác sĩ" in user_msg or "list bác sĩ" in user_msg:
        try:
            doctors = await get_doctor_list_with_token(token)
            if isinstance(doctors, list) and doctors:
                reply = "<b>Danh sách bác sĩ:</b><br>" + "<br>".join(
                    f"&bull; {d.get('fullName', d.get('name', 'Không rõ'))}" for d in doctors
                )
            else:
                reply = "Không tìm thấy bác sĩ nào."
        except Exception as e:
            reply = f"Không lấy được danh sách bác sĩ: {e}"
        return ChatResponse(sessionId=req.sessionId, reply=reply, toolCall=None)
        # Intent: hỏi quy trình khám bệnh
    if (
        "quy trình khám" in user_msg
        or "quy trình khám bệnh" in user_msg
        or "thủ tục khám" in user_msg
        or "bước khám bệnh" in user_msg
        or "các bước khám" in user_msg
        or "khám như thế nào" in user_msg
    ):
        reply = (
            "<b>Quy trình khám bệnh tại phòng khám:</b><br>"
            "1. <b>Đặt lịch trước:</b> Đặt lịch trên trang web của chúng tôi để trải nghiệm dịch vụ tốt nhất.<br>"
            "2. <b>Thanh toán & xác nhận:</b> Thanh toán tiền khám và nhận email xác nhận.<br>"
            "3. <b>Khám lâm sàng:</b> Gặp bác sĩ, trình bày triệu chứng, kiểm tra ban đầu.<br>"
            "4. <b>Làm dịch vụ theo chỉ định:</b> Nhận kết quả của bác sĩ, tiếp tục các dịch vụ theo yêu cầu bác sĩ (nếu cần).<br>"
            "5. <b>Nhận kết quả & tư vấn:</b> Quay lại gặp bác sĩ để được tư vấn, kê đơn.<br>"
            "6. <b>Thanh toán & nhận thuốc:</b> Thanh toán tại quầy, nhận thuốc và hướng dẫn sử dụng.<br>"
            "<i>Nếu cần hỗ trợ thêm, hãy hỏi trợ lý hoặc liên hệ lễ tân.</i>"
        )
        return ChatResponse(sessionId=req.sessionId, reply=reply, toolCall=None)
    
    # Intent: tra cứu thông tin chi nhánh (dùng API)
    if (
        "thông tin chi nhánh" in user_msg
        or "địa chỉ chi nhánh" in user_msg
        or "giờ làm việc chi nhánh" in user_msg
        or "số điện thoại chi nhánh" in user_msg
        or "liên hệ chi nhánh" in user_msg
        or "chi nhánh ở đâu" in user_msg
        or "các chi nhánh" in user_msg
    ):
        try:
            # Lấy danh sách chi nhánh từ API Gateway
            url = f"{API_GATEWAY_URL}/branches"
            async with httpx.AsyncClient(timeout=30) as client:
                r = await client.get(url)
                r.raise_for_status()
                branches = r.json()
            if not isinstance(branches, list) or not branches:
                reply = "Không tìm thấy chi nhánh nào."
                return ChatResponse(sessionId=req.sessionId, reply=reply, toolCall=None)
            # Liệt kê tên các chi nhánh
            branch_names = [b.get('branchName', 'Không rõ') for b in branches]
            reply = (
                "<b>Danh sách chi nhánh:</b><br>"
                + "<br>".join(f"&bull; {name}" for name in branch_names)
                + "<br><i>Bạn muốn biết thông tin chi nhánh nào? Hãy hỏi tên chi nhánh cụ thể!</i>"
            )
            # Nếu user hỏi tên chi nhánh cụ thể, trả về chi tiết
            import re
            import unicodedata
            def normalize(s):
                if not s: return ''
                s = unicodedata.normalize('NFKD', s)
                s = ''.join(c for c in s if not unicodedata.combining(c))
                s = s.lower().replace('-', ' ').replace('_', ' ')
                return ''.join(c for c in s if c.isalnum() or c.isspace()).strip()

            norm_user_msg = normalize(user_msg)
            for b in branches:
                name = b.get('branchName', '')
                norm_name = normalize(name)
                if norm_name and norm_name in norm_user_msg:
                    reply = (
                        f"<b>Thông tin chi nhánh {b.get('branchName','')}</b><br>"
                        f"Địa chỉ: {b.get('address','Không rõ')}<br>"
                        f"Giờ làm việc: {b.get('workingHours','Không rõ')}<br>"
                        f"SĐT: {b.get('phoneNumber','Không rõ')}"
                    )
                    break
            return ChatResponse(sessionId=req.sessionId, reply=reply, toolCall=None)
        except Exception as e:
            reply = f"Không lấy được thông tin chi nhánh: {e}"
            return ChatResponse(sessionId=req.sessionId, reply=reply, toolCall=None)
        
    # Intent: xem thông tin bác sĩ theo tên
    import re
    # Cải thiện nhận diện tên bác sĩ trong nhiều mẫu câu
    patterns = [
        r"thông tin về bác sĩ ([\w .-]+)",
        r"xem thông tin bác sĩ ([\w .-]+)",
        r"thông tin bác sĩ ([\w .-]+)",
        r"bác sĩ ([\w .-]+)"
    ]
    doctor_name = None
    for pat in patterns:
        match = re.search(pat, user_msg)
        if match:
            doctor_name = match.group(1).strip()
            break
    if doctor_name:
        try:
            doctors = await search_doctor_by_name(token, doctor_name)
            if not doctors:
                reply = f"Không tìm thấy bác sĩ tên '{doctor_name}'."
            else:
                doc = doctors[0]
                doc_id = doc.get("id")
                profile = await get_doctor_profile_by_id(token, doc_id)
                # Ưu tiên branchName nếu có, fallback branchId
                branch = doc.get('branchName') or profile.get('branchName') or doc.get('branchId') or profile.get('branchId') or 'Không rõ'
                reply = (
                    f"<span class='emoji'>\U0001F468‍⚕️</span> <b>{doc.get('fullName','Không rõ')}</b><br>"
                    f"<b>Chuyên khoa:</b> {doc.get('specialty', profile.get('specialty','Không rõ'))}<br>"
                    f"<b>Học vị:</b> {doc.get('degree', profile.get('degree','Không rõ'))}<br>"
                    f"<b>Chi nhánh:</b> {branch}<br>"
                    f"<b>Email:</b> {doc.get('email','Không rõ')}<br>"
                    f"<b>SĐT:</b> {doc.get('phoneNumber','Không rõ')}"
                )
        except Exception as e:
            reply = f"Không lấy được thông tin bác sĩ: {e}"
        return ChatResponse(sessionId=req.sessionId, reply=reply, toolCall=None)

    # ...existing code...
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
