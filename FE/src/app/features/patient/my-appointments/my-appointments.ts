import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { AuthService, UserDto } from '../../../core/services/auth';
import { AppointmentService, AppointmentResponseDto } from '../../../core/services/AppointmentService';
import { FilterAppointmentsPipe } from './filter-appointments.pipe';

@Component({
  selector: 'app-my-appointments',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-appointments.html',
  styleUrl: './my-appointments.scss'
})
export class MyAppointmentsComponent implements OnInit {

  appointments: AppointmentResponseDto[] = [];
  isLoading = true;
  loadError = false;
  currentUser: UserDto | null = null;

  constructor(
    private authService: AuthService,
    private appointmentService: AppointmentService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.loadAppointments();
  }

  loadAppointments(): void {
    console.log('🌀 [MyAppointments] Bắt đầu tải lịch hẹn...');
    this.isLoading = true;
    this.loadError = false;

    this.authService.getCurrentUser().subscribe({
      next: (user) => {
        if (!user) {
          this.toastr.error('Không tìm thấy người dùng.');
          this.isLoading = false;
          return;
        }

        this.currentUser = user;
        console.log('🔍 [MyAppointments] userId:', user.id);

        this.appointmentService.getMyAppointments(user.id).subscribe({
          next: (data) => {
            this.appointments = data;
            this.isLoading = false;
            console.log('✅ [MyAppointments] Nhận dữ liệu:', data);
          },
          error: (err) => {
            console.error('❌ [MyAppointments] Lỗi API:', err);
            this.isLoading = false;
            this.loadError = true;
            this.toastr.error('Không thể tải danh sách lịch hẹn.');
          }
        });
      },
      error: (err) => {
        console.error('🚨 [MyAppointments] Lỗi AuthService:', err);
        this.isLoading = false;
      }
    });
  }

  getStatusText(status: string): string {
    switch (status) {
      case 'PENDING': return 'Chờ thanh toán';
      case 'CONFIRMED': return 'Đã xác nhận';
      case 'COMPLETED': return 'Đã hoàn thành';
      case 'CANCELED': return 'Đã hủy';
      default: return status;
    }
  }

  getStatusClass(status: string): string {
    const base = 'badge rounded-pill';
    switch (status) {
      case 'PENDING': return `${base} status-pending`;
      case 'CONFIRMED': return `${base} status-confirmed`;
      case 'COMPLETED': return `${base} status-completed`;
      case 'CANCELED': return `${base} status-canceled`;
      default: return base;
    }
  }
}
