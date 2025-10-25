import { Routes } from '@angular/router';
import { PatientLayout } from './layouts/patient-layout/patient-layout';

export const routes: Routes = [
  {
    path: '', // Route gốc trỏ đến PatientLayout
    component: PatientLayout,
    loadChildren: () =>
      import('./features/patient/patient.routes').then((m) => m.PATIENT_ROUTES),
  },
  {
    path: '**', // Bất kỳ URL nào khác sẽ về trang chủ
    redirectTo: '',
    pathMatch: 'full',
  },
];