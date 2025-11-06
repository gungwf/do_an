import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth';

export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Kiểm tra xem đã đăng nhập và là admin không
  if (authService.isAuthenticated() && authService.isAdmin()) {
    return true; // OK, cho phép truy cập
  } else {
    // Chưa đăng nhập hoặc không phải admin, điều hướng về trang chủ
    console.warn('Truy cập bị từ chối, yêu cầu quyền admin!');
    router.navigate(['/']);
    return false; // Chặn truy cập
  }
};








