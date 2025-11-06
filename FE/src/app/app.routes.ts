import { Routes } from '@angular/router';
import { PatientLayout } from './layouts/patient-layout/patient-layout';
import { AdminLayout } from './layouts/admin-layout/admin-layout';
import { adminGuard } from './core/guards/admin-guard';

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
    path: '**', // Bất kỳ URL nào khác sẽ về trang chủ
    redirectTo: '',
    pathMatch: 'full',
  },
];