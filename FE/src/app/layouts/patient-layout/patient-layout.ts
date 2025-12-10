import { Component } from '@angular/core';
import {
  RouterOutlet,
  RouterLink,
  RouterLinkActive,
  Router,
} from '@angular/router';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '../../core/services/auth';
import { AuthModal } from '../../shared/components/auth-modal/auth-modal';
import { CartService } from '../../core/services/cartService';
import { ChatBubbleComponent } from '../../shared/components/chat-bubble/chat-bubble.component';
import { AiChatComponent } from '../../shared/components/ai-chat/ai-chat.component';
import { ChatService } from '../../core/services/chat.service';

@Component({
  selector: 'app-patient-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    AuthModal,
    ChatBubbleComponent,
    AiChatComponent
  ],
  templateUrl: './patient-layout.html',
  styleUrls: ['./patient-layout.scss'],
})
export class PatientLayout {
  isAuthModalOpen = false;
  showCartPreview = false;

  constructor(
    public authService: AuthService,
    private toastr: ToastrService,
    public router: Router,
    public cartService: CartService,
    private chatService: ChatService
  ) {}

  ngOnInit(): void {
    // Kết nối WebSocket nếu user đã đăng nhập
    if (this.authService.isAuthenticated()) {
      try {
        const token = this.authService.getToken();
        if (token) {
          this.chatService.connect(token);
        }
      } catch (error) {
        console.error('Error connecting to WebSocket:', error);
        // Không throw error để app vẫn chạy được
      }
    }
  }

  ngOnDestroy(): void {
    // Ngắt kết nối WebSocket khi rời layout
    try {
      this.chatService.disconnect();
    } catch (error) {
      console.error('Error disconnecting WebSocket:', error);
    }
  }  handleProtectedLink(url: string) {
    if (this.authService.isAuthenticated()) {
      this.router.navigate([url]);
    } else {
      this.toastr.info('Vui lòng đăng nhập để sử dụng chức năng này!');
      this.isAuthModalOpen = true;
    }
  }

  toggleCartPreview(show: boolean): void {
    this.showCartPreview = show;
  }

  goToCart(): void {
    this.router.navigate(['/cart']);
  }

  logout(): void {
    this.authService.logout();
    this.cartService.clearStorage();
    this.toastr.success('Đăng xuất thành công!');
    this.router.navigate(['/']);
  }
}