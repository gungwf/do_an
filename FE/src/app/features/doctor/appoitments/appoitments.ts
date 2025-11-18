import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { Observable } from 'rxjs';

import { 
  AppointmentService, 
  AppointmentResponseDto 
} from '../../../core/services/AppointmentService'; // Đường dẫn của bạn

// 1. IMPORT COMPONENT FORM BỆNH ÁN MỚI
import { MedicalRecordForm } from '../medical-record-form/medical-record-form';

@Component({
  selector: 'app-appoitments',
  standalone: true,
  // 2. THÊM COMPONENT MỚI VÀO IMPORTS
  imports: [CommonModule, MedicalRecordForm], 
  templateUrl: './appoitments.html',
  styleUrl: './appoitments.scss'
})
export class Appoitments implements OnInit { 
  
  private appointmentService = inject(AppointmentService);

  // Danh sách chính
  public appointments$!: Observable<AppointmentResponseDto[]>;

  // TODO: Cần lấy ID này từ user đang đăng nhập
  private currentDoctorId = '44ec66f7-8ab0-4e65-b47e-f11df325d938';
  
  // --- Các biến cho Modal XEM CHI TIẾT (Cũ) ---
  public selectedAppointment: AppointmentResponseDto | null = null;
  public isLoadingDetail = false; 

  // --- 3. THÊM BIẾN MỚI CHO MODAL TẠO BỆNH ÁN ---
  // Biến này giữ lịch hẹn đang được chọn để TẠO BỆNH ÁN
  public recordForAppointment: AppointmentResponseDto | null = null; 

  ngOnInit(): void {
    this.loadAppointments();
  }

  loadAppointments(): void {
    this.appointments$ = this.appointmentService.getDoctorAppointments(this.currentDoctorId);
  }

  // --- Các hàm cho Modal XEM CHI TIẾT (Cũ) ---
  
  onViewDetails(appointmentId: string): void {
    this.selectedAppointment = null;
    this.isLoadingDetail = true;

    this.appointmentService.getAppointmentById(appointmentId).subscribe({
      next: (data) => {
        this.selectedAppointment = data; 
        this.isLoadingDetail = false;
      },
      error: (err) => {
        console.error('Lỗi khi lấy chi tiết cuộc hẹn:', err);
        this.isLoadingDetail = false; 
      }
    });
  }

  onCloseModal(): void {
    this.selectedAppointment = null;
    this.isLoadingDetail = false;
  }

  // --- 4. THÊM CÁC HÀM MỚI ĐỂ ĐIỀU KHIỂN MODAL TẠO BỆNH ÁN ---

  /**
   * Mở Modal Tạo Bệnh Án.
   * (Được gọi từ nút "Tạo Bệnh án" trong file .html)
   */
  onStartMedicalRecord(appointment: AppointmentResponseDto): void {
    this.recordForAppointment = appointment;
  }

  /**
   * Đóng Modal Tạo Bệnh Án.
   * (Được gọi khi component con (form) phát sự kiện 'closeModal')
   */
  onCloseRecordModal(): void {
    this.recordForAppointment = null;
  }

  /**
   * Xử lý khi tạo bệnh án thành công.
   * (Được gọi khi component con (form) phát sự kiện 'submitSuccess')
   */
  onSubmitSuccess(): void {
    this.recordForAppointment = null; // Đóng modal
    this.loadAppointments(); // Tải lại danh sách (để cập nhật trạng thái)
  }
}