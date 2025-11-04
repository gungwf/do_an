import { Routes } from '@angular/router';
import { AdminDashboard } from './dashboard/dashboard';
import { AdminUsers } from './users/users';
import { AdminBranches } from './branches/branches';
import { AdminProducts } from './products/products';
import { AdminAppointments } from './appointments/appointments';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: 'dashboard',
    component: AdminDashboard,
  },
  {
    path: 'users',
    component: AdminUsers,
  },
  {
    path: 'branches',
    component: AdminBranches,
  },
  {
    path: 'products',
    component: AdminProducts,
  },
  {
    path: 'appointments',
    component: AdminAppointments,
  },
];








