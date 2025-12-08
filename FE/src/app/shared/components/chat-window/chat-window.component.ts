import { Component, OnInit, OnDestroy, Output, EventEmitter, Input, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService, ChatRoom, ChatMessage, ChatParticipant } from '../../../core/services/chat.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-chat-window',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat-window.component.html',
  styleUrl: './chat-window.component.scss'
})
export class ChatWindowComponent implements OnInit, OnDestroy, AfterViewChecked {
  @Input() userRole: 'PATIENT' | 'DOCTOR' = 'PATIENT';
  @Input() targetUserId: string | null = null; // Nếu có, tự động tạo/mở room với user này
  @Output() closeChat = new EventEmitter<void>();
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  // State
  currentView: 'rooms' | 'chat' = 'rooms';
  rooms: ChatRoom[] = [];
  selectedRoom: ChatRoom | null = null;
  messages: ChatMessage[] = [];
  newMessage = '';
  
  currentUserId: string | null = null;
  isLoading = false;
  isSending = false;
  
  private subscriptions: Subscription[] = [];
  private shouldScrollToBottom = false;

  constructor(private chatService: ChatService) {}

  ngOnInit(): void {
    this.currentUserId = localStorage.getItem('healthcare_user_id');
    console.log('💬 Chat window initialized - currentUserId:', this.currentUserId);
    
    // Kết nối WebSocket nếu chưa
    const token = this.chatService.getToken();
    if (token) {
      console.log('🔐 Token found, connecting WebSocket...');
      this.chatService.connect(token);
    }

    // Nếu có targetUserId, tự động tạo/mở room với user đó
    if (this.targetUserId && this.currentUserId) {
      this.createRoomWithUser(this.targetUserId);
    } else {
      // Load danh sách rooms
      this.loadRooms();
    }

    // Lắng nghe tin nhắn mới
    const messageSub = this.chatService.onMessageReceived().subscribe(
      (message: ChatMessage) => {
        console.log('💬 Message received in window:', message);
        // Nếu tin nhắn thuộc room đang mở
        if (this.selectedRoom && message.roomId === this.selectedRoom.id) {
          console.log('✅ Adding message to current room');
          this.messages.push(message);
          this.shouldScrollToBottom = true;
          
          // Đánh dấu đã đọc
          if (message.id && message.senderId !== this.currentUserId) {
            this.chatService.markAsRead(message.id);
          }
        } else {
          // Cập nhật unread count cho room khác
          console.log('📥 Message for different room, reloading rooms...');
          this.loadRooms(); // Reload để thấy room mới
          this.updateRoomUnreadCount(message.roomId);
        }
      }
    );

    this.subscriptions.push(messageSub);
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    
    // Unsubscribe from current room
    if (this.selectedRoom) {
      this.chatService.unsubscribeFromRoom(this.selectedRoom.id);
    }
  }

  /**
   * Load danh sách rooms của user
   */
  loadRooms(): void {
    if (!this.currentUserId) return;

    this.isLoading = true;
    this.chatService.getRoomsByUser(this.currentUserId).subscribe({
      next: (rooms) => {
        console.log('📋 Loaded rooms:', rooms);
        this.rooms = rooms;
        this.isLoading = false;
        
        // Auto subscribe to all rooms to receive messages
        rooms.forEach(room => {
          this.chatService.subscribeToRoom(room.id);
        });
      },
      error: (error) => {
        console.error('Error loading rooms:', error);
        this.isLoading = false;
      }
    });
  }

  /**
   * Mở một room để chat
   */
  openRoom(room: ChatRoom): void {
    console.log('📂 Opening room:', room.id);
    this.selectedRoom = room;
    this.currentView = 'chat';
    this.messages = [];
    this.shouldScrollToBottom = true;

    // Subscribe to room messages
    console.log('🔔 Subscribing to room:', room.id);
    this.chatService.subscribeToRoom(room.id);

    // Load message history
    this.chatService.getMessageHistory(room.id).subscribe({
      next: (messages) => {
        console.log('📜 Loaded message history:', messages);
        this.messages = messages;
        this.shouldScrollToBottom = true;
      },
      error: (error) => {
        console.error('Error loading message history:', error);
      }
    });

    // Load message history nếu có API
    // this.loadMessageHistory(room.id);
  }

  /**
   * Tạo room mới với bác sĩ (từ patient) hoặc với bệnh nhân (từ doctor)
   */
  createRoomWithUser(userId: string): void {
    if (!this.currentUserId) return;

    this.isLoading = true;
    // Đảm bảo patient luôn là userA, doctor là userB
    const userA = this.userRole === 'PATIENT' ? this.currentUserId : userId;
    const userB = this.userRole === 'PATIENT' ? userId : this.currentUserId;
    this.chatService.createOrGetOneToOneRoom(userA, userB).subscribe({
      next: (room) => {
        this.isLoading = false;
        this.openRoom(room);
      },
      error: (error) => {
        console.error('Error creating room:', error);
        this.isLoading = false;
        // Fallback: load rooms list
        this.loadRooms();
      }
    });
  }

  /**
   * Tạo room mới với bác sĩ (từ patient) - deprecated, use createRoomWithUser
   */
  createRoomWithDoctor(doctorId: string): void {
    this.createRoomWithUser(doctorId);
  }

  /**
   * Quay lại danh sách rooms
   */
  backToRooms(): void {
    if (this.selectedRoom) {
      this.chatService.unsubscribeFromRoom(this.selectedRoom.id);
    }
    this.currentView = 'rooms';
    this.selectedRoom = null;
    this.messages = [];
  }

  /**
   * Gửi tin nhắn
   */
  sendMessage(): void {
    if (!this.selectedRoom || !this.newMessage.trim() || this.isSending) {
      return;
    }

    this.isSending = true;
    
    console.log('📤 Sending message to room:', this.selectedRoom.id);

    // Gửi qua WebSocket (không tạo temp message để tránh duplicate)
    this.chatService.sendMessage(this.selectedRoom.id, this.newMessage.trim());
    
    this.newMessage = '';
    this.isSending = false;
  }

  /**
   * Xử lý khi nhấn Enter
   */
  onKeyPress(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  /**
   * Đóng cửa sổ chat
   */
  close(): void {
    this.closeChat.emit();
  }

  /**
   * Scroll xuống cuối danh sách tin nhắn
   */
  private scrollToBottom(): void {
    try {
      if (this.messagesContainer) {
        this.messagesContainer.nativeElement.scrollTop = 
          this.messagesContainer.nativeElement.scrollHeight;
      }
    } catch (err) {
      console.error('Error scrolling to bottom:', err);
    }
  }

  /**
   * Cập nhật unread count cho room
   */
  private updateRoomUnreadCount(roomId: number): void {
    const room = this.rooms.find(r => r.id === roomId);
    if (room) {
      room.unreadCount = (room.unreadCount || 0) + 1;
    }
  }

  /**
   * Load lịch sử tin nhắn
   */
  private loadMessageHistory(roomId: number): void {
    this.chatService.getMessageHistory(roomId).subscribe({
      next: (messages) => {
        this.messages = messages;
        this.shouldScrollToBottom = true;
      },
      error: (error) => {
        console.error('Error loading message history:', error);
      }
    });
  }

  /**
   * Format timestamp
   */
  formatTime(timestamp: string | undefined): string {
    if (!timestamp) return '';
    
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    
    if (minutes < 1) return 'Vừa xong';
    if (minutes < 60) return `${minutes} phút trước`;
    
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours} giờ trước`;
    
    return date.toLocaleDateString('vi-VN', { 
      day: '2-digit', 
      month: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  /**
   * Kiểm tra xem tin nhắn có phải của mình không
   */
  isMyMessage(message: ChatMessage): boolean {
    return message.senderId === this.currentUserId;
  }

  /**
   * Lấy tên người gửi từ message hoặc room participants
   */
  getSenderName(senderId: string, message?: ChatMessage): string {
    // Ưu tiên lấy từ message.senderName nếu có
    if (message?.senderName) {
      return message.senderName;
    }
    
    // Fallback: lấy từ participants
    if (!this.selectedRoom?.participants) return 'Unknown';
    
    const participant = this.selectedRoom.participants.find(p => p.userId === senderId);
    return participant?.userName || senderId;
  }

  /**
   * Lấy tên room (tên của người kia trong cuộc hội thoại 1-1)
   */
  getRoomName(room: ChatRoom): string {
    if (!room.participants || room.participants.length === 0) {
      return 'Unknown';
    }

    // Tìm participant khác với current user
    const otherParticipant = room.participants.find(p => p.userId !== this.currentUserId);
    return otherParticipant?.userName || otherParticipant?.userId || 'Unknown';
  }

  /**
   * Lấy role của người kia trong room
   */
  getOtherParticipantRole(room: ChatRoom): string {
    if (!room.participants) return '';
    
    const otherParticipant = room.participants.find(p => p.userId !== this.currentUserId);
    return otherParticipant?.role || '';
  }
}
