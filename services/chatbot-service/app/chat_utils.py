import os
import re
import datetime
import logging
import httpx
import unicodedata
from typing import Tuple, Optional, Dict, Any

API_GATEWAY_URL = os.getenv("API_GATEWAY_URL", "http://localhost:8080")
logging.basicConfig(level=logging.INFO)

# --- BỘ NHỚ TẠM (In-memory storage) ---
# Cấu trúc: { "session_id_1": { "doctor_name": "Hoa", "date": "20/12/2025", "time": None, "step": "asking_time" } }
booking_states = {} 

# --- HELPER FUNCTIONS ---

async def call_gateway_create_appointment(args: Dict[str, Any], token: str = "") -> Dict[str, Any]:
    url = f"{API_GATEWAY_URL}/appointments"
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    if not args.get("doctorId") or not args.get("appointmentTime"):
        raise Exception("Thiếu thông tin bác sĩ hoặc thời gian.")
    async with httpx.AsyncClient(timeout=30) as client:
        r = await client.post(url, json=args, headers=headers)
        if r.status_code == 400: raise Exception("Khung giờ này có thể đã bị trùng.")
        r.raise_for_status()
        appointment = r.json()
        if appointment.get('id'):
            try:
                pay_url = f"{API_GATEWAY_URL}/api/v1/payment/create-payment/{appointment['id']}"
                pay_resp = await client.post(pay_url, headers=headers)
                if pay_resp.status_code == 200: appointment['paymentLink'] = pay_resp.text.strip('"')
            except: pass
        return appointment

async def search_doctor_by_name(name: str, token: str) -> list:
    url = f"{API_GATEWAY_URL}/users/doctors/search"
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    async with httpx.AsyncClient(timeout=30) as client:
        r = await client.post(url, headers=headers, json={"fullName": name, "page": 0, "size": 5})
        return r.json().get("content", []) if r.status_code == 200 else []

async def get_doctor_list(token: str) -> list:
    url = f"{API_GATEWAY_URL}/users/doctors/simple"
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    async with httpx.AsyncClient(timeout=30) as client:
        r = await client.get(url, headers=headers)
        return r.json() if r.status_code == 200 else []

def parse_booking_datetime(user_msg: str) -> Tuple[Optional[str], Optional[str]]:
    user_msg = user_msg.lower()
    now = datetime.datetime.now()
    date_str, time_str = None, None

    # Parse Date
    match_full = re.search(r"(\d{1,2})[/-](\d{1,2})[/-](\d{4})", user_msg)
    match_short = re.search(r"(\d{1,2})[/-](\d{1,2})(?!\d)", user_msg)
    match_day = re.search(r"ngày (\d{1,2})", user_msg)
    
    if match_full: date_str = f"{match_full.group(1).zfill(2)}/{match_full.group(2).zfill(2)}/{match_full.group(3)}"
    elif match_short: date_str = f"{match_short.group(1).zfill(2)}/{match_short.group(2).zfill(2)}/{now.year}"
    elif match_day: date_str = f"{match_day.group(1).zfill(2)}/{now.month:02d}/{now.year}"
    elif "ngày mai" in user_msg or "mai" in user_msg: date_str = (now + datetime.timedelta(days=1)).strftime("%d/%m/%Y")
    elif "hôm nay" in user_msg: date_str = now.strftime("%d/%m/%Y")

    # Parse Time
    match_time = re.search(r"(\d{1,2})\s*(?::|h|g|giờ)\s*(\d{2})?", user_msg)
    if not match_time: match_time = re.search(r"(?:lúc|tại)\s+(\d{1,2})\b", user_msg)
    
    if match_time:
        try:
            h, m = int(match_time.group(1)), int(match_time.group(2) or 0)
            if ("chiều" in user_msg or "tối" in user_msg) and h < 12: h += 12
            time_str = f"{h:02d}:{m:02d}"
        except: pass
        
    return date_str, time_str

# --- LOGIC ĐẶT LỊCH THÔNG MINH (MULTI-TURN) ---

async def handle_booking_intent(user_msg: str, token: str, session_id: str) -> Tuple[Optional[str], Optional[Dict]]:
    msg_lower = user_msg.lower()

    # 1. Hủy lệnh nếu người dùng muốn dừng
    if any(w in msg_lower for w in ["hủy", "thôi", "dừng", "cancel", "không đặt nữa"]):
        if session_id in booking_states:
            del booking_states[session_id]
            return "Đã hủy yêu cầu đặt lịch.", None

    # 2. Lấy hoặc tạo trạng thái mới (State)
    state = booking_states.get(session_id)
    
    # Nếu chưa có state, kiểm tra xem có phải user muốn bắt đầu đặt lịch không
    if not state:
        if "đặt lịch" in msg_lower or "khám bệnh" in msg_lower or "bác sĩ" in msg_lower:
            state = {"doctor_name": None, "date_str": None, "time_str": None}
            booking_states[session_id] = state
        else:
            # Nếu không có từ khóa đặt lịch và không có state -> Không phải việc của hàm này
            return None, None

    # 3. Trích xuất thông tin từ tin nhắn hiện tại (Fill in the blanks)
    
    # 3a. Thử tìm tên bác sĩ
    # Regex ưu tiên: "bác sĩ Hoa", "bs Minh"
    match_doc = re.search(r"(?:bác sĩ|bs|bác sỹ)\s+([\w .-]+?)(?:\s+(?:vào lúc|lúc|ngày|tại|chi nhánh)\b|[.,;!?]|$)", user_msg, re.IGNORECASE)
    if match_doc:
        state["doctor_name"] = match_doc.group(1).strip()
    
    # Logic fallback: Nếu user trả lời ngắn (VD: "Hoa", "Minh") thì coi là tên.
    elif not state["doctor_name"]:
        # Chỉ nhận diện là tên nếu câu ngắn (<= 4 từ) VÀ không chứa số
        if len(user_msg.split()) <= 4 and not any(c.isdigit() for c in user_msg):
            potential_name = user_msg.replace("bác sĩ", "").replace("bs", "").strip()
            
            # Danh sách từ khóa cấm (Blocklist) - Nếu dính từ này thì KHÔNG PHẢI tên
            forbidden_words = ["đặt", "lịch", "khám", "muốn", "tôi", "cho", "em", "anh", "chị", "ơi", "ad"]
            
            # Chỉ chấp nhận nếu KHÔNG chứa từ cấm nào
            if not any(w in potential_name.lower() for w in forbidden_words):
                state["doctor_name"] = potential_name

    # 3b. Thử tìm ngày và giờ
    d_new, t_new = parse_booking_datetime(user_msg)
    if d_new: state["date_str"] = d_new
    if t_new: state["time_str"] = t_new

    # 4. Kiểm tra xem còn thiếu gì không? (Decision Logic)
    
    # Thiếu bác sĩ?
    if not state["doctor_name"]:
        return "Bạn muốn đặt lịch khám với <b>bác sĩ nào</b>? (Ví dụ: Bác sĩ Hoa)", None

    # Thiếu ngày?
    if not state["date_str"]:
        return f"Bạn muốn khám với Bác sĩ {state['doctor_name']} vào <b>ngày nào</b>?", None

    # Thiếu giờ?
    if not state["time_str"]:
        return f"Bạn muốn khám vào <b>lúc mấy giờ</b> ngày {state['date_str']}?", None

    # 5. Đã đủ thông tin -> Thực hiện đặt lịch (Execute)
    try:
        # Tìm ID bác sĩ
        doctors = await search_doctor_by_name(state["doctor_name"], token)
        if not doctors:
            # Tìm không thấy -> Xóa tên bác sĩ sai đi để hỏi lại
            wrong_name = state["doctor_name"]
            state["doctor_name"] = None 
            return f"Không tìm thấy bác sĩ tên '<b>{wrong_name}</b>'. Vui lòng nhập lại tên bác sĩ chính xác.", None
        
        doc = doctors[0]
        
        # Tạo thời gian ISO
        import pytz
        tz = pytz.timezone('Asia/Ho_Chi_Minh')
        fmt = '%Y-%m-%d %H:%M' if '-' in state["date_str"] else '%d/%m/%Y %H:%M'
        dt = datetime.datetime.strptime(f"{state['date_str']} {state['time_str']}", fmt)
        dt = tz.localize(dt)
        appointment_time = dt.isoformat()
        
        # Map branch (đơn giản hóa: lấy branch của bác sĩ)
        branch_id = doc.get('branchId')
        
        args = {"doctorId": doc['id'], "appointmentTime": appointment_time, "branchId": branch_id}
        
        # Gọi API
        result = await call_gateway_create_appointment(args, token)
        
        # Thành công -> Xóa state để user có thể đặt lịch mới sau này
        del booking_states[session_id]
        
        reply = f"<b>Đã đặt lịch thành công với BS {doc.get('fullName')}!</b><br>Thời gian: {state['time_str']} - {state['date_str']}"
        if result.get('paymentLink'):
            reply += f"<br>Vui lòng thanh toán tại: <a href='{result['paymentLink']}' target='_blank'>Link này</a>"
            
        return reply, {"name": "appointment_create", "result": result}

    except Exception as e:
        err = str(e)
        # Nếu lỗi trùng lịch, giữ lại state nhưng xóa giờ để user chọn giờ khác
        if 'trùng' in err.lower():
            state["time_str"] = None
            return f"Giờ khám <b>{state['time_str']}</b> ngày {state['date_str']} đã kín. Vui lòng chọn <b>giờ khác</b>.", None
        
        # Lỗi khác thì reset luôn
        del booking_states[session_id]
        return f"Lỗi đặt lịch: {err}. Vui lòng thử lại từ đầu.", {"error": err}

# --- LOGIC TRA CỨU KHÁC ---
def normalize_str(s: str) -> str:
    if not s: return ""
    return ''.join(c for c in unicodedata.normalize('NFKD', s) if not unicodedata.combining(c)).lower().strip()

async def handle_lookup_intent(user_msg: str, token: str) -> Optional[str]:
    msg_lower = normalize_str(user_msg)

    # ---------------------------------------------------------
    # 1. TRA CỨU LỊCH SỬ KHÁM (Yêu cầu đăng nhập)
    # ---------------------------------------------------------
    if any(k in msg_lower for k in ["lich su kham", "ho so kham", "don thuoc", "kham gan nhat"]):
        if not token:
            return "Bạn cần <b>đăng nhập</b> để xem lịch sử khám bệnh và đơn thuốc."
        
        try:
            url = f"{API_GATEWAY_URL}/medical-records/patient/me?page=0&size=5"
            async with httpx.AsyncClient(timeout=30) as client:
                r = await client.get(url, headers={"Authorization": f"Bearer {token}"})
                
                if r.status_code != 200:
                    return "Không thể lấy dữ liệu lịch sử. Vui lòng thử lại sau."
                
                records = r.json().get('content', [])
                if not records:
                    return "Bạn chưa có hồ sơ khám bệnh nào tại hệ thống."

                # Format hiển thị đẹp
                html = "<b>Hồ sơ khám gần nhất của bạn:</b><br>"
                for rec in records:
                    appt = rec.get('appointment', {})
                    # Lấy ngày giờ, cắt lấy phần ngày yyyy-mm-dd
                    date_time = appt.get('appointmentTime', rec.get('createdAt', ''))
                    date_display = date_time[:10] if date_time else "N/A"
                    
                    doctor_name = appt.get('doctor', {}).get('fullName', 'Bác sĩ ?')
                    diagnosis = rec.get('diagnosis', 'Chưa có chẩn đoán')
                    
                    html += f"📅 <b>{date_display}</b> - {doctor_name}<br>"
                    html += f"<i>➡ Chẩn đoán: {diagnosis}</i><br>"
                    html += "--------------------<br>"
                
                html += "<i>Nhập 'chi tiết + ngày' để xem đơn thuốc cụ thể.</i>"
                return html
        except Exception as e:
            logging.error(f"Lỗi tra cứu lịch sử: {e}")
            return "Hệ thống đang bảo trì chức năng tra cứu."

    # ---------------------------------------------------------
    # 2. TRA CỨU SẢN PHẨM / THUỐC (Product Search)
    # ---------------------------------------------------------
    # Regex bắt: "giá thuốc panadol", "sản phẩm vitamin", "thông tin thuốc X"
    match_prod = re.search(r"(san pham|thuoc|gia thuoc)\s+([\w\s\-]+)", msg_lower)
    if match_prod:
        keyword = match_prod.group(2).strip()
        try:
            url = f"{API_GATEWAY_URL}/products/search"
            payload = {"productName": keyword, "page": 0, "size": 10} # Lấy nhiều hơn chút để lọc
            
            headers = {}
            if token:
                headers = {"Authorization": f"Bearer {token}"}

            async with httpx.AsyncClient(timeout=30) as client:
                r = await client.post(url, json=payload, headers=headers) 
                # Nếu API trả về danh sách tất cả sản phẩm (do backend xử lý search chưa tốt)
                # Ta cần lọc cứng ở đây
                products = r.json().get('content', []) if r.status_code == 200 else []

            # --- LOGIC LỌC NGHIÊM NGẶT ---
            # Chỉ lấy sản phẩm mà tên có chứa từ khóa user nhập
            key_norm = normalize_str(keyword)
            results = [p for p in products if key_norm in normalize_str(p.get('productName', ''))]
            
            # [QUAN TRỌNG] XÓA DÒNG FALLBACK CŨ: if not results and products: results = products
            
            if results:
                html = f"<b>Kết quả tìm kiếm '{keyword}':</b><br>"
                for p in results[:3]: # Chỉ hiện 3 cái khớp nhất
                    price = f"{p.get('price', 0):,}".replace(",", ".")
                    html += f"💊 <b>{p.get('productName')}</b> - {price}đ<br>"
                    desc = p.get('description')
                    if desc: html += f"<i>({desc[:50]}...)</i><br>"
                return html
            else:
                # Nếu lọc xong mà danh sách rỗng -> Báo không tìm thấy
                return f"Không tìm thấy sản phẩm nào có tên chứa từ khóa '<b>{keyword}</b>'."

        except Exception as e:
             logging.error(f"Lỗi tra cứu thuốc: {e}")
             return f"Lỗi hệ thống khi tra cứu thuốc."

    # ---------------------------------------------------------
    # 3. TRA CỨU DỊCH VỤ (NÂNG CẤP TÌM KIẾM THÔNG MINH)
    # ---------------------------------------------------------
    if any(k in msg_lower for k in ["dich vu", "gia kham", "chi phi", "bang gia"]):
        try:
            headers = {}
            if token:
                headers = {"Authorization": f"Bearer {token}"}

            async with httpx.AsyncClient(timeout=30) as client:
                r = await client.get(f"{API_GATEWAY_URL}/services", headers=headers)
                services = r.json() if r.status_code == 200 else []
                if isinstance(services, dict): services = services.get('content', [])

            # --- BƯỚC 1: LÀM SẠCH TỪ KHÓA ---
            clean_msg = user_msg.lower()
            # Xóa các từ nối để lại từ khóa chính (VD: "giá khám răng" -> "răng")
            for w in ["giá dịch vụ", "dịch vụ", "giá khám", "chi phí", "bảng giá", "giá"]:
                clean_msg = clean_msg.replace(w, "")
            
            keyword = clean_msg.strip()
            key_norm = normalize_str(keyword)
            
            # Tách từ khóa thành các từ đơn (VD: "rang tong quat" -> ['rang', 'tong', 'quat'])
            key_tokens = key_norm.split()

            found_services = []

            # --- BƯỚC 2: THUẬT TOÁN TÍNH ĐIỂM KHỚP (SCORING) ---
            if not key_norm or len(key_norm) < 2:
                # Nếu không có từ khóa (Hỏi chung chung) -> Lấy top 5
                found_services = [(s, 0) for s in services[:5]]
                header_text = "Bảng giá các dịch vụ phổ biến:"
            else:
                header_text = f"Kết quả tìm kiếm '{keyword}':"
                for s in services:
                    s_name = s.get('serviceName', '')
                    s_norm = normalize_str(s_name)
                    
                    # Logic 1: Tìm chính xác (Tuyệt đối)
                    if key_norm in s_norm:
                        found_services.append((s, 100)) # Điểm cao nhất
                        continue
                    
                    # Logic 2: Tìm ngược (User tìm dài hơn tên dịch vụ)
                    # VD: User tìm "Khám răng tổng quát", DB có "Khám răng" -> Khớp
                    if len(s_norm) > 4 and s_norm in key_norm:
                        found_services.append((s, 90)) # Điểm cao nhì
                        continue

                    # Logic 3: Tìm theo từ khóa (Khớp từng từ)
                    # Đếm xem có bao nhiêu từ của User xuất hiện trong tên Dịch vụ
                    s_tokens = s_norm.split()
                    matches = 0
                    for k in key_tokens:
                        if any(k in t for t in s_tokens): # k nằm trong t
                            matches += 1
                    
                    # Nếu khớp trên 50% số từ khóa -> Chấp nhận
                    if len(key_tokens) > 0 and (matches / len(key_tokens)) >= 0.5:
                         found_services.append((s, matches * 10))

                # Sắp xếp kết quả theo điểm số (cao xuống thấp)
                found_services.sort(key=lambda x: x[1], reverse=True)

            # --- BƯỚC 3: HIỂN THỊ ---
            if found_services:
                html = f"<b>{header_text}</b><br>"
                # Chỉ lấy tối đa 5 kết quả tốt nhất
                for item in found_services[:5]:
                    s = item[0]
                    price = f"{s.get('price', 0):,}".replace(",", ".")
                    html += f"💉 {s.get('serviceName')}: <b>{price}đ</b><br>"
                return html
            else:
                return f"Không tìm thấy dịch vụ nào liên quan đến '<b>{keyword}</b>'.<br><i>Gợi ý: Thử nhập từ khóa ngắn hơn (VD: 'răng', 'siêu âm').</i>"

        except Exception as e:
             logging.error(f"Lỗi tra cứu dịch vụ: {e}")
             return "Lỗi hệ thống tra cứu dịch vụ."