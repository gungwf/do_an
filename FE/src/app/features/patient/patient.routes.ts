import { Routes } from '@angular/router';
import { Home } from './home/home';
import { MedicalRecords } from './medical-records/medical-records';
import { Appointments } from './appointments/appointments';
import { authGuard } from '../../core/guards/auth-guard'; // Đảm bảo tên file guard đúng

export const PATIENT_ROUTES: Routes = [
  {
    path: '', // Route gốc (trang chủ)
    component: Home,
  },
  {
    path: 'records',
    component: MedicalRecords,
    canActivate: [authGuard],
  },
  {
    path: 'appointments',
    component: Appointments,
    canActivate: [authGuard],
  },
  // Có thể thêm route '**' ở đây nếu muốn xử lý lỗi riêng cho patient
];