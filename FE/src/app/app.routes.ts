import { Routes } from '@angular/router';
import { PatientLayout } from './layouts/patient-layout/patient-layout';
import { AdminLayout } from './layouts/admin-layout/admin-layout';
import { adminGuard } from './core/guards/admin-guard';
import { DoctorLayout } from './layouts/doctor-layout/doctor-layout';
import { doctorGuard } from './core/guards/doctor.guard';
import { PaymentReturn } from './features/staff/payment-return/payment-return';
import { staffGuard } from './core/guards/staff.guard'; // ✅ Import staff guard (nếu có)

export const routes: Routes = [
  // ===== PATIENT ROUTES (Home) =====
  {
    path: '',
    component: PatientLayout,
    loadChildren: () =>
      import('./features/patient/patient.routes').then((m) => m.PATIENT_ROUTES),
  },

  // ===== ADMIN ROUTES =====
  {
    path: 'admin',
    component: AdminLayout,
    canActivate: [adminGuard],
    loadChildren: () =>
      import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },

  // ===== DOCTOR ROUTES =====
  {
    path: 'doctor',
    component: DoctorLayout,
    canActivate: [doctorGuard],
    loadChildren: () => 
      import('./features/doctor/doctor.routes').then(m => m.DOCTOR_ROUTES)
  },

  // ===== ✅ STAFF ROUTES - CẬP NHẬT =====
  {
    path: 'staff',
    // ✅ Thêm guard nếu có (optional)
    // canActivate: [staffGuard],
    loadChildren: () =>
      import('./features/staff/staff.routes').then(m => m.staffRoutes),
  },

  // ===== PAYMENT RETURN =====
  {
    path: 'payment-return',
    component: PaymentReturn
  },

  // ===== 404 REDIRECT =====
  {
    path: '**',
    redirectTo: '',
    pathMatch: 'full',
  }
];