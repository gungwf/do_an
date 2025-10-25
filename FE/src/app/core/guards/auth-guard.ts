import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth'; // <-- Import AuthService

export const authGuard: CanActivateFn = (route, state) => {
  // Inject AuthService và Router
  const authService = inject(AuthService);
  const router = inject(Router);

  // Kiểm tra xem đã đăng nhập chưa
  if (authService.isAuthenticated()) {
    return true; // OK, cho phép truy cập
  } else {
    // Chưa đăng nhập, điều hướng về trang login
    console.warn('Truy cập bị từ chối, yêu cầu đăng nhập!');
    router.navigate(['/auth/login']); 
    return false; // Chặn truy cập
  }
};