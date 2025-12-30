import { Component, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AiChatService } from '../../../core/services/ai-chat.service';

interface IMessage {
  role: 'user' | 'assistant' | 'system';
  text: string;
}

@Component({
  selector: 'app-ai-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-chat.component.html',
  styleUrl: './ai-chat.component.scss'
})
export class AiChatComponent {
  isOpen = false;
  isSending = false;
  newMessage = '';
  sessionId: string | null = null;
  messages: IMessage[] = [];

  @ViewChild('body') private chatBody!: ElementRef;
  @ViewChild('chatInput') private chatInput!: ElementRef<HTMLTextAreaElement>;

  constructor(private aiService: AiChatService) {
    this.sessionId = localStorage.getItem('ai_chat_session') || this.generateSessionId();
    localStorage.setItem('ai_chat_session', this.sessionId);
  }

  toggle(): void {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      this.scrollToBottom();
    }
  }

  // --- LOGIC AUTO RESIZE ---
  autoResize(target: any): void {
    const textarea = target as HTMLTextAreaElement;
    textarea.style.height = 'auto'; // Reset để tính lại
    textarea.style.height = textarea.scrollHeight + 'px';
  }

  private focusInput(): void {
    setTimeout(() => {
      if (this.chatInput) {
        this.chatInput.nativeElement.focus();
      }
    }, 10); // Delay nhỏ để đảm bảo DOM đã sẵn sàng
  }

  onKeyDown(e: KeyboardEvent): void {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault(); // Ngăn xuống dòng
      this.send();
    }
  }

  private resetInputHeight(): void {
    if (this.chatInput) {
      const textarea = this.chatInput.nativeElement;
      textarea.style.height = 'auto';
    }
  }
  // --------------------------

  send(): void {
    const text = this.newMessage?.trim();
    if (!text || this.isSending) return;

    this.messages.push({ role: 'user', text });
    this.newMessage = '';
    
    // Reset input về 1 dòng
    this.resetInputHeight();
    this.focusInput();
    this.isSending = true;
    this.scrollToBottom();

    this.aiService.sendMessage(this.sessionId, text).subscribe({
      next: (res) => {
        const reply = res?.reply || res?.choices?.[0]?.message?.content || 'Không có trả lời.';
        this.messages.push({ role: 'assistant', text: reply });
        this.isSending = false;
        this.scrollToBottom();
        this.focusInput();
      },
      error: (err) => {
        console.error('AI chat error', err);
        this.messages.push({ role: 'assistant', text: 'Lỗi khi gọi dịch vụ AI.' });
        this.isSending = false;
        this.scrollToBottom();
      }
    });
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      if (this.chatBody && this.chatBody.nativeElement) {
        this.chatBody.nativeElement.scrollTop = this.chatBody.nativeElement.scrollHeight;
      }
    }, 100);
  }

  private generateSessionId(): string {
    return 's_' + Math.random().toString(36).substring(2, 10);
  }
}