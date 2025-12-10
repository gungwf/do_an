import { Component, OnInit,AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { AuthService, UserDto } from '../../../core/services/auth';
import { AppointmentService, AppointmentResponseDto } from '../../../core/services/AppointmentService';
import * as AOS from 'aos';

@Component({
  selector: 'app-my-appointments',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './my-appointments.html',
  styleUrl: './my-appointments.scss'
})
export class MyAppointmentsComponent implements OnInit {

  appointments: AppointmentResponseDto[] = [];
  filteredAppointments: AppointmentResponseDto[] = [];
  isLoading = true;
  loadError = false;
  currentUser: UserDto | null = null;
  statusFilter: string = 'ALL';
  // Pagination
  page: number = 0;
  size: number = 10;
  totalPages: number = 0;
  totalElements: number = 0;

  constructor(
    private authService: AuthService,
    private appointmentService: AppointmentService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    AOS.init({ once: true });
    this.loadAppointments();
  }

  ngAfterViewInit() {
  AOS.refresh();
  }

  /**
   * Load danh sách lịch hẹn của user hiện tại
   */
  loadAppointments(): void {
    console.log('🌀 [MyAppointments] Bắt đầu tải lịch hẹn...');
    this.isLoading = true;
    this.loadError = false;

    this.authService.getCurrentUser().subscribe({
      next: (user) => {
        if (!user) {
          this.toastr.error('Không tìm thấy thông tin người dùng.', 'Lỗi');
          this.isLoading = false;
          this.loadError = true;
          return;
        }

        this.currentUser = user;
        console.log('🔍 [MyAppointments] patientId:', user.id);

        // ✅ Gọi API GET /appointments/patient/{patientId}?page=&size=
        this.appointmentService.getMyAppointments(user.id, this.page, this.size).subscribe({
          next: (pageData) => {
            const data = pageData?.content || [];
            this.appointments = data;
            this.filteredAppointments = data;
            this.totalPages = pageData?.totalPages || 0;
            this.totalElements = pageData?.totalElements || 0;
            this.isLoading = false;
            console.log('✅ [MyAppointments] Nhận được', data.length, 'lịch hẹn (page', this.page, ')');
            console.log('📦 Page data:', pageData);
          },
          error: (err: any) => {
            console.error('❌ [MyAppointments] Lỗi API:', err);
            this.isLoading = false;
            this.loadError = true;
            this.toastr.error('Không thể tải danh sách lịch hẹn.', 'Lỗi');
          }
        });
      },
      error: (err: any) => {
        console.error('🚨 [MyAppointments] Lỗi AuthService:', err);
        this.isLoading = false;
        this.loadError = true;
        this.toastr.error('Không thể xác thực người dùng.', 'Lỗi');
      }
    });
  }

  /**
   * Pagination helpers
   */
  goToPage(newPage: number): void {
    if (this.totalPages === 0) return;
    if (newPage < 0) return;
    if (newPage >= this.totalPages) return;
    this.page = newPage;
    this.loadAppointments();
  }

  nextPage(): void {
    if (this.page < this.totalPages - 1) {
      this.goToPage(this.page + 1);
    }
  }

  prevPage(): void {
    if (this.page > 0) {
      this.goToPage(this.page - 1);
    }
  }

  /**
   * Filter appointments by status
   */
  filterByStatus(status: string): void {
    this.statusFilter = status;
    
    if (status === 'ALL') {
      this.filteredAppointments = this.appointments;
    } else {
      this.filteredAppointments = this.appointments.filter(
        app => app.status === status
      );
    }
    
    console.log(`🔍 Filter: ${status}, Kết quả: ${this.filteredAppointments.length} lịch hẹn`);
  }

  /**
   * Đếm số lượng appointments theo status
   */
  getCountByStatus(status: string): number {
    return this.appointments.filter(app => app.status === status).length;
  }

  /**
   * Get Vietnamese text for status
   */
  getStatusText(status: string): string {
    const statusMap: { [key: string]: string } = {
      'PENDING': 'Chờ xác nhận',
      'CONFIRMED': 'Đã xác nhận',
      'COMPLETED': 'Hoàn thành',
      'CANCELLED': 'Đã hủy',
      'CANCELED': 'Đã hủy'
    };
    return statusMap[status] || status;
  }

  /**
   * Get CSS class for status badge
   */
  getStatusClass(status: string): string {
    const statusMap: { [key: string]: string } = {
      'PENDING': 'status-pending',
      'CONFIRMED': 'status-confirmed',
      'COMPLETED': 'status-completed',
      'CANCELLED': 'status-cancelled',
      'CANCELED': 'status-cancelled'
    };
    return statusMap[status] || 'status-default';
  }

  /**
   * Get Bootstrap icon class for status
   */
  getStatusIcon(status: string): string {
    const iconMap: { [key: string]: string } = {
      'PENDING': 'bi bi-hourglass-split',
      'CONFIRMED': 'bi bi-check-circle-fill',
      'COMPLETED': 'bi bi-clipboard-check-fill',
      'CANCELLED': 'bi bi-x-circle-fill',
      'CANCELED': 'bi bi-x-circle-fill'
    };
    return iconMap[status] || 'bi bi-circle';
  }

  /**
   * View appointment details
   */
  viewDetails(appointment: AppointmentResponseDto): void {
    console.log('👁️ View details:', appointment);
    const details = `
Bác sĩ: ${appointment.doctor.fullName}
Chi nhánh: ${appointment.branch.branchName}
Địa chỉ: ${appointment.branch.address}
Thời gian: ${new Date(appointment.appointmentTime).toLocaleString('vi-VN')}
Giá khám: ${appointment.priceAtBooking.toLocaleString('vi-VN')}đ
Ghi chú: ${appointment.notes || 'Không có'}
    `.trim();
    
    this.toastr.info(details, 'Chi tiết lịch hẹn', { 
      timeOut: 8000,
      enableHtml: true 
    });
  }

  /**
   * ✅ Cancel appointment - CHỈ cho PENDING
   * Gọi API PUT /appointments/{id}/cancel
   */
  cancelAppointment(appointment: AppointmentResponseDto): void {
    // ✅ Validation: Chỉ cho phép hủy khi status = PENDING
    if (appointment.status !== 'PENDING') {
      this.toastr.warning('Chỉ có thể hủy lịch hẹn đang chờ xác nhận.', 'Không thể hủy');
      return;
    }

    const confirmMsg = `Bạn có chắc chắn muốn hủy lịch hẹn với bác sĩ ${appointment.doctor.fullName}?\n\nThời gian: ${new Date(appointment.appointmentTime).toLocaleString('vi-VN')}\nĐịa điểm: ${appointment.branch.branchName}`;
    
    if (!confirm(confirmMsg)) {
      return;
    }

    console.log('🗑️ Cancelling appointment:', appointment.id);

    // ✅ Gọi API PUT /appointments/{id}/cancel
    this.appointmentService.cancelAppointment(appointment.id).subscribe({
      next: () => {
        this.toastr.success('Đã hủy lịch hẹn thành công!', 'Thành công');
        console.log('✅ Appointment cancelled successfully');
        
        // Reload appointments list
        this.loadAppointments();
      },
      error: (err: any) => {
        console.error('❌ Error cancelling appointment:', err);
        
        let errorMsg = 'Không thể hủy lịch hẹn. Vui lòng thử lại sau.';
        
        if (err.error?.message) {
          errorMsg = err.error.message;
        } else if (err.status === 404) {
          errorMsg = 'Không tìm thấy lịch hẹn này.';
        } else if (err.status === 400) {
          errorMsg = 'Không thể hủy lịch hẹn đã hoàn thành hoặc đã xác nhận.';
        }
        
        this.toastr.error(errorMsg, 'Lỗi');
      }
    });
  }

  /**
   * ✅ Pay now - CHỈ cho PENDING
   * Redirect to VNPay payment page
   */
  payNow(appointment: AppointmentResponseDto): void {
    // ✅ Validation: Chỉ cho phép thanh toán khi status = PENDING
    if (appointment.status !== 'PENDING') {
      this.toastr.warning('Chỉ có thể thanh toán cho lịch hẹn đang chờ xác nhận.', 'Không thể thanh toán');
      return;
    }

    console.log('💳 Creating payment for appointment:', appointment.id);
    this.toastr.info('Đang tạo link thanh toán...', 'Vui lòng chờ');
    
    // ✅ Gọi API tạo payment
    this.appointmentService.createPayment(appointment.id).subscribe({
      next: (paymentUrl: string) => {
        if (paymentUrl && paymentUrl.startsWith('http')) {
          console.log('✅ Redirecting to payment URL:', paymentUrl);
          this.toastr.success('Đang chuyển đến trang thanh toán VNPay...', 'Thành công');
          
          // Redirect sau 1 giây
          setTimeout(() => {
            window.location.href = paymentUrl;
          }, 1000);
        } else {
          this.toastr.error('Không thể tạo link thanh toán.', 'Lỗi');
          console.error('Invalid payment URL:', paymentUrl);
        }
      },
      error: (err: any) => {
        console.error('❌ Error creating payment:', err);
        
        let errorMsg = 'Không thể tạo thanh toán. Vui lòng thử lại.';
        
        if (err.error?.message) {
          errorMsg = err.error.message;
        } else if (err.status === 404) {
          errorMsg = 'Không tìm thấy lịch hẹn này.';
        } else if (err.status === 400) {
          errorMsg = 'Lịch hẹn không hợp lệ để thanh toán. Chỉ lịch hẹn "Chờ xác nhận" mới có thể thanh toán.';
        }
        
        this.toastr.error(errorMsg, 'Lỗi');
      }
    });
  }
}