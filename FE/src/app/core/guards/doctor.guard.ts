import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth';

export const doctorGuard = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  console.log('🔒 DoctorGuard: Checking access');

  // ✅ STEP 1: Check authentication (có token không?)
  if (!authService.isAuthenticated()) {
    console.warn('⚠️ DoctorGuard: User not authenticated');
    router.navigate(['/']);
    return false;
  }

  console.log('✅ DoctorGuard: User is authenticated');

  // ✅ STEP 2: Check doctor role (role có phải doctor không?)
  const role = authService.getUserRole();
  console.log('🔍 DoctorGuard: User role:', role);

  if (!authService.isDoctor()) {
    console.warn('⚠️ DoctorGuard: User is not a doctor, redirecting to home');
    router.navigate(['/']);
    return false;
  }

  console.log('✅ DoctorGuard: User is a doctor');
  console.log('✅ DoctorGuard: Access granted');
  
  // ✅ DONE! Cho vào ngay, không cần check userId hay profile
  // Dashboard component sẽ tự fetch profile khi mount
  return true;
};