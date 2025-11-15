import { Routes } from '@angular/router';
import { PatientLayout } from './layouts/patient-layout/patient-layout';
import { AdminLayout } from './layouts/admin-layout/admin-layout';
import { adminGuard } from './core/guards/admin-guard';
import { DoctorGuard } from './core/guards/doctor.guard'; // <-- Guard MỚI
import { DoctorLayout } from './layouts/doctor-layout/doctor-layout';
export const routes: Routes = [
  {
    path: '', // Route gốc trỏ đến PatientLayout
    component: PatientLayout,
    loadChildren: () =>
      import('./features/patient/patient.routes').then((m) => m.PATIENT_ROUTES),
  },
  {
    path: 'admin', // Route cho admin
    component: AdminLayout,
    canActivate: [adminGuard],
    loadChildren: () =>
      import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: 'doctor',
    component: DoctorLayout,
    canActivate: [DoctorGuard], // <-- Chỉ cần guard này
    loadChildren: () => import('./features/doctor/doctor.routes')
                            .then(m => m.DOCTOR_ROUTES)
  },
  {
    path: '**', // Bất kỳ URL nào khác sẽ về trang chủ
    redirectTo: '',
    pathMatch: 'full',
  },
];