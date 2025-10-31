import { Routes } from '@angular/router';
import { Home } from './home/home';
import { MedicalRecords } from './medical-records/medical-records';
import { AppointmentBooking } from './appointment-booking/appointment-booking';
import { authGuard } from '../../core/guards/auth-guard'; 
import { PaymentSuccess } from './payment-success/payment-success';
import { PaymentFailed } from './payment-failed/payment-failed';

// --- (MỚI) Import component trung gian ---
import { PaymentReturnHandler } from './payment-return-handler/payment-return-handler';


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
  
  // --- (MỚI) Route trung gian để "bắt" redirect từ VNPay ---
  {
    path: 'payment-return', // VNPay sẽ redirect về đây
    component: PaymentReturnHandler,
    canActivate: [authGuard] // Cần auth để gọi API confirm
  },
  
  // --- (GIỮ NGUYÊN) 2 route kết quả cuối cùng ---
  {
    path: 'payment-success', // Handler sẽ chuyển đến đây
    component: PaymentSuccess,
    canActivate: [authGuard] 
  },
  {
    path: 'payment-failed', // Handler sẽ chuyển đến đây
    component: PaymentFailed,
    canActivate: [authGuard] 
  }
];