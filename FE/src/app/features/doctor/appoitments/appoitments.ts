// src/app/features/doctor/appoitments/appoitments.component.ts

import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { Observable } from 'rxjs';

// 1. SỬA ĐỔI: Import cả Service VÀ DTO từ cùng một file
import { 
  AppointmentService, 
  AppointmentResponseDto 
} from '../../../core/services/AppointmentService'; // Giữ nguyên đường dẫn của bạn

// 2. SỬA ĐỔI: Xóa toàn bộ phần định nghĩa interface local (Patient, DoctorInfo, Branch, Appointment)
//    vì chúng ta đã import AppointmentResponseDto


@Component({
  selector: 'app-appoitments',
  standalone: true,
  imports: [CommonModule], 
  templateUrl: './appoitments.html',
  styleUrl: './appoitments.scss'
})
// Giữ nguyên tên class 'Appoitments' theo file bạn cung cấp
export class Appoitments implements OnInit { 
  
  private appointmentService = inject(AppointmentService);

  // 3. SỬA ĐỔI: Đổi kiểu dữ liệu của Observable thành AppointmentResponseDto[]
  public appointments$!: Observable<AppointmentResponseDto[]>;

  // TODO: Cần lấy ID này từ user đang đăng nhập
  private currentDoctorId = '44ec66f7-8ab0-4e65-b47e-f11df325d938';
  public selectedAppointment: AppointmentResponseDto | null = null;
  public isLoadingDetail = false; // Biến cờ để hiển thị spinner trong modal
  ngOnInit(): void {
    this.loadAppointments();
  }

  loadAppointments(): void {
    // Giờ đây kiểu dữ liệu đã khớp hoàn toàn
    this.appointments$ = this.appointmentService.getDoctorAppointments(this.currentDoctorId);
  }
  // ---- HÀM MỚI ĐỂ XEM CHI TIẾT ----
  /**
   * Được gọi khi nhấp vào nút "Xem"
   * @param appointmentId ID của cuộc hẹn
   */
  onViewDetails(appointmentId: string): void {
    // Đặt lại biến và hiển thị cờ tải
    this.selectedAppointment = null;
    this.isLoadingDetail = true;

    // Gọi service để lấy chi tiết
    this.appointmentService.getAppointmentById(appointmentId).subscribe({
      next: (data) => {
        this.selectedAppointment = data; // Lưu dữ liệu chi tiết
        this.isLoadingDetail = false; // Ẩn cờ tải
      },
      error: (err) => {
        console.error('Lỗi khi lấy chi tiết cuộc hẹn:', err);
        this.isLoadingDetail = false; // Ẩn cờ tải khi có lỗi
        // TODO: Hiển thị thông báo lỗi cho người dùng
      }
    });
  }

  // ---- HÀM MỚI ĐỂ ĐÓNG MODAL ----
  onCloseModal(): void {
    this.selectedAppointment = null;
    this.isLoadingDetail = false; // Đảm bảo cờ tải cũng tắt
  }
}