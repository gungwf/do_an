from flask import Flask, jsonify, request
from flask_cors import CORS
import pandas as pd
from prophet import Prophet
import psycopg2
import logging
from datetime import datetime, timedelta
import warnings

# --- CẤU HÌNH CHUNG ---
app = Flask(__name__)
CORS(app) 
logging.getLogger('prophet').setLevel(logging.WARNING)
warnings.filterwarnings("ignore")

# CẤU HÌNH DB ĐÃ CẬP NHẬT
DB_CONFIG = {
    "dbname": "do_an",
    "user": "postgres",
    "password": "123456789", 
    "host": "localhost",
    "port": "5432"
}

# --- DB CONNECTION HELPER ---
def get_db_connection():
    return psycopg2.connect(**DB_CONFIG)

# --- LOGIC XỬ LÝ DỰ BÁO CỐT LÕI ---
def run_forecast_query(query):
    conn = None
    try:
        conn = get_db_connection()
        df = pd.read_sql(query, conn)
        conn.close()

        # Kiểm tra dữ liệu
        if len(df) < 15: 
            return {"error": "Không đủ dữ liệu lịch sử (ít hơn 15 dòng) để mô hình học."}, 400

        # Chuẩn bị dữ liệu cho Prophet
        df = df.rename(columns={'ds_raw': 'ds', 'y_raw': 'y'})
        df['ds'] = pd.to_datetime(df['ds'])
        
        # Chạy AI (Prophet)
        m = Prophet(yearly_seasonality=True, weekly_seasonality=True, daily_seasonality=False)
        m.fit(df)
        future = m.make_future_dataframe(periods=7) 
        forecast = m.predict(future)
        
        # --- LOGIC TRÍCH XUẤT CỘT BỀN VỮNG ---
        cols_to_extract = ['ds', 'yhat', 'yhat_lower', 'yhat_upper', 'trend']
        
        if 'yearly_seasonality' in forecast.columns:
            cols_to_extract.append('yearly_seasonality')
        if 'weekly_seasonality' in forecast.columns:
            cols_to_extract.append('weekly_seasonality')
            
        # Total output rows = All historical + 7 forecast days
        total_output_rows = len(df) + 7 
        result = forecast[cols_to_extract].tail(total_output_rows)
        
        # Chuyển đổi và làm tròn kết quả
        result['ds'] = result['ds'].dt.strftime('%Y-%m-%dT%H:%M:%S')
        
        # Làm tròn tất cả các cột số
        for col in result.columns:
            if col not in ['ds']:
                result[col] = result[col].apply(lambda x: max(0, round(x))).astype(int)

        return result.to_dict(orient='records'), 200

    except Exception as e:
        logging.error(f"LỖI HỆ THỐNG TRONG FORECAST: {e}")
        if conn: conn.close()
        return {"error": f"Lỗi hệ thống: {e}"}, 500

# --- API DỰ BÁO ĐƠN LẺ (GET) ---
@app.route('/api/forecast/medicine/<branch_id>/<product_id>', methods=['GET'])
def forecast_medicine_single(branch_id, product_id):
    try:
        history_days = int(request.args.get('history_days', 730))
    except ValueError:
        return jsonify({"error": "Tham số 'history_days' phải là số nguyên."}), 400

    start_date = (datetime.now() - timedelta(days=history_days)).strftime('%Y-%m-%d')
    
    query = f"""
        SELECT date(b.created_at) as ds_raw, sum(bl.quantity) as y_raw
        FROM bill_lines bl
        JOIN bills b ON bl.bill_id = b.id
        WHERE b.branch_id = '{branch_id}' 
          AND bl.product_id = '{product_id}'
          AND b.bill_type = 'DRUG_PAYMENT' 
          AND b.status = 'PAID'
          AND b.created_at >= '{start_date}'
        GROUP BY 1
        ORDER BY 1
    """
    forecast_data, status_code = run_forecast_query(query)
    
    if status_code == 200:
        return jsonify(forecast_data)
    else:
        return jsonify({"error": f"Không đủ dữ liệu lịch sử ({history_days} ngày) cho dự báo."}, status_code)


# --- API DỰ BÁO LÔ (POST BATCH) ---
@app.route('/api/forecast/medicine/batch', methods=['POST'])
def forecast_medicine_batch():
    try:
        data = request.get_json()
        branch_id = data.get('branch_id')
        product_ids = data.get('product_ids', [])
        history_days = data.get('history_days', 730)
        
        if not branch_id or not product_ids:
            return jsonify({"error": "Missing branch_id or product_ids"}), 400
        
        start_date = (datetime.now() - timedelta(days=history_days)).strftime('%Y-%m-%d')
        results = {}
        conn = get_db_connection()
        
        for product_id in product_ids:
            try:
                query = f"""
                    SELECT 
                        date(b.created_at) as ds_raw,
                        sum(bl.quantity) as y_raw,
                        p.product_name as product_name 
                    FROM bill_lines bl
                    JOIN bills b ON bl.bill_id = b.id
                    JOIN products p ON bl.product_id = p.id
                    WHERE b.branch_id = '{branch_id}' 
                      AND bl.product_id = '{product_id}'
                      AND b.bill_type = 'DRUG_PAYMENT'
                      AND b.status = 'PAID'
                      AND b.created_at >= '{start_date}'
                    GROUP BY 1, p.product_name 
                    ORDER BY 1
                """
                
                df = pd.read_sql(query, conn)
                
                if len(df) < 15:
                    results[product_id] = {
                        "success": False,
                        "error": f"Không đủ dữ liệu (chỉ có {len(df)} ngày)"
                    }
                    continue
                
                # Chạy mô hình Prophet
                df_prophet = df[['ds_raw', 'y_raw']].copy().rename(columns={'ds_raw': 'ds', 'y_raw': 'y'})
                df_prophet['ds'] = pd.to_datetime(df_prophet['ds'])
                
                m = Prophet()
                m.fit(df_prophet)
                future = m.make_future_dataframe(periods=7)
                forecast = m.predict(future)
                
                # Trích xuất 7 ngày dự báo cuối cùng
                cols_to_extract = ['ds', 'yhat', 'yhat_lower', 'yhat_upper', 'trend']
                if 'yearly_seasonality' in forecast.columns: cols_to_extract.append('yearly_seasonality')
                if 'weekly_seasonality' in forecast.columns: cols_to_extract.append('weekly_seasonality')
                
                result_data = forecast[cols_to_extract].tail(7).copy()
                
                # Chuyển đổi và làm tròn
                result_data['ds'] = result_data['ds'].dt.strftime('%Y-%m-%dT%H:%M:%S')
                for col in result_data.columns:
                    if col not in ['ds']:
                        result_data[col] = result_data[col].apply(lambda x: max(0, round(x))).astype(int)
                
                results[product_id] = {
                    "success": True,
                    "product_name": df['product_name'].iloc[0],
                    "forecast": result_data.to_dict('records'),
                    "data_points": len(df)
                }
            except Exception as e:
                logging.error(f"Error forecasting product {product_id}: {e}")
                results[product_id] = {
                    "success": False,
                    "error": str(e)
                }
        
        conn.close()
        return jsonify(results), 200
        
    except Exception as e:
        logging.error(f"Batch forecast error: {str(e)}")
        return jsonify({"error": f"Lỗi hệ thống: {str(e)}"}), 500

# --- API DỰ BÁO VẬT TƯ LÔ (SUPPLY BATCH) ---
@app.route('/api/forecast/supply/batch', methods=['POST'])
def forecast_supply_batch():
    # Logic tương tự Medicine nhưng dùng truy vấn Supply
    try:
        data = request.get_json()
        product_ids = data.get('product_ids', [])
        history_days = data.get('history_days', 730)
        
        if not product_ids:
            return jsonify({"error": "Missing product_ids"}), 400
        
        start_date = (datetime.now() - timedelta(days=history_days)).strftime('%Y-%m-%d')
        results = {}
        conn = get_db_connection()
        
        for product_id in product_ids:
            try:
                # Query Supply: JOIN medical_record_services, service_materials, products
                query = f"""
                    SELECT 
                        date(mrs.created_at) as ds_raw,
                        sum(sm.quantity_consumed) as y_raw,
                        p.product_name as product_name
                    FROM medical_record_services mrs
                    JOIN service_materials sm ON mrs.service_id = sm.service_id
                    JOIN products p ON sm.product_id = p.id
                    WHERE sm.product_id = '{product_id}'
                      AND mrs.created_at >= '{start_date}'
                    GROUP BY 1, p.product_name
                    ORDER BY 1
                """
                
                df = pd.read_sql(query, conn)
                
                if len(df) < 15:
                    results[product_id] = {"success": False, "error": f"Không đủ dữ liệu (chỉ có {len(df)} ngày)"}
                    continue
                
                # Chạy mô hình Prophet (Logic tương tự Medicine Batch)
                df_prophet = df[['ds_raw', 'y_raw']].copy().rename(columns={'ds_raw': 'ds', 'y_raw': 'y'})
                df_prophet['ds'] = pd.to_datetime(df_prophet['ds'])
                m = Prophet()
                m.fit(df_prophet)
                future = m.make_future_dataframe(periods=7)
                forecast = m.predict(future)
                
                # Trích xuất 7 ngày dự báo cuối cùng
                cols_to_extract = ['ds', 'yhat', 'yhat_lower', 'yhat_upper', 'trend']
                if 'yearly_seasonality' in forecast.columns: cols_to_extract.append('yearly_seasonality')
                if 'weekly_seasonality' in forecast.columns: cols_to_extract.append('weekly_seasonality')
                
                result_data = forecast[cols_to_extract].tail(7).copy()
                
                # Chuyển đổi và làm tròn
                result_data['ds'] = result_data['ds'].dt.strftime('%Y-%m-%dT%H:%M:%S')
                for col in result_data.columns:
                    if col not in ['ds']:
                        result_data[col] = result_data[col].apply(lambda x: max(0, round(x))).astype(int)
                
                results[product_id] = {
                    "success": True,
                    "product_name": df['product_name'].iloc[0],
                    "forecast": result_data.to_dict('records'),
                    "data_points": len(df)
                }
            except Exception as e:
                logging.error(f"Error forecasting supply {product_id}: {e}")
                results[product_id] = {
                    "success": False,
                    "error": str(e)
                }
        
        conn.close()
        return jsonify(results), 200
    except Exception as e:
        logging.error(f"Batch forecast error: {str(e)}")
        return jsonify({"error": f"Lỗi hệ thống: {str(e)}"}), 500

# --- API LẤY DANH SÁCH SẢN PHẨM CỦA CHI NHÁNH (UTILITY) ---
@app.route('/api/forecast/branch/<branch_id>/products', methods=['GET'])
def get_branch_products(branch_id):
    try:
        conn = get_db_connection()
        query = f"""
            SELECT 
                i.product_id,
                p.product_name,
                i.quantity,
                p.image_url
            FROM inventories i
            JOIN products p ON i.product_id = p.id
            WHERE i.branch_id = '{branch_id}'
            ORDER BY i.quantity ASC
        """
        
        df = pd.read_sql(query, conn)
        conn.close()
        
        return jsonify(df.to_dict('records')), 200
        
    except Exception as e:
        logging.error(f"Get products error: {str(e)}")
        return jsonify({"error": f"Lỗi hệ thống: {str(e)}"}), 500


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)