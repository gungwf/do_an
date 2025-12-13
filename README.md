
# Tổng quan dự án Quản lý Y tế


## 1. Mục tiêu dự án
Hệ thống được xây dựng với mục đích chính là giúp khách hàng (bệnh nhân) chủ động đăng ký lịch khám bệnh với bác sĩ, từ đó tối ưu hóa thời gian đến khám, giảm thiểu tình trạng chờ đợi và hạn chế tập trung đông người tại cơ sở y tế, đặc biệt trong thời kỳ dịch bệnh phức tạp.

Bên cạnh đó, hệ thống còn cung cấp các tính năng cập nhật tin tức y tế mới nhất, chia sẻ cẩm nang sức khỏe do các bác sĩ biên soạn, giúp khách hàng nâng cao kiến thức và bảo vệ sức khỏe trong mùa dịch.

Đối với bác sĩ, hệ thống hỗ trợ chủ động quản lý thời gian khám bệnh thông qua việc đăng ký lịch khám, đồng thời tạo môi trường để bác sĩ chia sẻ kiến thức, kinh nghiệm y khoa đến cộng đồng thông qua các bài viết, cẩm nang sức khỏe.

Tóm lại, hệ thống hướng tới mục tiêu:
- Chủ động hóa quy trình đặt lịch khám cho bệnh nhân và bác sĩ.
- Giảm tải, hạn chế tập trung đông người tại nơi khám bệnh.
- Cập nhật tin tức y tế, chia sẻ cẩm nang sức khỏe từ bác sĩ.
- Hỗ trợ bác sĩ quản lý lịch khám và truyền đạt kiến thức y khoa đến khách hàng.


## 2. Kiến trúc tổng thể
Dự án áp dụng kiến trúc microservices, mỗi chức năng chính được triển khai thành một dịch vụ độc lập, giao tiếp qua API Gateway. Điều này giúp hệ thống dễ mở rộng, bảo trì, triển khai linh hoạt và tăng khả năng chịu lỗi.

## 3. Luồng hoạt động cực kỳ chi tiết

3.1. Luồng đặt lịch khám bệnh
**Vai trò:** Bệnh nhân, bác sĩ, nhân viên
**Các bước:**
1. Bệnh nhân đăng nhập vào hệ thống qua giao diện Angular FE.
2. Chọn chức năng "Đặt lịch khám" và nhập thông tin: ngày, giờ, chuyên khoa, bác sĩ mong muốn.
3. FE gửi request đến API Gateway.
4. API Gateway chuyển tiếp đến Appointment Service.
5. Appointment Service kiểm tra lịch trống của bác sĩ, xác thực thông tin bệnh nhân qua Medical Record Service.
6. Nếu hợp lệ, Appointment Service tạo lịch hẹn mới, lưu vào database.
7. Appointment Service gửi thông báo xác nhận qua email/SMS (tích hợp Notification Service nếu có).
8. Bác sĩ và nhân viên nhận thông báo về lịch hẹn mới qua giao diện FE.
9. Bệnh nhân có thể xem, sửa, hủy lịch hẹn trên FE. Mỗi thao tác đều gửi request qua API Gateway đến Appointment Service để cập nhật trạng thái.
10. Lịch hẹn được đồng bộ với Medical Record Service để cập nhật lịch sử khám bệnh.

**Dữ liệu trao đổi:** Thông tin bệnh nhân, lịch hẹn, trạng thái xác nhận, thông báo.
**Dịch vụ liên quan:** FE, API Gateway, Appointment Service, Medical Record Service, Notification Service (nếu có).

---

3.2. Luồng quản lý hồ sơ bệnh án
**Vai trò:** Bác sĩ, bệnh nhân, nhân viên
**Các bước:**
1. Bác sĩ đăng nhập, truy cập chức năng "Hồ sơ bệnh án" trên FE.
2. FE gửi request đến API Gateway, chuyển tiếp đến Medical Record Service.
3. Medical Record Service xác thực quyền truy cập (chỉ bác sĩ hoặc bệnh nhân liên quan mới xem/sửa được).
4. Bác sĩ xem thông tin bệnh sử, kết quả xét nghiệm, hình ảnh, đơn thuốc.
5. Bác sĩ cập nhật thông tin khám, thêm ghi chú, upload tài liệu mới (hình ảnh, file PDF).
6. Medical Record Service lưu dữ liệu mới, đồng bộ với các dịch vụ liên quan (ví dụ: Prediction Service để phân tích dữ liệu mới).
7. Bệnh nhân có thể xem hồ sơ cá nhân, tải về tài liệu, nhưng không được sửa.
8. Nhân viên hỗ trợ cập nhật thông tin hành chính, bảo hiểm.

**Dữ liệu trao đổi:** Hồ sơ bệnh án, tài liệu, hình ảnh, quyền truy cập.
**Dịch vụ liên quan:** FE, API Gateway, Medical Record Service, Prediction Service.

---

3.3. Luồng tư vấn qua Chatbot
**Vai trò:** Bệnh nhân, chatbot AI
**Các bước:**
1. Bệnh nhân truy cập chức năng "Chatbot tư vấn" trên FE.
2. Nhập câu hỏi hoặc chọn chủ đề tư vấn (triệu chứng, quy trình khám, đặt lịch).
3. FE gửi request đến API Gateway, chuyển tiếp đến Chatbot Service.
4. Chatbot Service xử lý ngôn ngữ tự nhiên, truy vấn dữ liệu từ Medical Record Service (nếu cần cá nhân hóa).
5. Chatbot trả lời bệnh nhân, hướng dẫn quy trình, giải đáp thắc mắc, hoặc đề xuất đặt lịch khám.
6. Nếu cần, chatbot có thể chuyển tiếp cuộc hội thoại cho nhân viên hỗ trợ thực tế.
7. Lịch sử chat được lưu lại để cải thiện AI và phục vụ phân tích sau này.

**Dữ liệu trao đổi:** Nội dung chat, thông tin bệnh nhân, lịch sử hội thoại.
**Dịch vụ liên quan:** FE, API Gateway, Chatbot Service, Medical Record Service.

---

3.4. Luồng dự đoán bệnh bằng AI
**Vai trò:** Bác sĩ, Prediction Service
**Các bước:**
1. Bác sĩ truy cập hồ sơ bệnh nhân, chọn chức năng "Dự đoán nguy cơ bệnh".
2. FE gửi dữ liệu bệnh nhân (lịch sử khám, kết quả xét nghiệm, triệu chứng) đến API Gateway.
3. API Gateway chuyển tiếp đến Prediction Service.
4. Prediction Service xử lý dữ liệu, áp dụng mô hình AI/ML để phân tích nguy cơ bệnh.
5. Kết quả dự đoán (xác suất mắc bệnh, khuyến nghị) trả về cho bác sĩ qua FE.
6. Bác sĩ sử dụng kết quả để tư vấn, lên phác đồ điều trị, hoặc cảnh báo bệnh nhân.
7. Kết quả dự đoán được lưu vào Medical Record Service để theo dõi lâu dài.

**Dữ liệu trao đổi:** Dữ liệu bệnh nhân, kết quả dự đoán, khuyến nghị.
**Dịch vụ liên quan:** FE, API Gateway, Prediction Service, Medical Record Service.

---

3.5. Luồng quản lý kho vật tư, thuốc men
**Vai trò:** Nhân viên kho, bác sĩ, Product Inventory Service
**Các bước:**
1. Nhân viên kho đăng nhập, truy cập chức năng "Quản lý kho" trên FE.
2. FE gửi request đến API Gateway, chuyển tiếp đến Product Inventory Service.
3. Nhân viên kiểm tra tồn kho, nhập/xuất vật tư, cập nhật số lượng.
4. Khi bác sĩ kê đơn thuốc hoặc đặt lịch khám, hệ thống kiểm tra tồn kho tự động để đảm bảo đủ vật tư/thuốc.
5. Product Inventory Service cảnh báo khi số lượng vật tư/thuốc dưới ngưỡng tối thiểu.
6. Lịch sử xuất nhập tồn được lưu lại để phục vụ báo cáo và kiểm toán.

**Dữ liệu trao đổi:** Thông tin vật tư, thuốc men, số lượng tồn kho, lịch sử xuất nhập.
**Dịch vụ liên quan:** FE, API Gateway, Product Inventory Service.

---

3.6. Luồng báo cáo doanh thu, hiệu suất
**Vai trò:** Quản trị viên, Reporting Service
**Các bước:**
1. Quản trị viên đăng nhập, truy cập chức năng "Báo cáo" trên FE.
2. Chọn loại báo cáo: doanh thu, hiệu suất khám chữa bệnh, thống kê bệnh nhân, v.v.
3. FE gửi request đến API Gateway, chuyển tiếp đến Reporting Service.
4. Reporting Service tổng hợp dữ liệu từ các dịch vụ: Appointment, Medical Record, Product Inventory, v.v.
5. Xử lý, phân tích dữ liệu, xuất báo cáo theo tiêu chí thời gian, phòng ban, bác sĩ.
6. Kết quả báo cáo trả về FE dưới dạng bảng, biểu đồ, file xuất (PDF, Excel).
7. Quản trị viên sử dụng báo cáo để ra quyết định quản trị, tối ưu vận hành.

**Dữ liệu trao đổi:** Dữ liệu lịch hẹn, hồ sơ bệnh án, kho, doanh thu, thống kê.
**Dịch vụ liên quan:** FE, API Gateway, Reporting Service, các dịch vụ dữ liệu.

---

3.7. Luồng phân quyền, quản lý người dùng
**Vai trò:** Quản trị viên, Sys Service
**Các bước:**
1. Quản trị viên truy cập chức năng "Quản lý người dùng" trên FE.
2. FE gửi request đến API Gateway, chuyển tiếp đến Sys Service.
3. Quản trị viên tạo mới, sửa, xóa tài khoản người dùng, phân quyền theo vai trò (bác sĩ, bệnh nhân, nhân viên, quản trị viên).
4. Sys Service xác thực, lưu thông tin người dùng, phân quyền truy cập cho các dịch vụ khác.
5. Khi người dùng đăng nhập, hệ thống kiểm tra quyền truy cập để hiển thị chức năng phù hợp.
6. Lịch sử hoạt động của người dùng được lưu lại để phục vụ kiểm toán, bảo mật.

**Dữ liệu trao đổi:** Thông tin người dùng, quyền truy cập, lịch sử hoạt động.
**Dịch vụ liên quan:** FE, API Gateway, Sys Service, các dịch vụ khác.

---

## 4. Mô tả chi tiết các dịch vụ

## 3. Mô tả chi tiết các dịch vụ

Frontend (FE)
- Xây dựng bằng Angular, cung cấp giao diện cho các vai trò: quản trị viên, bác sĩ, nhân viên, bệnh nhân.
- Tích hợp các tính năng: đặt lịch, xem hồ sơ bệnh án, chat với chatbot, quản lý kho, xem báo cáo doanh thu.
- Responsive, dễ sử dụng trên nhiều thiết bị.

API Gateway
- Đóng vai trò trung gian, bảo mật và định tuyến các request từ frontend đến các dịch vụ backend.
- Hỗ trợ xác thực, phân quyền truy cập.

Eureka Server
- Quản lý đăng ký và phát hiện các microservice, giúp các dịch vụ tự động nhận biết nhau khi triển khai mới hoặc thay đổi.

Appointment Service
- Quản lý toàn bộ quy trình đặt lịch khám: tạo, sửa, hủy lịch hẹn.
- Tích hợp thông báo tự động cho bệnh nhân và bác sĩ.
- Hỗ trợ kiểm tra lịch trống, tránh trùng lịch.

Medical Record Service
- Lưu trữ, cập nhật và truy xuất hồ sơ bệnh án điện tử.
- Hỗ trợ upload tài liệu, hình ảnh liên quan đến bệnh nhân.
- Phân quyền truy cập theo vai trò (bác sĩ, bệnh nhân, nhân viên).

Chatbot Service
- Tích hợp AI chatbot hỗ trợ tư vấn sức khỏe, trả lời câu hỏi thường gặp, hướng dẫn quy trình khám chữa bệnh.
- Chatbot có thể học từ dữ liệu thực tế để cải thiện chất lượng trả lời.

Prediction Service
- Ứng dụng AI/ML để dự đoán nguy cơ bệnh dựa trên dữ liệu bệnh nhân.
- Hỗ trợ bác sĩ ra quyết định điều trị, cảnh báo sớm các nguy cơ.

Product Inventory Service
- Quản lý kho vật tư, thuốc men, thiết bị y tế.
- Theo dõi xuất nhập tồn, cảnh báo khi sắp hết hàng.
- Tích hợp với các dịch vụ khác để kiểm tra tồn kho khi đặt lịch hoặc kê đơn.

Reporting Service
- Tổng hợp dữ liệu hoạt động, doanh thu, hiệu suất khám chữa bệnh.
- Xuất báo cáo theo nhiều tiêu chí: thời gian, phòng ban, bác sĩ, loại dịch vụ.
- Hỗ trợ ra quyết định quản trị và tối ưu vận hành.

Sys Service
- Quản lý các chức năng hệ thống chung: người dùng, phân quyền, cấu hình hệ thống.

## 4. Công nghệ sử dụng
- Backend: Java (Spring Boot) cho các dịch vụ chính, Python cho AI/ML và chatbot.
- Frontend: Angular.
- Docker, Docker Compose để đóng gói và triển khai dịch vụ.
- Eureka Server cho service discovery.
- Kiến trúc Microservices.


## 5. Phân quyền và chức năng chi tiết của từng vai trò

### 5.1. Thành viên hệ thống (tất cả người dùng đã đăng ký)
- Đăng nhập, đăng xuất hệ thống
- Đăng ký tài khoản mới
- Thay đổi thông tin cá nhân, đổi mật khẩu
- Xem cẩm nang sức khỏe do bác sĩ chia sẻ
- Xem tin tức y tế mới nhất

### 5.2. Khách hàng (bệnh nhân)
- Đặt lịch khám bệnh (cho bản thân hoặc người thân): chọn bác sĩ, chuyên khoa, thời gian, nhập thông tin người khám
- Xem, sửa, hủy lịch khám đã đặt
- Xem kết quả khám bệnh, đơn thuốc, lịch sử khám
- Thanh toán dịch vụ khám bệnh trực tuyến
- Chat trực tiếp với bác sĩ qua hệ thống chat (trao đổi, hỏi đáp)
- Phản hồi, đánh giá về bác sĩ sau khi khám
- Quản lý thông tin cá nhân, cập nhật hồ sơ

### 5.3. Bác sĩ
- Đăng ký lịch khám, quản lý lịch làm việc cá nhân
- Xem danh sách bệnh nhân đã đặt lịch, chi tiết từng ca khám
- Cập nhật kết quả khám bệnh, nhập chẩn đoán, kê đơn thuốc cho bệnh nhân
- Viết, chia sẻ cẩm nang sức khỏe, bài viết chuyên môn lên hệ thống
- Chat trực tiếp với bệnh nhân qua hệ thống chat (trao đổi, tư vấn)
- Quản lý thông tin cá nhân, cập nhật hồ sơ bác sĩ

### 5.4. Nhân viên (Staff)
- Hỗ trợ khách hàng đăng ký, sửa, hủy lịch khám bệnh (thay mặt bệnh nhân thao tác trên hệ thống)
- Quản lý, cập nhật thông tin hành chính, bảo hiểm cho bệnh nhân
- Quản lý kho vật tư, thuốc men: nhập kho, xuất kho, kiểm kê số lượng, cập nhật tồn kho
- Hỗ trợ bác sĩ cập nhật hồ sơ bệnh án, nhập thông tin hành chính cho bệnh nhân
- Xem danh sách lịch khám, hỗ trợ điều phối bệnh nhân

### 5.5. Quản lý (quản trị viên)
- Quản lý tài khoản khách hàng: thêm, sửa, khóa/mở tài khoản
- Quản lý bác sĩ: thêm tài khoản, sửa thông tin, khóa/mở tài khoản
- Quản lý nhân viên: thêm, sửa, khóa/mở tài khoản
- Quản lý tin tức y tế, cẩm nang sức khỏe: đăng mới, cập nhật, xóa bài viết
- Xem thống kê số lượng đặt lịch khám theo tháng, doanh thu, hiệu suất hoạt động
- Quản lý phân quyền, cấu hình hệ thống

## 6. Điểm nổi bật
- Tích hợp AI/ML vào quy trình khám chữa bệnh.
- Chatbot thông minh hỗ trợ bệnh nhân 24/7.
- Quản lý dữ liệu tập trung, bảo mật cao.
- Kiến trúc microservices dễ mở rộng, bảo trì.
- Giao diện hiện đại, thân thiện, đa nền tảng.

## 7. Hướng dẫn sử dụng
1. Triển khai các dịch vụ bằng Docker Compose.
2. Truy cập giao diện web qua địa chỉ cấu hình.
3. Đăng nhập theo vai trò để sử dụng các chức năng tương ứng.
4. Tham khảo tài liệu chi tiết trong từng thư mục dịch vụ hoặc liên hệ nhóm phát triển để được hỗ trợ.

---

> Vui lòng xem chi tiết từng dịch vụ trong các thư mục tương ứng hoặc liên hệ nhóm phát triển để biết thêm thông tin.