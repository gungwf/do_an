import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
  ],
  templateUrl: './admin-layout.html',
  styleUrl: './admin-layout.scss',
})
export class AdminLayout {
  isSidebarCollapsed = false;
  currentUser: import('../../core/services/auth').UserDto | null = null;

  constructor(
    public authService: AuthService,
    private toastr: ToastrService,
    private router: Router
  ) {
    this.authService.getCurrentUser().subscribe(user => {
      this.currentUser = user;
    });
  }

  toggleSidebar() {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }

  logout(): void {
    this.authService.logout();
    this.toastr.success('Đăng xuất thành công!');
    this.router.navigate(['/']);
  }
}








