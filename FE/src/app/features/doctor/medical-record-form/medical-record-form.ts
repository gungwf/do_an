import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { AppointmentResponseDto } from '../../../core/services/AppointmentService';
import { ServiceDto, ServiceService } from '../../../core/services/service.service';
import { CreateMedicalRecordDto, MedicalRecordService } from '../../../core/services/medical-record.service';

@Component({
  selector: 'app-medical-record-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './medical-record-form.html',
  styleUrl: './medical-record-form.scss'
})
export class MedicalRecordForm implements OnChanges {
  @Input() appointment: AppointmentResponseDto | null = null;
  @Output() closeModal = new EventEmitter<void>();
  @Output() submitSuccess = new EventEmitter<void>();

  private serviceService = inject(ServiceService);
  private medicalRecordService = inject(MedicalRecordService);

  public diagnosis: string = '';
  public icd10Code: string = '';
  public allServices: ServiceDto[] = [];
  public selectedServices: ServiceDto[] = [];
  public serviceSearchTerm: string = '';
  public filteredServices: ServiceDto[] = [];
  public showConfirmationDialog: boolean = false;
  public isSubmitting: boolean = false;
  
  // ✅ THÊM MỚI: Biến kiểm soát hiển thị dropdown
  public showServiceDropdown: boolean = false;
  
  public get totalServicePrice(): number {
    return this.selectedServices.reduce((acc, s) => acc + s.price, 0);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['appointment'] && this.appointment) {
      this.resetForm();
      this.loadAllServices();
    }
  }

  resetForm(): void {
    this.diagnosis = '';
    this.icd10Code = '';
    this.selectedServices = [];
    this.serviceSearchTerm = '';
    this.filteredServices = [];
    this.showConfirmationDialog = false;
    this.isSubmitting = false;
    this.showServiceDropdown = false;
  }

  loadAllServices(): void {
    this.serviceService.getServices().subscribe(services => {
      this.allServices = services.filter(s => s.active); // Chỉ lấy services active
      this.filteredServices = [...this.allServices];
      console.log('✅ Đã load services:', this.allServices.length);
    });
  }

  // ✅ THÊM MỚI: Handler khi focus vào search box
  onServiceSearchFocus(): void {
    this.showServiceDropdown = true;
    // Nếu chưa có search term, hiển thị tất cả services
    if (!this.serviceSearchTerm) {
      this.onSearchService();
    }
    console.log('✅ Focus vào search box, hiển thị dropdown');
  }

  // ✅ THÊM MỚI: Handler khi blur khỏi search box
  onServiceSearchBlur(): void {
    // Delay để xử lý click vào dropdown trước khi ẩn
    setTimeout(() => {
      this.showServiceDropdown = false;
      console.log('✅ Blur khỏi search box, ẩn dropdown');
    }, 200);
  }

  onSearchService(): void {
    const term = this.serviceSearchTerm.toLowerCase().trim();
    
    // ✅ Nếu không có search term, hiển thị tất cả (trừ đã chọn)
    if (!term) {
      this.filteredServices = this.allServices.filter(s => 
        !this.selectedServices.some(selected => selected.id === s.id)
      );
      console.log('✅ Hiển thị tất cả services:', this.filteredServices.length);
    } else {
      // ✅ Có search term, filter theo tên
      this.filteredServices = this.allServices.filter(s => 
        s.serviceName.toLowerCase().includes(term) &&
        !this.selectedServices.some(selected => selected.id === s.id)
      );
      console.log('✅ Tìm kiếm:', term, '→', this.filteredServices.length, 'kết quả');
    }
  }

  onAddService(service: ServiceDto): void {
    // ✅ Kiểm tra trùng lặp
    if (this.selectedServices.some(s => s.id === service.id)) {
      console.warn('⚠️ Service đã được chọn:', service.serviceName);
      return;
    }
    
    this.selectedServices.push(service);
    console.log('✅ Đã thêm service:', service.serviceName, '→ Tổng:', this.selectedServices.length);
    
    // ✅ Clear search và refresh dropdown
    this.serviceSearchTerm = '';
    this.onSearchService();
    
    // ✅ Giữ dropdown mở sau khi chọn
    this.showServiceDropdown = true;
  }

  onRemoveService(serviceId: string): void {
    const removedService = this.selectedServices.find(s => s.id === serviceId);
    this.selectedServices = this.selectedServices.filter(s => s.id !== serviceId);
    
    if (removedService) {
      console.log('✅ Đã xóa service:', removedService.serviceName);
    }
    
    // ✅ Refresh dropdown để hiển thị lại service vừa xóa
    this.onSearchService();
  }

  onShowConfirmation(): void {
    if (!this.diagnosis || this.diagnosis.trim() === '') {
      alert('Vui lòng nhập chẩn đoán.');
      return;
    }
    if (this.selectedServices.length === 0) {
      alert('Vui lòng chọn ít nhất một dịch vụ.');
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
      diagnosis: this.diagnosis.trim(),
      icd10Code: this.icd10Code.trim() || '',
      serviceIds: this.selectedServices.map(s => s.id),
      prescriptionItems: [],
      templateId: null
    };

    console.log('✅ Payload gửi đi:', payload);

    this.medicalRecordService.createMedicalRecord(payload)
      .pipe(finalize(() => this.isSubmitting = false))
      .subscribe({
        next: (response) => {
          console.log('✅ Tạo bệnh án thành công:', response);
          alert('Tạo bệnh án thành công!');
          this.submitSuccess.emit();
          this.onClose();
        },
        error: (err) => {
          console.error('❌ Lỗi khi tạo bệnh án:', err);
          alert('Có lỗi xảy ra: ' + (err.error?.message || 'Vui lòng thử lại.'));
          this.showConfirmationDialog = false;
        }
      });
  }

  onClose(): void {
    this.closeModal.emit();
  }
}