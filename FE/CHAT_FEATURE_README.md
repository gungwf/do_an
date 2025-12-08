# Hướng Dẫn Sử Dụng Chức Năng Chat Realtime

## Tổng Quan

Chức năng chat realtime cho phép bệnh nhân và bác sĩ giao tiếp trực tiếp thông qua WebSocket. Bong bóng chat sẽ xuất hiện ở góc dưới bên phải màn hình.

## Các Thành Phần Đã Tạo

### 1. **ChatService** (`core/services/chat.service.ts`)
- Quản lý kết nối WebSocket với backend
- Gửi/nhận tin nhắn realtime
- Quản lý danh sách rooms và participants
- API endpoints:
  - `POST /api/chat/rooms/one2one` - Tạo hoặc lấy room 1-1
  - `GET /api/chat/rooms/by-user/{userId}` - Lấy danh sách rooms
  - `GET /api/chat/doctors/online` - Lấy danh sách bác sĩ online

### 2. **ChatBubbleComponent** (`shared/components/chat-bubble/`)
- Bong bóng chat nổi ở góc dưới bên phải
- Hiển thị số tin nhắn chưa đọc
- Animation khi có tin nhắn mới
- Tự động responsive trên mobile

### 3. **ChatWindowComponent** (`shared/components/chat-window/`)
- Cửa sổ chat chính
- 2 views:
  - **Rooms List**: Danh sách các cuộc hội thoại
  - **Chat View**: Màn hình chat với tin nhắn
- Tính năng:
  - Gửi/nhận tin nhắn realtime
  - Hiển thị trạng thái đã gửi/đã đọc
  - Scroll tự động đến tin nhắn mới
  - Format thời gian thông minh

## Cách Sử Dụng

### Cho Bệnh Nhân

1. **Đăng nhập** vào hệ thống
2. Vào trang **"Đặt lịch hẹn"**
3. Chọn bác sĩ từ danh sách
4. Nhấn nút **"Liên hệ ngay"** (màu xanh lá)
5. Bong bóng chat sẽ tự động mở và tạo room với bác sĩ
6. Bắt đầu chat!

**Hoặc:**
- Nhấn vào bong bóng chat ở góc dưới phải
- Xem danh sách các cuộc hội thoại hiện có
- Chọn cuộc hội thoại để tiếp tục chat

### Cho Bác Sĩ

1. **Đăng nhập** vào hệ thống với role Doctor
2. Bong bóng chat sẽ xuất hiện tự động
3. Khi bệnh nhân nhắn tin:
   - Bong bóng chat sẽ hiển thị số tin nhắn chưa đọc
   - Animation pulse để thu hút sự chú ý
4. Nhấn vào bong bóng để xem và trả lời tin nhắn

## Cấu Hình Backend

### WebSocket Endpoint
- **URL**: `http://localhost:8080/ws`
- **Protocol**: STOMP over SockJS
- **Authentication**: JWT token trong header

### API Endpoints
```
POST   /api/chat/rooms/one2one?userA={userId1}&userB={userId2}
GET    /api/chat/rooms/by-user/{userId}
GET    /api/chat/rooms/{roomId}/participants
GET    /api/chat/doctors/online
```

### WebSocket Topics
- **Subscribe**: `/topic/chat.{roomId}` - Nhận tin nhắn của room
- **Subscribe**: `/user/{userId}/queue/notifications` - Nhận thông báo cá nhân
- **Send**: `/app/chat.send` - Gửi tin nhắn
- **Send**: `/app/chat.delivered` - Đánh dấu đã nhận
- **Send**: `/app/chat.read` - Đánh dấu đã đọc

## Cấu Trúc Dữ Liệu

### ChatMessage
```typescript
{
  id?: number;
  roomId: number;
  senderId: string;
  senderName?: string;
  content: string;
  timestamp?: string;
  delivered?: boolean;
  read?: boolean;
}
```

### ChatRoom
```typescript
{
  id: number;
  roomType: string;
  createdAt?: string;
  participants?: ChatParticipant[];
  lastMessage?: ChatMessage;
  unreadCount?: number;
}
```

### ChatParticipant
```typescript
{
  id: number;
  roomId: number;
  userId: string;
  role: string; // "PATIENT" hoặc "DOCTOR"
  joinedAt?: string;
  userName?: string;
}
```

## Tính Năng Đã Implement

✅ Kết nối WebSocket tự động khi đăng nhập
✅ Ngắt kết nối khi đăng xuất
✅ Bong bóng chat nổi responsive
✅ Hiển thị số tin nhắn chưa đọc
✅ Animation khi có tin nhắn mới
✅ Tạo room 1-1 tự động
✅ Gửi/nhận tin nhắn realtime
✅ Hiển thị trạng thái tin nhắn (đã gửi/đã đọc)
✅ Scroll tự động đến tin nhắn mới
✅ Format thời gian thông minh
✅ Tích hợp với patient layout
✅ Tích hợp với doctor layout
✅ Nút "Liên hệ ngay" trong danh sách bác sĩ

## Styling

Các component sử dụng:
- **Bootstrap 5** cho base styling
- **Bootstrap Icons** cho icons
- **Custom SCSS** cho animations và layout
- **Gradient colors**: `#667eea` đến `#764ba2`

### Mobile Responsive
- Chat window chiếm full screen trên mobile
- Chat bubble nhỏ hơn trên mobile
- Touch-friendly buttons và interactions

## Troubleshooting

### WebSocket không kết nối
- Kiểm tra backend đang chạy trên port 8080
- Kiểm tra JWT token còn hợp lệ
- Xem console log để debug

### Không nhận được tin nhắn
- Kiểm tra đã subscribe vào đúng room chưa
- Xem Network tab để kiểm tra WebSocket frames
- Kiểm tra backend có publish tin nhắn đúng topic không

### Lỗi CORS
- Đảm bảo backend config CORS cho WebSocket
- Thêm origin `http://localhost:4200` vào whitelist

## TODO / Cải Tiến Tương Lai

- [ ] Lưu lịch sử tin nhắn vào database
- [ ] API lấy lịch sử tin nhắn cũ
- [ ] Upload file/hình ảnh trong chat
- [ ] Typing indicator (đang nhập...)
- [ ] Online/offline status cho users
- [ ] Push notifications khi có tin nhắn mới
- [ ] Group chat (nhiều người)
- [ ] Tìm kiếm trong tin nhắn
- [ ] Xóa tin nhắn
- [ ] Edit tin nhắn đã gửi

## Cấu Hình Cần Thiết

### Backend (Spring Boot)
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    // Xem file ChatController.java, ChatRoomController.java đã có
}
```

### Frontend (Angular)
```bash
# Đã cài đặt
npm install @stomp/stompjs sockjs-client
```

## Liên Hệ & Hỗ Trợ

Nếu có vấn đề, vui lòng kiểm tra:
1. Console log của browser
2. Network tab (WebSocket frames)
3. Backend logs
4. Database records cho rooms và messages
