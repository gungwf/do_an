import { Routes } from '@angular/router';
import { StaffLayout} from '../../layouts/staff-layout/staff-layout';
import { authGuard } from '../../core/guards/auth-guard';
import { staffGuard } from '../../core/guards/staff.guard';
import { Dashboard } from './dashboard/dashboard';
import { MedicalRecord } from './medical-record/medical-record';
import { PaymentReturn } from './payment-return/payment-return';
import { StaffProfile } from './staff-profile/staff-profile';

export const staffRoutes: Routes = [
  {
    path: '',
    component: StaffLayout,
    canActivate: [authGuard, staffGuard],
    children: [
      {
        path: '',
        redirectTo: 'medical-records',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        component: Dashboard
      },
      {
        path: 'medical-records',
        component: MedicalRecord
      },
      {
        path: 'payment-return',
        component: PaymentReturn
      },
      {
        path: 'profile',
        component: StaffProfile
      },
      {
        path: 'inventory',
        loadComponent: () => import('./inventory/inventory')
          .then(m => m.Inventory)
      },
      // ✅ NEW: Forecast Dashboard Route
      {
        path: 'forecast',
        loadComponent: () => import('./forecast-dashboard/forecast-dashboard')
          .then(m => m.ForecastDashboard)
      }
    ]
  }
];