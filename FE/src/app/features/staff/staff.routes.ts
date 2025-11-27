import { Routes } from '@angular/router';
import { StaffLayout} from '../../layouts/staff-layout/staff-layout';
import { authGuard } from '../../core/guards/auth-guard';
import { staffGuard } from '../../core/guards/staff.guard';
import { Dashboard } from './dashboard/dashboard';
import { MedicalRecord } from './medical-record/medical-record';
import { PaymentReturn } from './payment-return/payment-return';

export const staffRoutes: Routes = [
  {
    path: '',
    component: StaffLayout,
    canActivate: [authGuard, staffGuard],
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        component: Dashboard
      },
      {
        path: 'medical-records', // ← ĐÂY LÀ ĐÚNG
        component: MedicalRecord
      },
      {
        path: 'payment-return',
        component: PaymentReturn
      }
    ]
  }
];