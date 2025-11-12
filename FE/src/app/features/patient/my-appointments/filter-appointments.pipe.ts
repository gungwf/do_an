import { Pipe, PipeTransform } from '@angular/core';
import { AppointmentResponseDto } from '../../../core/services/AppointmentService';

@Pipe({
  name: 'filterAppointments',
  standalone: true
})
export class FilterAppointmentsPipe implements PipeTransform {

  transform(items: AppointmentResponseDto[] | null, status: 'UPCOMING' | 'COMPLETED' | 'CANCELED'): AppointmentResponseDto[] {
    if (!items) {
      return [];
    }
    
    switch (status) {
      case 'UPCOMING':
        // Sắp tới bao gồm Chờ thanh toán (PENDING) và Đã xác nhận (CONFIRMED)
        return items.filter(a => a.status === 'PENDING' || a.status === 'CONFIRMED');
      case 'COMPLETED':
        return items.filter(a => a.status === 'COMPLETED');
      case 'CANCELED':
        return items.filter(a => a.status === 'CANCELED');
      default:
        return items;
    }
  }
}