import {
  Component,
  OnInit,
  OnDestroy,
  Output,
  EventEmitter,
  Input,
  ViewChild,
  ElementRef,
  ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ChatService,
  ChatRoom,
  ChatMessage,
  ChatParticipant
} from '../../../core/services/chat.service';
import { AuthService, UserDto } from '../../../core/services/auth';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-chat-window',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat-window.component.html',
  styleUrl: './chat-window.component.scss'
})
export class ChatWindowComponent implements OnInit, OnDestroy {

  @Input() userRole: 'PATIENT' | 'DOCTOR' = 'PATIENT';
  @Input() targetUserId: string | null = null;
  @Output() closeChat = new EventEmitter<void>();

  /** 🔥 Anchor để scroll */
  @ViewChild('bottomAnchor')
  private bottomAnchor!: ElementRef<HTMLDivElement>;

  currentView: 'rooms' | 'chat' = 'rooms';
  rooms: ChatRoom[] = [];
  selectedRoom: ChatRoom | null = null;
  messages: ChatMessage[] = [];
  newMessage = '';

  currentUserId: string | null = null;
  isLoading = false;
  isSending = false;
  isTyping = false;

  private typingTimeout: any;
  private subscriptions: Subscription[] = [];

  /** cache */
  private participantsCache = new Map<number, ChatParticipant[]>();
  private userNameCache = new Map<string, string>();

  constructor(
    private chatService: ChatService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  // ================= INIT =================

  ngOnInit(): void {
    this.currentUserId = localStorage.getItem('healthcare_user_id');

    const token = this.chatService.getToken();
    if (token) {
      this.chatService.connect(token);
    }

    if (this.targetUserId && this.currentUserId) {
      this.createRoomWithUser(this.targetUserId);
    } else {
      this.loadRooms();
    }

    this.subscriptions.push(
      this.chatService.onMessageReceived().subscribe(msg => {
        this.onIncomingMessage(msg);
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
    if (this.selectedRoom) {
      this.chatService.unsubscribeFromRoom(this.selectedRoom.id);
    }
  }

  // ================= MESSAGE =================

  private onIncomingMessage(message: ChatMessage): void {
    if (this.selectedRoom && message.roomId === this.selectedRoom.id) {
      this.resolveSenderName(message);
      this.messages.push(message);
      this.afterMessagesChanged();

      if (message.id && message.senderId !== this.currentUserId) {
        this.chatService.markAsRead(message.id);
      }
    } else {
      this.updateRoomUnreadCount(message.roomId);
      this.loadRooms();
    }
  }

  // ================= ROOMS =================

  loadRooms(): void {
    if (!this.currentUserId) return;

    this.isLoading = true;
    this.chatService.getRoomsByUser(this.currentUserId).subscribe({
      next: rooms => {
        this.rooms = rooms;
        this.isLoading = false;
        rooms.forEach(r => this.chatService.subscribeToRoom(r.id));
      },
      error: () => (this.isLoading = false)
    });
  }

  openRoom(room: ChatRoom): void {
    this.selectedRoom = room;
    this.currentView = 'chat';
    this.messages = [];

    this.chatService.subscribeToRoom(room.id);

    // participants
    this.chatService.getParticipants(room.id).subscribe(parts => {
      this.participantsCache.set(room.id, parts);
      if (this.selectedRoom?.id === room.id) {
        this.selectedRoom.participants = parts;
      }
    });

    // local cache
    const localKey = `chat_history_room_${room.id}`;
    const local = localStorage.getItem(localKey);
    if (local) {
      try {
        this.messages = JSON.parse(local);
        this.afterMessagesChanged();
      } catch {}
    }

    // api history
    this.chatService.getMessageHistory(room.id).subscribe(msgs => {
      this.messages = msgs;
      localStorage.setItem(localKey, JSON.stringify(msgs));
      this.afterMessagesChanged();
    });
  }

  backToRooms(): void {
    if (this.selectedRoom) {
      this.chatService.unsubscribeFromRoom(this.selectedRoom.id);
    }
    this.selectedRoom = null;
    this.messages = [];
    this.currentView = 'rooms';
  }

  // ================= SEND =================

  sendMessage(): void {
    if (!this.selectedRoom || !this.newMessage.trim() || this.isSending) return;

    this.isSending = true;
    this.chatService.sendMessage(
      this.selectedRoom.id,
      this.newMessage.trim()
    );
    this.newMessage = '';
    this.isSending = false;

    this.afterMessagesChanged();
  }

  onKeyPress(e: KeyboardEvent): void {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      this.sendMessage();
    }
  }

  /** 🔥 HÀM BỊ THIẾU – TEMPLATE ĐANG GỌI */
  onInputChange(): void {
    if (this.typingTimeout) clearTimeout(this.typingTimeout);
    this.isTyping = true;
    this.typingTimeout = setTimeout(() => {
      this.isTyping = false;
    }, 1500);
  }

  // ================= SCROLL – TRỌNG TÂM =================

  private afterMessagesChanged(): void {
    this.cdr.detectChanges();

    requestAnimationFrame(() => {
      if (this.bottomAnchor) {
        this.bottomAnchor.nativeElement.scrollIntoView({
          behavior: 'auto',
          block: 'end'
        });
      }
    });
  }

  // ================= TEMPLATE HELPERS =================

  isMyMessage(m: ChatMessage): boolean {
    return m.senderId === this.currentUserId;
  }

  formatTime(ts?: string): string {
    if (!ts) return '';
    const d = new Date(ts);
    return d.toLocaleTimeString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  /** 🔥 BỊ THIẾU → ĐÃ BỔ SUNG */
  getRoomName(room: ChatRoom): string {
    const participants =
      room.participants || this.participantsCache.get(room.id) || [];
    const other = participants.find(p => p.userId !== this.currentUserId);
    return other?.userName || other?.userId || '...';
  }

  /** 🔥 BỊ THIẾU → ĐÃ BỔ SUNG */
  getOtherParticipantRole(room: ChatRoom): string {
    const participants =
      room.participants || this.participantsCache.get(room.id) || [];
    const other = participants.find(p => p.userId !== this.currentUserId);
    return other?.role || '';
  }

  getSenderName(senderId: string, m?: ChatMessage): string {
    return m?.senderName || senderId;
  }

  private resolveSenderName(m: ChatMessage): void {
    if (m.senderName) return;

    const cached = this.userNameCache.get(m.senderId);
    if (cached) {
      m.senderName = cached;
      return;
    }

    this.authService.getUserById(m.senderId).subscribe((u: UserDto) => {
      const name = u?.fullName || m.senderId;
      this.userNameCache.set(m.senderId, name);
      m.senderName = name;
      this.cdr.detectChanges();
    });
  }

  private updateRoomUnreadCount(roomId: number): void {
    const r = this.rooms.find(x => x.id === roomId);
    if (r) r.unreadCount = (r.unreadCount || 0) + 1;
  }

  createRoomWithUser(userId: string): void {
    if (!this.currentUserId) return;

    const userA = this.userRole === 'PATIENT' ? this.currentUserId : userId;
    const userB = this.userRole === 'PATIENT' ? userId : this.currentUserId;

    this.chatService.createOrGetOneToOneRoom(userA, userB).subscribe(room => {
      this.openRoom(room);
    });
  }

  close(): void {
    this.closeChat.emit();
  }
}
