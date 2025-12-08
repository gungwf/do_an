import psycopg2
import uuid
import random
from datetime import datetime, timedelta
import math

# --- CẤU HÌNH KẾT NỐI DB ĐÃ CẬP NHẬT ---
DB_CONFIG = {
    "dbname": "do_an",
    "user": "postgres",
    "password": "123456789",  # Mật khẩu đã cập nhật
    "host": "localhost",
    "port": "5432"
}

# --- THÔNG TIN SẢN PHẨM MỤC TIÊU ---
TARGET_PRODUCT_NAME = "Men vi sinh Enterogermina"
TARGET_PRODUCT_ID = "b0c1d2e3-f4a5-6789-b0c1-d2e3f4a56789"
UNIT_PRICE = 140000.00
BRANCH_ID = "f7816c42-9d8c-4ac5-bd89-aea043049ff4"

def generate_medicine_sales():
    conn = None
    try:
        conn = psycopg2.connect(**DB_CONFIG)
        cur = conn.cursor()
        
        # --- THỜI GIAN VÀ CẤU HÌNH DATA ---
        DAYS_HISTORY = 730 # 2 năm lịch sử
        END_DATE = datetime.now()
        START_DATE = END_DATE - timedelta(days=DAYS_HISTORY)
        
        print(f"Đang tạo dữ liệu giả cho sản phẩm '{TARGET_PRODUCT_NAME}' ({DAYS_HISTORY} ngày)...")
        
        # Xóa dữ liệu cũ của sản phẩm mục tiêu
        print("Cảnh báo: Đang xóa dữ liệu cũ của sản phẩm này trong bills/bill_lines...")
        cur.execute(f"DELETE FROM bill_lines WHERE product_id = %s", (TARGET_PRODUCT_ID,))
        
        current_date = START_DATE
        i = 0
        while current_date <= END_DATE:
            
            # 1. Xu hướng Tăng nhẹ:
            trend = 0.08 * i # Tăng mạnh hơn phiên bản cũ
            
            # 2. Tính Mùa vụ (Chu kỳ 365 ngày):
            # Biên độ mạnh hơn để Prophet dễ học hơn (Biến động +- 20 đơn vị)
            seasonality = 20 * math.sin(2 * math.pi * i / 365) 
            
            # 3. Nhiễu (Random Noise):
            noise = random.randint(-8, 8) 
            
            # Tổng hợp số lượng: Đảm bảo số lượng bán không âm
            daily_quantity = int(max(1, 40 + seasonality + trend + noise)) # Base tăng lên 40
            
            # Tạo 1 hóa đơn giả cho ngày này
            bill_id = str(uuid.uuid4())
            total_amount = daily_quantity * UNIT_PRICE
            
            # Insert vào bảng BILLS
            sql_bill = """
                INSERT INTO bills (id, total_amount, status, bill_type, created_at, updated_at, currency, branch_id)
                VALUES (%s, %s, 'PAID', 'DRUG_PAYMENT', %s, %s, 'VND', %s)
            """
            cur.execute(sql_bill, (bill_id, total_amount, current_date, current_date, BRANCH_ID))
            
            # Insert vào bảng BILL_LINES
            sql_line = """
                INSERT INTO bill_lines (id, bill_id, product_id, quantity, unit_price, line_amount, created_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s)
            """
            line_id = str(uuid.uuid4())
            cur.execute(sql_line, (line_id, bill_id, TARGET_PRODUCT_ID, daily_quantity, UNIT_PRICE, total_amount, current_date))
            
            current_date += timedelta(days=1)
            i += 1

        conn.commit()
        print("Xong! Đã bơm dữ liệu lịch sử 2 năm vào bills và bill_lines.")

    except Exception as e:
        print(f"Lỗi khi kết nối/insert DB: {e}")
        if conn:
            conn.rollback()
    finally:
        if conn:
            cur.close()
            conn.close()

if __name__ == "__main__":
    generate_medicine_sales()