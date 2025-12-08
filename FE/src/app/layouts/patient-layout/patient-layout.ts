import { Component, OnInit, OnDestroy } from '@angular/core';
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
import { ChatService } from '../../core/services/chat.service';
import { ChatBubbleComponent } from '../../shared/components/chat-bubble/chat-bubble.component';

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
  ],
  templateUrl: './patient-layout.html',
  styleUrls: ['./patient-layout.scss'],
})
export class PatientLayout implements OnInit, OnDestroy {
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
        const token = this.chatService.getToken();
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
    this.chatService.disconnect(); // Ngắt kết nối chat
    this.authService.logout(); // 1. Đăng xuất khỏi auth service
    this.cartService.clearStorage(); // 2. Dọn dẹp giỏ hàng
    this.toastr.success('Đăng xuất thành công!');
    this.router.navigate(['/']); // 3. Chuyển hướng về trang chủ
    
    // ⛔️ BỎ DÒNG RELOAD NÀY ĐI
    // setTimeout(() => window.location.reload(), 500);
  }
}