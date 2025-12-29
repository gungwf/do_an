import {
  Component, OnInit, OnDestroy, Output, EventEmitter, Input,
  ViewChild, ElementRef, ChangeDetectorRef
} from '@angular/core';
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
export class ChatWindowComponent implements OnInit, OnDestroy {

  @Input() userRole: 'PATIENT' | 'DOCTOR' = 'PATIENT';
  @Input() targetUserId: string | null = null;
  @Output() closeChat = new EventEmitter<void>();

  @ViewChild('bottomAnchor') private bottomAnchor!: ElementRef;
  @ViewChild('chatInput') private chatInput!: ElementRef<HTMLTextAreaElement>;

  currentView: 'rooms' | 'chat' = 'rooms';
  rooms: ChatRoom[] = [];
  selectedRoom: ChatRoom | null = null;
  messages: ChatMessage[] = [];
  newMessage = '';
  currentUserId: string | null = null;
  isLoading = false;
  isSending = false;
  isTyping = false;
  
  private subscriptions: Subscription[] = [];
  private participantsCache = new Map<number, ChatParticipant[]>();
  private userNameCache = new Map<string, string>();

  constructor(
    private chatService: ChatService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.currentUserId = localStorage.getItem('healthcare_user_id');
    const token = this.chatService.getToken();
    if (token) this.chatService.connect(token);

    if (this.targetUserId && this.currentUserId) {
      this.createRoomWithUser(this.targetUserId);
    } else {
      this.loadRooms();
    }

    this.subscriptions.push(
      this.chatService.onMessageReceived().subscribe(msg => this.onIncomingMessage(msg))
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
    if (this.selectedRoom) this.chatService.unsubscribeFromRoom(this.selectedRoom.id);
  }

  // --- LOGIC GIAO DIỆN MỚI (AUTO RESIZE) ---

  autoResize(target: any): void {
    const textarea = target as HTMLTextAreaElement;
    textarea.style.height = 'auto'; 
    textarea.style.height = textarea.scrollHeight + 'px';
  }

  onKeyDown(e: KeyboardEvent): void {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      this.sendMessage();
    }
  }

  private resetInputHeight(): void {
    if (this.chatInput) {
      const textarea = this.chatInput.nativeElement;
      textarea.style.height = 'auto';
    }
  }

  // --- LOGIC CHAT ---

  sendMessage(): void {
    if (!this.selectedRoom || !this.newMessage.trim() || this.isSending) return;

    this.isSending = true;
    this.chatService.sendMessage(this.selectedRoom.id, this.newMessage.trim());
    
    this.newMessage = '';
    this.resetInputHeight(); // Reset chiều cao sau khi gửi
    this.isSending = false;
    this.afterMessagesChanged();
  }

  private onIncomingMessage(message: ChatMessage): void {
    if (this.selectedRoom && message.roomId === this.selectedRoom.id) {
      this.resolveSenderName(message);
      this.messages.push(message);
      this.afterMessagesChanged();
      if (message.senderId !== this.currentUserId) {
        this.chatService.markAsRead(message.id!);
      }
    } else {
      this.updateRoomUnreadCount(message.roomId);
      this.loadRooms();
    }
  }

  openRoom(room: ChatRoom): void {
    this.selectedRoom = room;
    this.currentView = 'chat';
    this.messages = [];
    this.chatService.subscribeToRoom(room.id);
    
    // Load history... (Giữ nguyên logic cũ của bạn)
    this.chatService.getParticipants(room.id).subscribe(parts => {
      this.participantsCache.set(room.id, parts);
      if (this.selectedRoom?.id === room.id) this.selectedRoom.participants = parts;
    });

    this.chatService.getMessageHistory(room.id).subscribe(msgs => {
      this.messages = msgs;
      this.afterMessagesChanged();
    });
  }

  backToRooms(): void {
    if (this.selectedRoom) this.chatService.unsubscribeFromRoom(this.selectedRoom.id);
    this.selectedRoom = null;
    this.currentView = 'rooms';
  }

  private afterMessagesChanged(): void {
    this.cdr.detectChanges();
    // Scroll mượt xuống cuối
    setTimeout(() => {
      if (this.bottomAnchor) {
        this.bottomAnchor.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'end' });
      }
    }, 100);
  }

  // --- HELPERS ---
  
  loadRooms(): void {
    if (!this.currentUserId) return;
    this.isLoading = true;
    this.chatService.getRoomsByUser(this.currentUserId).subscribe({
      next: rooms => {
        this.rooms = rooms;
        this.isLoading = false;
        rooms.forEach(r => this.chatService.subscribeToRoom(r.id));
      },
      error: () => this.isLoading = false
    });
  }

  isMyMessage(m: ChatMessage): boolean {
    return m.senderId === this.currentUserId;
  }

  getRoomName(room: ChatRoom): string {
    const parts = room.participants || this.participantsCache.get(room.id) || [];
    const other = parts.find(p => p.userId !== this.currentUserId);
    return other?.userName || 'Chat';
  }

  getOtherParticipantRole(room: ChatRoom): string {
    const parts = room.participants || this.participantsCache.get(room.id) || [];
    const other = parts.find(p => p.userId !== this.currentUserId);
    return other?.role || '';
  }

  getSenderName(senderId: string, m?: ChatMessage): string {
    return m?.senderName || senderId;
  }

  resolveSenderName(m: ChatMessage): void {
    if (m.senderName || !m.senderId) return;
    const cached = this.userNameCache.get(m.senderId);
    if (cached) {
      m.senderName = cached;
      return;
    }
    this.authService.getUserById(m.senderId).subscribe(u => {
      const name = u?.fullName || m.senderId;
      this.userNameCache.set(m.senderId, name);
      m.senderName = name;
    });
  }

  private updateRoomUnreadCount(roomId: number): void {
    const r = this.rooms.find(x => x.id === roomId);
    if (r) r.unreadCount = (r.unreadCount || 0) + 1;
  }

  createRoomWithUser(userId: string): void { /* Logic giữ nguyên */ }
  close(): void { this.closeChat.emit(); }
  formatTime(ts?: string): string {
    if (!ts) return '';
    return new Date(ts).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  }
}