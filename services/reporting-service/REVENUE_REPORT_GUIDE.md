# Hướng dẫn Sử dụng Chức năng Báng Cáo Tài Chính

## Giới thiệu
Chức năng báng cáo tài chính cho phép bạn:
- Xem tóm tắt tài chính từ các hóa đơn (bills)
- Phân tích doanh thu theo loại hóa đơn
- Phân tích doanh thu theo chi nhánh
- Xuất báng cáo sang định dạng Excel hoặc PDF

## API Endpoints

### 1. Lấy Báng Cáo Tài Chính (JSON)
```
GET /reports/revenue
```

**Query Parameters:**
- `startDate` (optional): Ngày bắt đầu (ISO 8601 format: `2024-01-01T00:00:00Z`)
- `endDate` (optional): Ngày kết thúc (ISO 8601 format: `2024-12-31T23:59:59Z`)
- `billType` (optional): Loại hóa đơn (VD: TREATMENT, ONLINE, INSURANCE)
- `branchId` (optional): ID chi nhánh (UUID)

**Example Request:**
```bash
curl -X GET "http://localhost:8085/reports/revenue?startDate=2024-01-01T00:00:00Z&endDate=2024-12-31T23:59:59Z&billType=TREATMENT"
```

**Response:**
```json
{
  "code": 200,
  "message": "Báng cáo tài chính",
  "result": {
    "period": "01/01/2024 to 31/12/2024",
    "totalRevenue": 50000000.00,
    "totalBills": 150,
    "paidBills": 140,
    "pendingBills": 10,
    "paymentRate": 93.33,
    "revenueByBillType": {
      "TREATMENT": 40000000.00,
      "ONLINE": 10000000.00
    },
    "revenueByBranch": {
      "branch-id-1": 30000000.00,
      "branch-id-2": 20000000.00
    },
    "topProducts": []
  }
}
```

### 2. Xuất Báng Cáo sang Excel
```
GET /reports/revenue/export/excel
```

**Query Parameters:**
- Giống như endpoint lấy báng cáo JSON

**Example Request:**
```bash
curl -X GET "http://localhost:8085/reports/revenue/export/excel?startDate=2024-01-01T00:00:00Z&endDate=2024-12-31T23:59:59Z" \
  -H "Accept: application/octet-stream" \
  -o revenue_report.xlsx
```

### 3. Xuất Báng Cáo sang PDF
```
GET /reports/revenue/export/pdf
```

**Query Parameters:**
- Giống như endpoint lấy báng cáo JSON

**Example Request:**
```bash
curl -X GET "http://localhost:8085/reports/revenue/export/pdf?startDate=2024-01-01T00:00:00Z&endDate=2024-12-31T23:59:59Z" \
  -H "Accept: application/pdf" \
  -o revenue_report.pdf
```

## Thông tin Báng Cáo

### Các Chỉ số Chính (Main Metrics)
1. **Tổng Doanh Thu**: Tổng giá trị của tất cả hóa đơn đã thanh toán
2. **Tổng số Hóa Đơn**: Tổng số lượng hóa đơn
3. **Đã Thanh Toán**: Số lượng hóa đơn có trạng thái PAID
4. **Chưa Thanh Toán**: Số lượng hóa đơn có trạng thái PENDING
5. **Tỷ Lệ Thanh Toán**: Phần trăm hóa đơn đã thanh toán = (Đã Thanh Toán / Tổng số) × 100

### Doanh Thu Theo Loại Hóa Đơn
Phân tích tổng doanh thu cho từng loại hóa đơn:
- TREATMENT: Hóa đơn khám/điều trị trực tiếp
- ONLINE: Hóa đơn dịch vụ trực tuyến
- INSURANCE: Hóa đơn bảo hiểm
- etc.

### Doanh Thu Theo Chi Nhánh
Phân tích tổng doanh thu cho từng chi nhánh/phòng khám

## Cấu Hình

File `application.properties` đã cấu hình:

```properties
# Medical Record Service URL
medical.record.service.url=http://localhost:8083
```

Nếu cần thay đổi URL của medical-record-service, hãy cập nhật giá trị này.

## Cấu Trúc Project

```
reporting-service/
├── src/main/java/com/reporting_service/
│   ├── client/
│   │   ├── MedicalRecordServiceClient.java  (Feign client)
│   │   └── BillSearchItemDto.java
│   ├── dto/
│   │   ├── ApiResponse.java
│   │   ├── request/
│   │   │   └── RevenueReportRequest.java
│   │   └── response/
│   │       ├── RevenueReportDto.java
│   │       └── TopProductDto.java
│   ├── service/
│   │   ├── RevenueReportService.java  (Tính toán báng cáo)
│   │   └── export/
│   │       ├── ExcelExportService.java
│   │       └── PdfExportService.java
│   └── controller/
│       └── ReportController.java  (API endpoints)
```

## Lỗi Thường Gặp & Cách Giải Quyết

### 1. Không kết nối được đến medical-record-service
- Kiểm tra medical-record-service đang chạy
- Kiểm tra URL cấu hình đúng trong `application.properties`

### 2. Báng cáo không có dữ liệu
- Kiểm tra database có dữ liệu bill không
- Kiểm tra khoảng thời gian startDate/endDate
- Kiểm tra trạng thái của bill (phải là PAID để tính doanh thu)

### 3. Lỗi khi xuất PDF/Excel
- Kiểm tra dependency đã được thêm vào pom.xml
- Kiểm tra logs để xem chi tiết lỗi

## Phát Triển Tiếp Theo

Các tính năng có thể thêm:
1. Báng cáo Top sản phẩm (từ BillLine items)
2. Báng cáo theo từng bệnh nhân
3. Báng cáo so sánh tháng/quý
4. Báng cáo chi tiết từng loại dịch vụ
5. Thêm biểu đồ/visualizations
