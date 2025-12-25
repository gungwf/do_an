import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth';

@Component({
  selector: 'app-staff-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './staff-layout.html',
  styleUrl: './staff-layout.scss'
})
export class StaffLayout {
  isSidebarCollapsed = false;
  currentUser: import('../../core/services/auth').UserDto | null = null;

  constructor(
    public authService: AuthService,
    private router: Router
  ) {
    this.authService.getCurrentUser().subscribe(user => {
      this.currentUser = user;
    });
  }

  toggleSidebar(): void {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }

  get displayName(): string {
    return 'Staff';
  }
}