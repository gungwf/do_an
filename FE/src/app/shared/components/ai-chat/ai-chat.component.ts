import { Component } from '@angular/core';
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

  constructor(private aiService: AiChatService) {
    // Use or generate session id so conversation persists in backend
    this.sessionId = localStorage.getItem('ai_chat_session') || this.generateSessionId();
    localStorage.setItem('ai_chat_session', this.sessionId);
  }

  toggle(): void {
    this.isOpen = !this.isOpen;
  }

  send(): void {
    const text = this.newMessage?.trim();
    if (!text) return;

    this.messages.push({ role: 'user', text });
    this.newMessage = '';
    this.isSending = true;

    this.aiService.sendMessage(this.sessionId, text).subscribe({
      next: (res) => {
        // Expecting { sessionId, reply, toolCall? }
        const reply = res?.reply || res?.choices?.[0]?.message?.content || 'Không có trả lời.';
        this.messages.push({ role: 'assistant', text: reply });
        this.isSending = false;
      },
      error: (err) => {
        console.error('AI chat error', err);
        this.messages.push({ role: 'assistant', text: 'Lỗi khi gọi dịch vụ AI.' });
        this.isSending = false;
      }
    });
  }

  private generateSessionId(): string {
    return 's_' + Math.random().toString(36).substring(2, 10);
  }
}
