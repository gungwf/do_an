import { Component, OnInit, OnDestroy } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth';
import { ToastrService } from 'ngx-toastr';
import { ChatService } from '../../core/services/chat.service';
import { ChatBubbleComponent } from '../../shared/components/chat-bubble/chat-bubble.component';

@Component({
  selector: 'app-doctor-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    ChatBubbleComponent,
  ],
  templateUrl: './doctor-layout.html',
  styleUrl: './doctor-layout.scss', // Đảm bảo bạn đã sao chép file scss
})
export class DoctorLayout implements OnInit, OnDestroy {
  isSidebarCollapsed = false;

  constructor(
    public authService: AuthService,
    private toastr: ToastrService,
    private router: Router,
    private chatService: ChatService
  ) {}

  ngOnInit(): void {
    // Kết nối WebSocket cho bác sĩ
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
  }

  toggleSidebar() {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }

  logout(): void {
    this.chatService.disconnect(); // Ngắt kết nối chat
    this.authService.logout();
    this.toastr.success('Đăng xuất thành công!');
    this.router.navigate(['/']); // Điều hướng về trang chủ
  }
}