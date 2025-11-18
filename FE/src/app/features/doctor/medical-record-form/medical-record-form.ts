import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // Quan trọng: Import FormsModule
import { finalize } from 'rxjs';

// Import DTO và Services
import { AppointmentResponseDto } from '../../../core/services/AppointmentService';
import { ServiceDto, ServiceService } from '../../../core/services/service.service';
import { CreateMedicalRecordDto, MedicalRecordService } from '../../../core/services/medical-record.service';

@Component({
  selector: 'app-medical-record-form',
  imports: [CommonModule, FormsModule],
  templateUrl: './medical-record-form.html',
  styleUrl: './medical-record-form.scss'
})
export class MedicalRecordForm implements OnChanges{
  // 1. Inputs/Outputs để giao tiếp với component cha (appoitments)
  @Input() appointment: AppointmentResponseDto | null = null; // Nhận lịch hẹn
  @Output() closeModal = new EventEmitter<void>(); // Báo cha "đóng tôi lại"
  @Output() submitSuccess = new EventEmitter<void>(); // Báo cha "tạo thành công"

  // 2. Services
  private serviceService = inject(ServiceService);
  private medicalRecordService = inject(MedicalRecordService);

  // 3. Toàn bộ state của Form
  public diagnosis: string = '';
  public allServices: ServiceDto[] = [];
  public selectedServices: ServiceDto[] = [];
  public serviceSearchTerm: string = '';
  public filteredServices: ServiceDto[] = [];
  public showConfirmationDialog: boolean = false;
  public isSubmitting: boolean = false;
  
  public get totalServicePrice(): number {
    // Tính toán tổng giá
    return this.selectedServices.reduce((acc, s) => acc + s.price, 0);
  }

  // 4. Logic
  ngOnChanges(changes: SimpleChanges): void {
    // Khi component cha set Input 'appointment', component này sẽ kích hoạt
    if (changes['appointment'] && this.appointment) {
      this.resetForm();
      this.loadAllServices();
    }
  }

  resetForm(): void {
    this.diagnosis = '';
    this.selectedServices = [];
    this.serviceSearchTerm = '';
    this.filteredServices = [];
    this.showConfirmationDialog = false;
    this.isSubmitting = false;
  }

  loadAllServices(): void {
    this.serviceService.getServices().subscribe(services => {
      this.allServices = services;
      this.filteredServices = [...services];
    });
  }

  onSearchService(): void {
    const term = this.serviceSearchTerm.toLowerCase();
    this.filteredServices = this.allServices.filter(s => 
      s.serviceName.toLowerCase().includes(term) &&
      !this.selectedServices.some(selected => selected.id === s.id)
    );
  }

  onAddService(service: ServiceDto): void {
    this.selectedServices.push(service);
    this.serviceSearchTerm = '';
    this.onSearchService();
  }

  onRemoveService(serviceId: string): void {
    this.selectedServices = this.selectedServices.filter(s => s.id !== serviceId);
    this.onSearchService();
  }

  onShowConfirmation(): void {
    if (!this.diagnosis || this.diagnosis.trim() === '') {
      alert('Vui lòng nhập chẩn đoán.');
      return;
    }
    this.showConfirmationDialog = true;
  }

  onBackToForm(): void {
    this.showConfirmationDialog = false;
  }

  onConfirmSubmit(): void {
    if (!this.appointment) return;

    this.isSubmitting = true;

    const payload: CreateMedicalRecordDto = {
      appointmentId: this.appointment.id,
      diagnosis: this.diagnosis,
      serviceIds: this.selectedServices.map(s => s.id),
      prescriptionItems: []
    };

    this.medicalRecordService.createMedicalRecord(payload)
      .pipe(
        finalize(() => this.isSubmitting = false)
      )
      .subscribe({
        next: () => {
          alert('Tạo bệnh án thành công!');
          this.submitSuccess.emit(); // Báo cho cha biết đã thành công
        },
        error: (err) => {
          console.error('Lỗi khi tạo bệnh án:', err);
          alert('Có lỗi xảy ra, vui lòng thử lại.');
          this.showConfirmationDialog = true; 
        }
      });
  }

  // Hàm để gọi Output "đóng"
  onClose(): void {
    this.closeModal.emit();
  }
}
