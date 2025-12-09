import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject, Subject } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';

export interface ChatMessage {
  id?: number;
  roomId: number;
  senderId: string;
  senderName?: string;
  content: string;
  createdAt?: string; // Backend uses createdAt, not timestamp
  delivered?: boolean;
  read?: boolean;
}

export interface ChatRoom {
  id: number;
  roomType: string;
  createdAt?: string;
  participants?: ChatParticipant[];
  lastMessage?: ChatMessage;
  unreadCount?: number;
}

export interface ChatParticipant {
  id: number;
  roomId: number;
  userId: string;
  role: string;
  joinedAt?: string;
  userName?: string;
}

export interface ChatNotification {
  roomId: number;
  senderId: string;
  senderName: string;
  message: string;
  timestamp: string;
}

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private apiUrl = 'http://localhost:8080/api/chat';
  private wsUrl = 'http://localhost:8080/ws-chat';
  
  private stompClient: Client | null = null;
  private connected$ = new BehaviorSubject<boolean>(false);
  
  // Subject để phát các tin nhắn mới
  private messageReceived$ = new Subject<ChatMessage>();
  private notificationReceived$ = new Subject<ChatNotification>();
  
  // Lưu trữ các subscription theo roomId
  private roomSubscriptions = new Map<number, any>();
  
  // Lưu trữ các rooms và messages
  private chatRooms$ = new BehaviorSubject<ChatRoom[]>([]);
  private activeRoom$ = new BehaviorSubject<ChatRoom | null>(null);

  constructor(private http: HttpClient) {}

  /**
   * Kết nối WebSocket với JWT token
   */
  connect(token: string): void {
    if (this.stompClient?.connected) {
      console.log('WebSocket already connected');
      return;
    }

    const userId = this.getCurrentUserId();
    console.log('🔌 Connecting WebSocket - userId from localStorage:', userId);
    console.log('🔑 Token (first 20 chars):', token.substring(0, 20) + '...');

    this.stompClient = new Client({
      brokerURL: this.wsUrl.replace('http', 'ws') + '?access_token=' + token,
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      debug: (str) => {
        console.log('STOMP: ' + str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.stompClient.onConnect = (frame) => {
      console.log('✅ Connected to WebSocket:', frame);
      const userId = this.getCurrentUserId();
      console.log('👤 Current userId after connect:', userId);
      this.connected$.next(true);
      
      // Subscribe to user-specific notifications
      if (userId) {
        this.subscribeToUserNotifications(userId);
      }
    };

    this.stompClient.onStompError = (frame) => {
      console.error('STOMP error:', frame);
      this.connected$.next(false);
    };

    this.stompClient.onWebSocketClose = () => {
      console.log('WebSocket connection closed');
      this.connected$.next(false);
    };

    this.stompClient.activate();
  }

  /**
   * Ngắt kết nối WebSocket
   */
  disconnect(): void {
    if (this.stompClient?.connected) {
      this.stompClient.deactivate();
      this.connected$.next(false);
      this.roomSubscriptions.clear();
    }
  }

  /**
   * Kiểm tra trạng thái kết nối
   */
  isConnected(): Observable<boolean> {
    return this.connected$.asObservable();
  }

  /**
   * Subscribe vào một room để nhận tin nhắn
   */
  subscribeToRoom(roomId: number): void {
    if (!this.stompClient?.connected) {
      console.error('WebSocket not connected');
      return;
    }

    // Nếu đã subscribe rồi thì không subscribe lại
    if (this.roomSubscriptions.has(roomId)) {
      return;
    }

    console.log('🔔 Subscribing to /topic/chat.' + roomId);
    
    const subscription = this.stompClient.subscribe(
      `/topic/chat.${roomId}`,
      (message: IMessage) => {
        const chatMessage: ChatMessage = JSON.parse(message.body);
        console.log('📨 Message received from WebSocket:', chatMessage);
        this.messageReceived$.next(chatMessage);
      }
    );

    this.roomSubscriptions.set(roomId, subscription);
  }

  /**
   * Unsubscribe khỏi một room
   */
  unsubscribeFromRoom(roomId: number): void {
    const subscription = this.roomSubscriptions.get(roomId);
    if (subscription) {
      subscription.unsubscribe();
      this.roomSubscriptions.delete(roomId);
    }
  }

  /**
   * Subscribe vào notifications của user
   */
  private subscribeToUserNotifications(userId: string): void {
    if (!this.stompClient?.connected) {
      return;
    }

    this.stompClient.subscribe(
      `/user/${userId}/queue/notifications`,
      (message: IMessage) => {
        const notification: ChatNotification = JSON.parse(message.body);
        console.log('Notification received:', notification);
        this.notificationReceived$.next(notification);
      }
    );
  }

  /**
   * Gửi tin nhắn
   */
  sendMessage(roomId: number, content: string): void {
    if (!this.stompClient?.connected) {
      console.error('WebSocket not connected');
      return;
    }

    const currentUserId = this.getCurrentUserId();
    console.log('📤 Sending message - userId:', currentUserId, 'roomId:', roomId);

    const chatMessage: ChatMessage = {
      roomId,
      senderId: currentUserId || '',
      content,
      createdAt: new Date().toISOString()
    };

    console.log('📤 Message payload:', JSON.stringify(chatMessage));

    this.stompClient.publish({
      destination: '/app/chat.send',
      body: JSON.stringify(chatMessage)
    });
  }

  /**
   * Đánh dấu tin nhắn đã nhận
   */
  markAsDelivered(messageId: number): void {
    if (!this.stompClient?.connected) {
      return;
    }

    this.stompClient.publish({
      destination: '/app/chat.delivered',
      body: JSON.stringify({ messageId })
    });
  }

  /**
   * Đánh dấu tin nhắn đã đọc
   */
  markAsRead(messageId: number): void {
    if (!this.stompClient?.connected) {
      return;
    }

    this.stompClient.publish({
      destination: '/app/chat.read',
      body: JSON.stringify({ messageId })
    });
  }

  /**
   * Lấy observable cho tin nhắn mới
   */
  onMessageReceived(): Observable<ChatMessage> {
    return this.messageReceived$.asObservable();
  }

  /**
   * Lấy observable cho thông báo mới
   */
  onNotificationReceived(): Observable<ChatNotification> {
    return this.notificationReceived$.asObservable();
  }

  // ========== HTTP API Methods ==========

  /**
   * Tạo hoặc lấy room 1-1 giữa bệnh nhân và bác sĩ
   */
  createOrGetOneToOneRoom(userA: string, userB: string): Observable<ChatRoom> {
    return this.http.post<ChatRoom>(`${this.apiUrl}/rooms/one2one?userA=${userA}&userB=${userB}`, {});
  }

  /**
   * Lấy danh sách rooms của user
   */
  getRoomsByUser(userId: string): Observable<ChatRoom[]> {
    return this.http.get<ChatRoom[]>(`${this.apiUrl}/rooms/by-user/${userId}`);
  }

  /**
   * Lấy danh sách participants của room
   */
  getParticipants(roomId: number): Observable<ChatParticipant[]> {
    return this.http.get<ChatParticipant[]>(`${this.apiUrl}/rooms/${roomId}/participants`);
  }

  /**
   * Lấy lịch sử tin nhắn của room
   */
  getMessageHistory(roomId: number, page: number = 0, size: number = 50): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.apiUrl}/rooms/${roomId}/messages?page=${page}&size=${size}`);
  }

  /**
   * Lấy danh sách bác sĩ đang online
   */
  getOnlineDoctors(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/doctors/online`);
  }

  // ========== Helper Methods ==========

  /**
   * Lấy userId hiện tại từ localStorage
   */
  private getCurrentUserId(): string | null {
    return localStorage.getItem('healthcare_user_id');
  }

  /**
   * Lấy token hiện tại từ localStorage
   */
  getToken(): string | null {
    return localStorage.getItem('healthcare_token');
  }

  /**
   * Set active room
   */
  setActiveRoom(room: ChatRoom | null): void {
    this.activeRoom$.next(room);
  }

  /**
   * Get active room observable
   */
  getActiveRoom(): Observable<ChatRoom | null> {
    return this.activeRoom$.asObservable();
  }

  /**
   * Get chat rooms observable
   */
  getChatRooms(): Observable<ChatRoom[]> {
    return this.chatRooms$.asObservable();
  }

  /**
   * Update chat rooms list
   */
  updateChatRooms(rooms: ChatRoom[]): void {
    this.chatRooms$.next(rooms);
  }

  /**
   * Trigger to open chat with specific user (for external components)
   */
  private openChatWith$ = new Subject<string>();

  triggerOpenChatWith(userId: string): void {
    this.openChatWith$.next(userId);
  }

  onOpenChatWith(): Observable<string> {
    return this.openChatWith$.asObservable();
  }
}
