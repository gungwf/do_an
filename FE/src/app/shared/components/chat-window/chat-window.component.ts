import { Component, OnInit, OnDestroy, Output, EventEmitter, Input, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService, ChatRoom, ChatMessage, ChatParticipant } from '../../../core/services/chat.service';
import { AuthService, UserDto } from '../../../core/services/auth';
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
  // Cache participants per room to avoid repeated network calls
  private participantsCache: Map<number, ChatParticipant[]> = new Map();
  // Track rooms for which a participants fetch is in-flight
  private participantsFetching: Set<number> = new Set();

  // cache userId -> fullName
  private userNameCache: Map<string, string> = new Map();

  constructor(private chatService: ChatService, private authService: AuthService) {}

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

          const setSenderNameFromParticipants = (participants?: ChatParticipant[]) => {
            if (!message.senderName && participants) {
              const p = participants.find(pp => pp.userId === message.senderId);
              if (p && p.userName) {
                message.senderName = p.userName;
              }
            }
          };

          // 1) try selectedRoom.participants
          setSenderNameFromParticipants(this.selectedRoom.participants);

          // 2) try cache
          if (!message.senderName) {
            const cached = this.participantsCache.get(message.roomId);
            if (cached) {
              setSenderNameFromParticipants(cached);
            }
          }

          // 3) if still missing, fetch participants for this room once
          if (!message.senderName) {
            this.chatService.getParticipants(message.roomId).subscribe({
              next: (participants) => {
                console.log('👥 Fetched participants for incoming message room', message.roomId, participants);
                this.participantsCache.set(message.roomId, participants);
                if (this.selectedRoom && this.selectedRoom.id === message.roomId) {
                  this.selectedRoom.participants = participants;
                }
                setSenderNameFromParticipants(participants);
                this.messages.push(message);
                this.shouldScrollToBottom = true;
                // Đánh dấu đã đọc
                if (message.id && message.senderId !== this.currentUserId) {
                  this.chatService.markAsRead(message.id);
                }
              },
              error: (err) => {
                console.error('Error fetching participants for message room', message.roomId, err);
                // still push message even if we couldn't resolve name
                this.messages.push(message);
                this.shouldScrollToBottom = true;
              }
            });

            return; // handled in async callback
          }

          // If senderName resolved synchronously, push message
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

    // Load participants for the room so we can show sender names
    this.chatService.getParticipants(room.id).subscribe({
      next: (participants) => {
        console.log('👥 Loaded participants for room', room.id, participants);
        // cache participants to avoid future calls
        this.participantsCache.set(room.id, participants);
        if (this.selectedRoom && this.selectedRoom.id === room.id) {
          this.selectedRoom.participants = participants;
          // If messages already loaded, map names
          if (this.messages && this.messages.length > 0) {
            this.mapSenderNames(this.messages, participants);
          }
        }
      },
      error: (err) => {
        console.error('Error loading participants for room', room.id, err);
      }
    });

    // Load message history
    this.chatService.getMessageHistory(room.id).subscribe({
      next: (messages) => {
        console.log('📜 Loaded message history:', messages);
        this.messages = messages;
        // If participants already available (or cached), map names; otherwise fetch participants
        const participants = this.selectedRoom?.participants || this.participantsCache.get(room.id);
        if (participants && participants.length > 0) {
          this.mapSenderNames(this.messages, participants);
          this.shouldScrollToBottom = true;
        } else {
          // fetch participants to map names
          this.chatService.getParticipants(room.id).subscribe({
            next: (participants) => {
              console.log('👥 Fetched participants for history mapping', room.id, participants);
              this.participantsCache.set(room.id, participants);
              if (this.selectedRoom && this.selectedRoom.id === room.id) {
                this.selectedRoom.participants = participants;
              }
              this.mapSenderNames(this.messages, participants);
              this.shouldScrollToBottom = true;
            },
            error: (err) => {
              console.error('Error fetching participants for history mapping', room.id, err);
              this.shouldScrollToBottom = true;
            }
          });
        }
      },
      error: (error) => {
        console.error('Error loading message history:', error);
      }
    });

    // Load message history nếu có API
    // this.loadMessageHistory(room.id);
  }

  /**
   * Map participant.userName onto messages' senderName when missing
   */
  private mapSenderNames(messages: ChatMessage[], participants: ChatParticipant[]): void {
    if (!messages || !participants) return;
    messages.forEach(m => {
      if (!m.senderName) {
        const p = participants.find(pp => pp.userId === m.senderId);
        if (p && p.userName) {
          m.senderName = p.userName;
          return;
        }

        // try cached userName
        const cachedName = this.userNameCache.get(m.senderId);
        if (cachedName) {
          m.senderName = cachedName;
          // also update participant if exists
          if (p) p.userName = cachedName;
          return;
        }

        // fallback: fetch user profile by id
        this.authService.getUserById(m.senderId).subscribe({
          next: (user: UserDto) => {
            const name = user?.fullName || user?.id || m.senderId;
            this.userNameCache.set(m.senderId, name);
            m.senderName = name;
            if (p) p.userName = name;
          },
          error: (err) => {
            console.error('Error fetching user profile for', m.senderId, err);
          }
        });
      }
    });
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

    // If websocket not connected, we still call service.sendMessage() which will queue the message
    if (!this.chatService.isConnectedSync()) {
      console.warn('WebSocket not connected when trying to send message. Message will be queued and connection will be attempted.');
      const token = this.chatService.getToken();
      if (token) {
        this.chatService.connect(token);
      } else {
        console.error('No auth token available; cannot connect WebSocket. Message will remain queued until login.');
      }
      // continue so service can queue the message
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
    
    // Fallback: lấy từ participants của selectedRoom
    let participants = this.selectedRoom?.participants;

    // Nếu selectedRoom không có participants, thử lấy từ cache
    if ((!participants || participants.length === 0) && this.selectedRoom) {
      const cached = this.participantsCache.get(this.selectedRoom.id);
      if (cached && cached.length > 0) {
        participants = cached;
        // assign vào selectedRoom để UI có thể dùng
        this.selectedRoom.participants = cached;
      } else if (this.selectedRoom && !this.participantsFetching.has(this.selectedRoom.id)) {
        // Khi chưa có cached, fetch participants một lần
        const roomId = this.selectedRoom.id;
        this.participantsFetching.add(roomId);
        this.chatService.getParticipants(roomId).subscribe({
          next: (parts) => {
            this.participantsCache.set(roomId, parts);
            if (this.selectedRoom && this.selectedRoom.id === roomId) {
              this.selectedRoom.participants = parts;
            }
            this.participantsFetching.delete(roomId);
          },
          error: () => {
            this.participantsFetching.delete(roomId);
          }
        });
      }

    }

    if (!participants || participants.length === 0) return senderId || 'Unknown';
    const participant = participants.find(p => p.userId === senderId);
    return participant?.userName || senderId;
  }

  /**
   * Lấy tên room (tên của người kia trong cuộc hội thoại 1-1)
   */
  getRoomName(room: ChatRoom): string {
    // Nếu đã có participants thì dùng luôn (và nếu thiếu userName, thử fetch từng user)
    if (room.participants && room.participants.length > 0) {
      const otherParticipant = room.participants.find(p => p.userId !== this.currentUserId);
      if (!otherParticipant) return 'Unknown';

      if (otherParticipant.userName) return otherParticipant.userName;

      // try cache
      const cached = this.userNameCache.get(otherParticipant.userId);
      if (cached) {
        otherParticipant.userName = cached;
        return cached;
      }

      // fallback: fetch user profile once
      this.authService.getUserById(otherParticipant.userId).subscribe({
        next: (user) => {
          const name = user?.fullName || user?.id || otherParticipant.userId;
          this.userNameCache.set(otherParticipant.userId, name);
          otherParticipant.userName = name;
        },
        error: (err) => {
          console.error('Error fetching user for room header', otherParticipant.userId, err);
        }
      });

      return otherParticipant.userId || '...';
    }

    // Thử lấy từ cache
    const cached = this.participantsCache.get(room.id);
    if (cached && cached.length > 0) {
      room.participants = cached;
      const otherParticipant = cached.find(p => p.userId !== this.currentUserId);
      return otherParticipant?.userName || otherParticipant?.userId || 'Unknown';
    }

    // Nếu chưa có, khởi tạo fetch (một lần) để cập nhật header khi về
    if (!this.participantsFetching.has(room.id)) {
      this.participantsFetching.add(room.id);
      this.chatService.getParticipants(room.id).subscribe({
        next: (parts) => {
          this.participantsCache.set(room.id, parts);
          // Nếu room đang là selectedRoom, sync participants để UI update
          if (this.selectedRoom && this.selectedRoom.id === room.id) {
            this.selectedRoom.participants = parts;
          }
          this.participantsFetching.delete(room.id);
        },
        error: () => {
          this.participantsFetching.delete(room.id);
        }
      });
    }

    // Trả về tạm userId để không block UI; sẽ cập nhật khi data về
    return '...';
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
