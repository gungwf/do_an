import { Routes } from '@angular/router';
import { Home } from './home/home';
import { MedicalRecords } from './medical-records/medical-records';
import { AppointmentBooking } from './appointment-booking/appointment-booking';
import { authGuard } from '../../core/guards/auth-guard';

export const PATIENT_ROUTES: Routes = [
  {
    path: '',
    component: Home,
  },
  {
    path: 'records',
    component: MedicalRecords,
    canActivate: [authGuard],
  },
  {
    path: 'appointments',
    component: AppointmentBooking,
    canActivate: [authGuard],
  },
  {
    path: 'my-appointments',
    loadComponent: () =>
      import('./my-appointments/my-appointments').then(
        (m) => m.MyAppointmentsComponent
      ),
    canActivate: [authGuard],
  },
  {
    path: 'cart',
    loadComponent: () =>
      import('./cart/cart').then((m) => m.CartComponent),
    canActivate: [authGuard]
  },
  {
    path: 'products',
    loadComponent: () =>
      import('./products/products').then(m => m.ProductsComponent),
  },
  
];