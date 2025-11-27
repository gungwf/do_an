import { Routes } from '@angular/router';
import { PatientLayout } from './layouts/patient-layout/patient-layout';
import { AdminLayout } from './layouts/admin-layout/admin-layout';
import { adminGuard } from './core/guards/admin-guard';
import { DoctorLayout } from './layouts/doctor-layout/doctor-layout';
import { DoctorGuard } from './core/guards/doctor.guard';
import { PaymentReturn } from './features/staff/payment-return/payment-return';

export const routes: Routes = [
  {
    path: '',
    component: PatientLayout,
    loadChildren: () =>
      import('./features/patient/patient.routes').then((m) => m.PATIENT_ROUTES),
  },
  {
    path: 'admin',
    component: AdminLayout,
    canActivate: [adminGuard],
    loadChildren: () =>
      import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: 'doctor',
    component: DoctorLayout,
    canActivate: [DoctorGuard],
    loadChildren: () => import('./features/doctor/doctor.routes')
                            .then(m => m.DOCTOR_ROUTES)
  },
  {
    path: 'staff',
    loadChildren: () =>
      import('./features/staff/staff.routes').then(m => m.staffRoutes),
  },
  {
    path: 'payment-return',
    component: PaymentReturn
  },
  {
    path: '**',
    redirectTo: '',
    pathMatch: 'full',
  }
];