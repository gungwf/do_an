import { Routes } from '@angular/router';
import { Dashboard } from './dashboard/dashboard';
import { Appoitments } from './appoitments/appoitments';
import { DoctorProfile } from './doctor-profile/doctor-profile';

export const DOCTOR_ROUTES: Routes = [
  // ===== DASHBOARD =====
  {
    path: 'dashboard',
    component: Dashboard,
    title: 'Bảng điều khiển'
  },

  // ===== APPOINTMENTS =====
  {
    path: 'appointments', 
    component: Appoitments,
    title: 'Quản lý Lịch hẹn'
  },

  // ===== PROFILE =====
  {
    path: 'profile',
    component: DoctorProfile,
    title: 'Hồ sơ cá nhân'
  },

  // ===== DEFAULT REDIRECT =====
  {
    path: '', 
    redirectTo: 'dashboard',
    pathMatch: 'full'
  }
];