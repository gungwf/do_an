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
        if r.status_code == 400:
            # Có thể là trùng slot
            raise Exception(r.text)
        r.raise_for_status()
        appointment = r.json()
        # Sau khi tạo thành công, gọi tiếp API tạo link thanh toán
        appointment_id = appointment.get('id')
        if not appointment_id:
            return appointment
        # Gọi API tạo link thanh toán
        try:
            pay_url = f"{API_GATEWAY_URL}/api/v1/payment/create-payment/{appointment_id}"
            pay_resp = await client.post(pay_url)
            pay_resp.raise_for_status()
            payment_link = pay_resp.text.strip('"')
            appointment['paymentLink'] = payment_link
        except Exception:
            appointment['paymentLink'] = None
        return appointment

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
                    # Intent: đặt lịch khám nếu phát hiện đủ thông tin (bác sĩ, ngày, giờ)
                import re
                book_pattern = re.search(r"đặt lịch.*bác sĩ ([\w .-]+?)(?:\s+(vào|ngày|lúc|thời gian|date|at)\b|$)(.*)", user_msg)
                if book_pattern:
                    doctor_name = book_pattern.group(1).strip()
                    time_info = book_pattern.group(3) or ''
                    # Tìm ngày và giờ trong time_info
                    date_match = re.search(r"(\d{1,2}/\d{1,2}/\d{4}|\d{4}-\d{2}-\d{2})", time_info)
                    time_match = re.search(r"(\d{1,2}:\d{2})", time_info)
                    date_str = date_match.group(1) if date_match else None
                    time_str = time_match.group(1) if time_match else None
                    if doctor_name and date_str and time_str:
                        try:
                            # Tìm bác sĩ theo tên
                            doctors = await search_doctor_by_name(token, doctor_name)
                            if not doctors:
                                reply = f"Không tìm thấy bác sĩ tên '{doctor_name}'."
                                return ChatResponse(sessionId=req.sessionId, reply=reply, toolCall=None)
                            doc = doctors[0]
                            doctor_id = doc.get('id')
                            # Ghép ngày giờ thành appointmentTime ISO
                            from datetime import datetime
                            try:
                                if '-' in date_str:
                                    dt = datetime.strptime(date_str + ' ' + time_str, '%Y-%m-%d %H:%M')
                                else:
                                    dt = datetime.strptime(date_str + ' ' + time_str, '%d/%m/%Y %H:%M')
                                appointment_time = dt.isoformat()
                            except Exception:
                                reply = "Không nhận diện được ngày giờ. Vui lòng nhập đúng định dạng."
                                return ChatResponse(sessionId=req.sessionId, reply=reply, toolCall=None)
                            # Chỉ cần doctorId, branchId, appointmentTime là đủ
                            args = {
                                "doctorId": doctor_id,
                                "appointmentTime": appointment_time
                            }
                            if doc.get('branchId'):
                                args['branchId'] = doc.get('branchId')
                            # Đặt lịch luôn, không hỏi lại tên/sdt/dịch vụ
                            result = await call_gateway_create_appointment(args)
                            if result.get('paymentLink'):
                                reply = (
                                    "<b>Đặt lịch thành công!</b><br>"
                                    f"Vui lòng thanh toán tại: <a href='{result['paymentLink']}' target='_blank'>Link thanh toán</a>"
                                )
                            else:
                                reply = "Đặt lịch thành công! (Không lấy được link thanh toán)"
                            return ChatResponse(sessionId=req.sessionId, reply=reply, toolCall={"name": "appointment_create", "result": result})
                        except Exception as e:
                            err = str(e)
                            if 'đã có người đặt' in err or 'trùng' in err:
                                reply = "<b>Lịch hẹn này đã có người đặt. Vui lòng chọn thời gian khác.</b>"
                            else:
                                reply = f"Không đặt được lịch: {err}"
                            return ChatResponse(sessionId=req.sessionId, reply=reply, toolCall={"name": "appointment_create", "error": err})
                    # Nếu thiếu ngày/giờ thì fallback về intent hỏi bác sĩ
                # ...intent hỏi thông tin bác sĩ như cũ...
                patterns = [
                    r"thông tin về bác sĩ ([\w .-]+)",
                    r"xem thông tin bác sĩ ([\w .-]+)",
                    r"thông tin bác sĩ ([\w .-]+)",
                    r"bác sĩ ([\w .-]+?)(?:\s+(vào|ngày|lúc|thời gian|date|at)\b|$)"
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

    # Intent: tra cứu lịch sử khám (ưu tiên, luôn return nếu match)
    import re
    match_limit = re.search(r"(\d+)\s*(lần|record|history|gần nhất)", user_msg)
    limit = int(match_limit.group(1)) if match_limit else None
    if (
        re.search(r"lịch sử khám|xem lịch sử khám|lịch sử bệnh|lịch sử khám bệnh|các lần khám|đã từng khám|hồ sơ khám|lần khám gần nhất|record|history", user_msg)
    ):
        if not token:
            reply = "<b>Vui lòng đăng nhập để tra cứu lịch sử khám bệnh của bạn.</b>"
            return ChatResponse(sessionId=req.sessionId, reply=reply, toolCall=None)
        try:
            url = f"{API_GATEWAY_URL}/medical-records/patient/me?page=0&size=50"
            headers = {"Authorization": f"Bearer {token}"}
            async with httpx.AsyncClient(timeout=30) as client:
                r = await client.get(url, headers=headers)
                r.raise_for_status()
                data = r.json()
            records = data.get('content', []) if isinstance(data, dict) else []
            if not records:
                reply = "Không tìm thấy lịch sử khám nào cho tài khoản này."
            else:
                show_records = records[:limit] if limit else records
                reply = "<b>Lịch sử khám bệnh của bạn:</b><br>"
                for rec in show_records:
                    appt = rec.get('appointment', {})
                    date = appt.get('appointmentTime', rec.get('createdAt', 'Không rõ'))
                    doctor = appt.get('doctor', {}).get('fullName', 'Bác sĩ ?')
                    branch = appt.get('branch', {}).get('branchName', '')
                    diagnosis = rec.get('diagnosis', '')
                    reply += f"&bull; <b>{date[:10]}</b> - {doctor}"
                    if branch:
                        reply += f" ({branch})"
                    reply += "<br>"
                    if diagnosis:
                        reply += f"<i>Chẩn đoán: {diagnosis}</i><br>"
            return ChatResponse(sessionId=req.sessionId, reply=reply, toolCall=None)
        except Exception as e:
            reply = f"Không lấy được lịch sử khám: {e}"
            return ChatResponse(sessionId=req.sessionId, reply=reply, toolCall=None)

    # Intent: xem chi tiết bệnh án, đơn thuốc, chi tiết lần khám
    match_detail = re.search(r"(bệnh án|đơn thuốc|chi tiết lần khám|chi tiết khám|chi tiết bệnh án|chi tiết đơn thuốc)(?:.*?)([0-9a-f\-]{16,})?", user_msg)
    match_time = re.search(r"(bệnh án|đơn thuốc|chi tiết lần khám|chi tiết khám|chi tiết bệnh án|chi tiết đơn thuốc).*?(\d{4}-\d{2}-\d{2})", user_msg)
    record_id = None
    # Ưu tiên lấy id nếu user hỏi trực tiếp
    if match_detail and match_detail.group(2):
        record_id = match_detail.group(2)
    # Nếu hỏi theo ngày, tìm id bệnh án gần nhất với ngày đó
    elif match_time and token:
        date_str = match_time.group(2)
        try:
            url = f"{API_GATEWAY_URL}/medical-records/patient/me?page=0&size=50"
            headers = {"Authorization": f"Bearer {token}"}
            async with httpx.AsyncClient(timeout=30) as client:
                r = await client.get(url, headers=headers)
                r.raise_for_status()
                data = r.json()
            records = data.get('content', []) if isinstance(data, dict) else []
            # Tìm record gần nhất với ngày hỏi
            from datetime import datetime
            def date_diff(rec):
                appt = rec.get('appointment', {})
                d = appt.get('appointmentTime', rec.get('createdAt'))
                try:
                    return abs((datetime.fromisoformat(d[:19]) - datetime.fromisoformat(date_str)).total_seconds())
                except:
                    return float('inf')
            if records:
                records = sorted(records, key=date_diff)
                record_id = records[0].get('medicalRecordId')
        except Exception:
            pass
    if (match_detail or match_time) and record_id and token:
        try:
            url = f"{API_GATEWAY_URL}/medical-records/{record_id}/details"
            headers = {"Authorization": f"Bearer {token}"}
            async with httpx.AsyncClient(timeout=30) as client:
                r = await client.get(url, headers=headers)
                r.raise_for_status()
                detail = r.json()
            reply = f"<b>Chi tiết bệnh án:</b><br>"
            reply += f"Chẩn đoán: <b>{detail.get('diagnosis','Không rõ')}</b><br>"
            if detail.get('performedServices'):
                reply += "<b>Dịch vụ đã thực hiện:</b><br>"
                for s in detail['performedServices']:
                    reply += f"- {s.get('serviceName','?')} ({s.get('price','?')}đ)<br>"
            if detail.get('prescriptionItems'):
                reply += "<b>Đơn thuốc:</b><br>"
                for p in detail['prescriptionItems']:
                    reply += f"- {p.get('productName','?') or 'Thuốc'}: {p.get('quantity','?')} viên, liều: {p.get('dosage','?')}<br>"
            return ChatResponse(sessionId=req.sessionId, reply=reply, toolCall=None)
        except Exception as e:
            reply = f"Không lấy được chi tiết bệnh án: {e}"
            return ChatResponse(sessionId=req.sessionId, reply=reply, toolCall=None)
        
        
    # Intent: hỏi thông tin sản phẩm theo tên
    match_product_name = re.search(r"(?:sản phẩm|vật tư|thuốc|dụng cụ|vật dụng)\s+([\w\s\-]+)", user_msg, re.IGNORECASE)
    if match_product_name and not re.search(r"[0-9a-f\-]{16,}", user_msg):
        product_name = match_product_name.group(1).strip()
        try:
            url = f"{API_GATEWAY_URL}/products/search"
            headers = {"Authorization": f"Bearer {token}"} if token else {}
            payload = {"productName": product_name, "page": 0, "size": 10}
            async with httpx.AsyncClient(timeout=30) as client:
                r = await client.post(url, json=payload, headers=headers)
                r.raise_for_status()
                data = r.json()
            items = data.get('content', []) if isinstance(data, dict) else []
            # So khớp chính xác tên (không phân biệt hoa thường, loại bỏ khoảng trắng thừa)
            def norm(s):
                return (s or '').strip().lower()
            exact = [prod for prod in items if norm(prod.get('productName')) == norm(product_name)]
            if exact:
                prod = exact[0]
                reply = f"<b>Thông tin sản phẩm:</b><br>"
                reply += f"Tên: <b>{prod.get('productName','Không rõ')}</b><br>"
                reply += f"Mô tả: {prod.get('description','Không có mô tả')}<br>"
                reply += f"Giá: <b>{prod.get('price','?'):,}đ</b>"
            else:
                reply = f"Không tìm thấy sản phẩm tên '{product_name}'"
            return ChatResponse(sessionId=req.sessionId, reply=reply, toolCall=None)
        except Exception as e:
            reply = f"Không lấy được thông tin sản phẩm: {e}"
            return ChatResponse(sessionId=req.sessionId, reply=reply, toolCall=None)
        
    # Intent: hỏi thông tin dịch vụ theo tên
    match_service_name = re.search(r"(?:dịch vụ|service)\s+([\w\s\-]+)", user_msg, re.IGNORECASE)
    if match_service_name and not re.search(r"[0-9a-f\-]{16,}", user_msg):
        service_name = match_service_name.group(1).strip()
        try:
            # Sử dụng GET /services?serviceName=...
            import urllib.parse
            import unicodedata
            def norm(s):
                if not s: return ''
                s = unicodedata.normalize('NFKD', s)
                s = ''.join(c for c in s if not unicodedata.combining(c))
                return s.strip().lower()
            params = urllib.parse.urlencode({"serviceName": service_name, "page": 0, "size": 10})
            url = f"{API_GATEWAY_URL}/services?{params}"
            headers = {"Authorization": f"Bearer {token}"} if token else {}
            async with httpx.AsyncClient(timeout=30) as client:
                r = await client.get(url, headers=headers)
                r.raise_for_status()
                data = r.json()
            # Hỗ trợ cả kiểu Page (dict) và List
            if isinstance(data, dict) and 'content' in data:
                items = data.get('content', [])
            elif isinstance(data, list):
                items = data
            else:
                items = []
            # So khớp không dấu, không phân biệt hoa thường
            norm_service_name = norm(service_name)
            exact = [svc for svc in items if norm(svc.get('serviceName')) == norm_service_name]
            if exact:
                svc = exact[0]
                reply = f"<b>Thông tin dịch vụ:</b><br>"
                reply += f"Tên: <b>{svc.get('serviceName','Không rõ')}</b><br>"
                reply += f"Mô tả: {svc.get('description','Không có mô tả')}<br>"
                reply += f"Giá: <b>{svc.get('price','?'):,}đ</b>"
            else:
                reply = f"Không tìm thấy dịch vụ tên '{service_name}'"
            return ChatResponse(sessionId=req.sessionId, reply=reply, toolCall=None)
        except Exception as e:
            reply = f"Không lấy được thông tin dịch vụ: {e}"
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
                    if result.get('paymentLink'):
                        reply = (
                            "<b>Đặt lịch thành công!</b><br>"
                            f"Vui lòng thanh toán tại: <a href='{result['paymentLink']}' target='_blank'>Link thanh toán</a>"
                        )
                    else:
                        reply = "Đặt lịch thành công! (Không lấy được link thanh toán)"
                    tool_result = {"name": "appointment_create", "result": result}
                except Exception as e:
                    err = str(e)
                    if 'đã có người đặt' in err or 'trùng' in err:
                        reply = "<b>Lịch hẹn này đã có người đặt. Vui lòng chọn thời gian khác.</b>"
                    else:
                        reply = f"Không đặt được lịch: {err}"
                    tool_result = {"name": "appointment_create", "error": err}
                return ChatResponse(sessionId=req.sessionId, reply=reply, toolCall=tool_result)

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
