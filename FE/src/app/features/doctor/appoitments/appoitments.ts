import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { Observable, finalize, forkJoin } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import * as AOS from 'aos';

import { 
  AppointmentService, 
  AppointmentResponseDto,
  DoctorAppointmentSearchDto,
  PagedAppointmentResponse
} from '../../../core/services/AppointmentService';

import { MedicalRecordForm } from '../medical-record-form/medical-record-form';
import { PrescriptionDialog } from '../../../shared/components/prescription-dialog/prescription-dialog';
import { AuthService } from '../../../core/services/auth';
import { MedicalRecordService } from '../../../core/services/medical-record.service';

@Component({
  selector: 'app-appoitments',
  standalone: true,
  imports: [CommonModule, MedicalRecordForm, FormsModule, PrescriptionDialog], 
  templateUrl: './appoitments.html',
  styleUrl: './appoitments.scss'
})
export class Appoitments implements OnInit { 
  
  private appointmentService = inject(AppointmentService);
  private authService = inject(AuthService);
  private toastr = inject(ToastrService);
  private medicalRecordService = inject(MedicalRecordService);

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

  public showPrescriptionDialog = false;
  public prescriptionAppointment: AppointmentResponseDto | null = null;

  ngOnInit(): void {
    AOS.init({ once: true });
    this.currentDoctorId = this.authService.getUserId();
    
    if (!this.currentDoctorId) {
      console.error('Không tìm thấy ID bác sĩ! Vui lòng đăng nhập lại.');
      return;
    }

    this.loadAppointments();
  }
  ngAfterViewInit() {
  AOS.refresh();
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
        
        // ✅ Lấy medicalRecordId cho các appointment có thể kê đơn
        const eligibleStatuses = ['PAID_SERVICE', 'PENDING_BILLING', 'COMPLETED'];
        const eligibleAppointments = this.appointments.filter(
          a => eligibleStatuses.includes(a.status)
        );
        
        if (eligibleAppointments.length > 0) {
          this.loadMedicalRecordsForAppointments(eligibleAppointments);
        }
        
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

  // ✅ Method mới: Lấy medical record cho nhiều appointments
  private loadMedicalRecordsForAppointments(appointments: AppointmentResponseDto[]): void {
    appointments.forEach(appt => {
      this.medicalRecordService.getMedicalRecordByAppointment(appt.id).subscribe({
        next: (record) => {
          // Cập nhật thông tin medical record vào appointment
          appt.medicalRecordId = record.id;
          appt.diagnosis = record.diagnosis;
          appt.icd10Code = record.icd10Code || '';
          
          console.log(`✓ Loaded medical record for appointment ${appt.id}:`, {
            medicalRecordId: record.id,
            diagnosis: record.diagnosis
          });
        },
        error: (err) => {
          // Không có medical record thì bỏ qua (chưa tạo bệnh án)
          console.log(`ℹ No medical record for appointment ${appt.id}`);
        }
      });
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
    this.loadAppointments();
  }

  canCreateMedicalRecord(appointment: AppointmentResponseDto): boolean {
    const createdStatuses = ['PENDING_BILLING', 'PAID_SERVICE'];
    if (createdStatuses.includes(appointment.status)) {
      return false;
    }
    return appointment.status === 'CONFIRMED' || appointment.status === 'COMPLETED';
  }

  // ✅ Kiểm tra có thể kê đơn thuốc không
  canCreatePrescription(appointment: AppointmentResponseDto): boolean {
    return appointment.status === 'PAID_SERVICE' && !!appointment.medicalRecordId;
  }

  // ✅ Mở dialog kê đơn thuốc
  openPrescriptionDialog(appointment: AppointmentResponseDto): void {
    if (!appointment.medicalRecordId) {
      this.toastr.error('Không tìm thấy bệnh án cho cuộc hẹn này');
      return;
    }
    this.prescriptionAppointment = appointment;
    this.showPrescriptionDialog = true;
  }

  closePrescriptionDialog(): void {
    this.showPrescriptionDialog = false;
    this.prescriptionAppointment = null;
  }

  onPrescriptionSaved(): void {
    this.toastr.success('Đơn thuốc đã được lưu thành công!');
    this.loadAppointments();
  }
}