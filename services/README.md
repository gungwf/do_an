# Tài liệu thu thập và phân tích yêu cầu hệ thống

Hệ thống được thiết kế theo kiến trúc microservices cho quản lý hoạt động phòng khám/bệnh viện, bao gồm các dịch vụ: `sys-service` (người dùng, phân quyền, chi nhánh, hồ sơ bác sĩ/bệnh nhân), `medical-record-service` (hồ sơ bệnh án, dịch vụ khám, hóa đơn), `appointment-service` (đặt lịch, thanh toán, đánh giá, theo dõi phác đồ), `product-inventory-service` (sản phẩm, tồn kho vật tư), `reporting-service` (báo cáo doanh thu), tất cả được điều phối qua `api-gateway` (cổng vào, CORS, định tuyến) và đăng ký/dò tìm dịch vụ qua `eureka-server`. Người dùng truy cập qua API Gateway tại các đường dẫn đã chuẩn hóa, dữ liệu luân chuyển giữa các dịch vụ theo chuẩn REST, hỗ trợ lọc, phân trang và xuất báo cáo.

## Mục đích hệ thống
- Hỗ trợ vận hành tổng thể phòng khám/bệnh viện: quản lý người dùng và phân quyền, quản lý chi nhánh, đặt lịch khám, lập và lưu hồ sơ bệnh án, quản lý dịch vụ và vật tư, tạo hóa đơn và thanh toán, theo dõi kết quả và trải nghiệm bệnh nhân, và tổng hợp báo cáo tài chính/doanh thu.
- Chuẩn hóa quy trình nghiệp vụ, giảm sai sót thủ công, tăng khả năng truy vết và phân tích dữ liệu.

## Phạm vi hệ thống (vai trò và chức năng)
- Người quản trị hệ thống (Admin): quản lý người dùng, phân quyền (`User`, `UserRole`), quản lý chi nhánh (`Branch`), cấu hình hồ sơ bác sĩ (`DoctorProfile`) và bệnh nhân (`PatientProfile`).
- Nhân viên/Điều phối (Staff): tiếp nhận lịch hẹn (`Appointment`), điều phối phác đồ (`ProtocolTracking`), xử lý thanh toán (`Payment`).
- Bác sĩ (Doctor): tạo và cập nhật hồ sơ bệnh án (`MedicalRecord`), chẩn đoán (`DiagnosisTemplate`), chỉ định dịch vụ (`Service`), lập đơn thuốc (`PrescriptionItem`, `PrescriptionTemplateItem`), ghi nhận dịch vụ vật tư (`ServiceMaterial`).
- Bệnh nhân (Patient): đặt lịch, xem lịch hẹn, đánh giá dịch vụ (`Review`).
- Kế toán/Tài chính (Accounting): theo dõi hóa đơn (`Bill`, `BillLine`, `BillStatus`, `BillType`), đối soát, xem và xuất báo cáo doanh thu qua `reporting-service`.

## Hoạt động nghiệp vụ các chức năng
- Đặt lịch và tiếp nhận: tạo `Appointment`, kiểm tra `Available Slots`, theo dõi trạng thái `AppointmentStatus`, ghi nhận đánh giá `Review` sau khám; theo dõi tiến trình `ProtocolTracking` nếu có phác đồ.
- Khám và điều trị: lập `MedicalRecord`, chọn `Service`/`ServiceMaterial`, áp dụng `Protocol`/`ProtocolServiceLink`, sử dụng mẫu chẩn đoán/đơn thuốc (`DiagnosisTemplate`, `PrescriptionTemplateItem`).
- Thanh toán và hóa đơn: tạo `Bill` gồm nhiều `BillLine`, theo dõi `PaymentStatus`/`BillStatus`, phân loại `BillType` để tổng hợp doanh thu; dữ liệu hóa đơn là nguồn cho báo cáo.
- Quản lý sản phẩm và tồn kho: quản lý `Product`, `Inventory` với khóa `InventoryId`, đảm bảo vật tư sẵn có cho dịch vụ; liên hệ với `ServiceMaterial` trong hồ sơ bệnh án.
- Báo cáo doanh thu: `reporting-service` tổng hợp theo thời gian, loại hóa đơn, chi nhánh; hỗ trợ xuất Excel/PDF và API JSON (`/reports/**`).

## Thông tin các đối tượng cần xử lý, quản lý
- Hệ thống: `User`, `UserRole`, `Branch`, `DoctorProfile`, `PatientProfile`.
- Lịch hẹn & phản hồi: `Appointment`, `Review`, `ProtocolTracking`, `Payment`.
- Hồ sơ bệnh án & dịch vụ: `MedicalRecord`, `Service`, `ServiceMaterial`, `Protocol`, `DiagnosisTemplate`, `PrescriptionItem`, `PrescriptionTemplateItem`.
- Tài chính: `Bill`, `BillLine`, `BillStatus`, `BillType`.
- Sản phẩm & kho: `Product`, `Inventory`, `InventoryId`, `ProductType`.

## Quan hệ giữa các đối tượng (khái quát)
- Người dùng thuộc vai trò (`User`—`UserRole`), có thể gắn hồ sơ bác sĩ (`DoctorProfile`) hoặc bệnh nhân (`PatientProfile`), và thuộc chi nhánh (`Branch`).
- Lịch hẹn (`Appointment`) liên kết bệnh nhân và bác sĩ; có thanh toán (`Payment`), đánh giá (`Review`) và theo dõi phác đồ (`ProtocolTracking`).
- Hồ sơ bệnh án (`MedicalRecord`) liên kết bệnh nhân, bác sĩ, chi nhánh; chứa chẩn đoán, dịch vụ (`Service`), vật tư (`ServiceMaterial`), đơn thuốc; có thể tham chiếu `Protocol`.
- Hóa đơn (`Bill`) liên kết hồ sơ bệnh án/ dịch vụ thực hiện, gồm nhiều `BillLine`; trạng thái (`BillStatus`) và loại (`BillType`) phục vụ tổng hợp doanh thu và báo cáo.
- Dịch vụ vật tư tiêu hao liên hệ tới kho (`Inventory`, `Product`); xuất/nhập kho phản ánh qua `InventoryId` và loại sản phẩm (`ProductType`).

## Kiến trúc triển khai
- API Gateway (`api-gateway`, port 8080) định tuyến đến các dịch vụ: `/auth`, `/users`, `/branches`, `/doctor-profiles`, `/patient-profiles` (sys-service); `/appointments`, `/reviews`, `/slots`, `/protocol-tracking`, `/api/v1` (appointment-service); `/services`, `/protocols`, `/medical-records`, `/service-materials`, `/templates`, `/bills` (medical-record-service); `/products`, `/inventory` (product-inventory-service); `/reports` (reporting-service); tích hợp chatbot `/api/ai/**` (dịch vụ ngoài cổng).
- Eureka Server (`eureka-server`, port 8761) quản lý đăng ký và phát hiện dịch vụ.
- Các dịch vụ Spring Boot cấu hình qua `application.yml`/`properties`, giao tiếp nội bộ dùng `lb://service-name` qua Eureka.

## Tham khảo nhanh báo cáo doanh thu
- Endpoint: `GET /reports/revenue` kèm tham số `startDate`, `endDate`, `billType`, `branchId`.
- Xuất file: `GET /reports/revenue/export/excel`, `GET /reports/revenue/export/pdf`.

