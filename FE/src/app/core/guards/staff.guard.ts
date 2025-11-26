import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth';

export const staffGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  console.log('staffGuard() được gọi');

  if (!authService.isAuthenticated()) {
    console.warn('staffGuard: Chưa đăng nhập!');
    router.navigate(['/auth/login']);
    return false;
  }

  const isStaff = authService.isStaff();
  console.log('staffGuard: isStaff =', isStaff);

  if (!isStaff) {
    console.warn('staffGuard: Không có quyền staff!');
    router.navigate(['/']);
    return false;
  }

  return true;
};