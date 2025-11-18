// src/app/features/doctor/doctor.routes.ts

import { Routes } from '@angular/router';
import { Dashboard } from './dashboard/dashboard';

// 1. Import component bạn đã tạo
import { Appoitments } from './appoitments/appoitments';

export const DOCTOR_ROUTES: Routes = [
  {
    path: 'dashboard',
    component: Dashboard,
    title: 'Bảng điều khiển'
  },
  {
    // 2. Kích hoạt route này
    // Đường dẫn 'appointments' sẽ khớp với routerLink trong layout.html
    path: 'appointments', 
    component: Appoitments, // Trỏ đến component bạn đã tạo
    title: 'Quản lý Lịch hẹn'
  },
  {
    path: '', 
    redirectTo: 'dashboard',
    pathMatch: 'full'
  }
];