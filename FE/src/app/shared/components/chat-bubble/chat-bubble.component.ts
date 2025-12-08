import { Component, OnInit, OnDestroy, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChatService, ChatRoom, ChatMessage, ChatNotification } from '../../../core/services/chat.service';
import { ChatWindowComponent } from '../chat-window/chat-window.component';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-chat-bubble',
  standalone: true,
  imports: [CommonModule, ChatWindowComponent],
  templateUrl: './chat-bubble.component.html',
  styleUrl: './chat-bubble.component.scss'
})
export class ChatBubbleComponent implements OnInit, OnDestroy {
  @Input() userRole: 'PATIENT' | 'DOCTOR' = 'PATIENT';
  
  isOpen = false;
  hasUnread = false;
  unreadCount = 0;
  targetUserId: string | null = null; // ID của user cần chat (nếu mở từ nút "Liên hệ ngay")
  
  private subscriptions: Subscription[] = [];

  constructor(private chatService: ChatService) {}

  ngOnInit(): void {
    // Lắng nghe thông báo mới
    const notificationSub = this.chatService.onNotificationReceived().subscribe(
      (notification: ChatNotification) => {
        this.hasUnread = true;
        this.unreadCount++;
        
        // Hiển thị notification nếu cần
        console.log('New chat notification:', notification);
      }
    );

    this.subscriptions.push(notificationSub);

    // Lắng nghe tin nhắn mới
    const messageSub = this.chatService.onMessageReceived().subscribe(
      (message: ChatMessage) => {
        // Chỉ tăng unread nếu không phải tin nhắn của mình
        const currentUserId = localStorage.getItem('healthcare_user_id');
        if (message.senderId !== currentUserId) {
          this.hasUnread = true;
          this.unreadCount++;
        }
      }
    );

    this.subscriptions.push(messageSub);

    // Lắng nghe sự kiện mở chat từ các component khác
    const openChatSub = this.chatService.onOpenChatWith().subscribe(
      (userId: string) => {
        this.targetUserId = userId;
        this.isOpen = true;
        this.hasUnread = false;
        this.unreadCount = 0;
      }
    );

    this.subscriptions.push(openChatSub);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  toggleChat(): void {
    this.isOpen = !this.isOpen;
    
    if (this.isOpen) {
      this.hasUnread = false;
      this.unreadCount = 0;
    }
  }
}
