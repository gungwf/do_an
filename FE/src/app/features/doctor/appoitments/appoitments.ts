import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { Observable, finalize } from 'rxjs';
import { FormsModule } from '@angular/forms';

import { 
  AppointmentService, 
  AppointmentResponseDto,
  DoctorAppointmentSearchDto,
  PagedAppointmentResponse
} from '../../../core/services/AppointmentService';

import { MedicalRecordForm } from '../medical-record-form/medical-record-form';
import { AuthService } from '../../../core/services/auth';

@Component({
  selector: 'app-appoitments',
  standalone: true,
  imports: [CommonModule, MedicalRecordForm, FormsModule], 
  templateUrl: './appoitments.html',
  styleUrl: './appoitments.scss'
})
export class Appoitments implements OnInit { 
  
  private appointmentService = inject(AppointmentService);
  private authService = inject(AuthService);

  public appointments: AppointmentResponseDto[] = [];
  public isLoading = true;

  public paginationState: DoctorAppointmentSearchDto = {
    page: 0,
    size: 10,
    sort: 'appointmentTime,desc',
    searchText: '',
    status: ''
  };

  public totalPages: number = 0;
  public totalElements: number = 0;

  private currentDoctorId: string | null = null;
  
  public selectedAppointment: AppointmentResponseDto | null = null;
  public isLoadingDetail = false; 
  public recordForAppointment: AppointmentResponseDto | null = null; 

  ngOnInit(): void {
    this.currentDoctorId = this.authService.getUserId();
    
    if (!this.currentDoctorId) {
      console.error('Không tìm thấy ID bác sĩ! Vui lòng đăng nhập lại.');
      return;
    }

    this.loadAppointments();
  }

  loadAppointments(): void {
    if (!this.currentDoctorId) {
      console.error('currentDoctorId is null, không thể tải lịch hẹn.');
      return;
    }

    this.isLoading = true;
    this.appointmentService.getDoctorAppointments(
      this.currentDoctorId,
      this.paginationState
    )
    .pipe(finalize(() => this.isLoading = false))
    .subscribe({
      next: (response: PagedAppointmentResponse) => {
        this.appointments = response.content; 
        this.totalPages = response.totalPages;
        this.totalElements = response.totalElements;
        this.paginationState.page = response.number;
      },
      error: (err) => {
        console.error('Lỗi khi tải danh sách lịch hẹn:', err);
        this.appointments = [];
      }
    });
  }

  onFilterChange(): void {
    this.paginationState.page = 0;
    this.loadAppointments();
  }

  onSearch(): void {
    this.paginationState.page = 0;
    this.loadAppointments();
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.paginationState.page = page;
      this.loadAppointments();
    }
  }

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

  onStartMedicalRecord(appointment: AppointmentResponseDto): void {
    this.recordForAppointment = appointment;
  }

  onCloseRecordModal(): void {
    this.recordForAppointment = null;
  }

  onSubmitSuccess(): void {
    console.log('Bệnh án đã được tạo thành công, đang reload...');
    this.recordForAppointment = null;
    // Reload lại danh sách để cập nhật trạng thái mới
    this.loadAppointments();
  }

  // Hàm kiểm tra có thể tạo bệnh án không
  canCreateMedicalRecord(appointment: AppointmentResponseDto): boolean {
    // Chỉ cho phép tạo khi trạng thái là CONFIRMED hoặc COMPLETED
    // và chưa tạo bệnh án (không phải PENDING_BILLING hoặc PAID_SERVICE)
    const createdStatuses = ['PENDING_BILLING', 'PAID_SERVICE'];
    if (createdStatuses.includes(appointment.status)) {
      return false;
    }
    return appointment.status === 'CONFIRMED' || appointment.status === 'COMPLETED';
  }
}