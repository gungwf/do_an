import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router'; // Import Router
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth'; // Đường dẫn service auth
import { ToastrService } from 'ngx-toastr';
import { AuthModal } from '../../shared/components/auth-modal/auth-modal'; // Import Modal

@Component({
  selector: 'app-patient-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    AuthModal // Thêm Modal vào imports
  ],
  templateUrl: './patient-layout.html', // Đảm bảo tên file HTML đúng
  styleUrl: './patient-layout.scss', // Đảm bảo tên file SCSS đúng (nếu có)
})
export class PatientLayout {
  isAuthModalOpen = false; // Biến quản lý trạng thái mở/đóng modal

  constructor(
    public authService: AuthService, // public để HTML dùng được
    private toastr: ToastrService,
    private router: Router // Cần Router để điều hướng
  ) {}

  // Hàm xử lý link cần bảo vệ
  handleProtectedLink(url: string) {
    if (this.authService.isAuthenticated()) {
      this.router.navigate([url]); // Điều hướng nếu đã đăng nhập
    } else {
      this.toastr.info('Vui lòng đăng nhập để sử dụng chức năng này!');
      this.isAuthModalOpen = true; // Mở modal nếu chưa đăng nhập
    }
  }

  // Hàm đăng xuất
  logout(): void {
    this.authService.logout();
    this.toastr.success('Đăng xuất thành công!');
    window.location.reload(); // Tải lại trang
  }
}