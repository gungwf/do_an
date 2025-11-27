import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MedicalRecordService } from '../../../core/services/medical-record.service';
import { PaymentService } from '../../../core/services/payment.service';

interface PerformedService {
  serviceId: string;
  serviceName: string;
  price: number;
}

interface MedicalRecordViewDto {
  id: string;
  appointmentId: string;
  diagnosis: string;
  icd10Code: string | null;
  createdAt: string;
  updatedAt: string;
  performedServices: PerformedService[];
  locked: boolean;
  esignature: string | null;
}

@Component({
  selector: 'app-medical-record-view-dialog',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './medical-record.html',
  styleUrls: ['./medical-record.scss']
})
export class MedicalRecordViewDialogComponent implements OnInit {
  @Input() appointmentId!: string;
  @Output() closed = new EventEmitter<void>();

  loading = true;
  error: string | null = null;
  data: MedicalRecordViewDto | null = null;
  paymentLoading = false;

  constructor(
    private service: MedicalRecordService,
    private paymentService: PaymentService
  ) {}

  ngOnInit(): void {
    if (!this.appointmentId) {
      this.error = 'Thiếu appointmentId';
      this.loading = false;
      return;
    }
    this.service.getMedicalRecordByAppointment(this.appointmentId).subscribe({
      next: (res) => {
        this.data = res as MedicalRecordViewDto;
        this.loading = false;
      },
      error: () => {
        this.error = 'Không tải được bệnh án';
        this.loading = false;
      }
    });
  }

  close(): void {
    this.closed.emit();
  }

  getTotalPrice(): number {
    if (!this.data?.performedServices) return 0;
    return this.data.performedServices.reduce((sum, s) => sum + s.price, 0);
  }

  proceedToPayment(): void {
    if (!this.appointmentId) {
      alert('Không tìm thấy mã cuộc hẹn');
      return;
    }
    
    console.log('=== PAYMENT INFO ===');
    console.log('Appointment ID:', this.appointmentId);
    console.log('Total Price:', this.getTotalPrice());
    
    if (confirm('Xác nhận thanh toán ' + this.getTotalPrice().toLocaleString('vi-VN') + ' VNĐ?')) {
      this.paymentLoading = true;
      
      // GỌI VỚI APPOINTMENT ID
      this.paymentService.generateBillPayment(this.appointmentId).subscribe({
        next: (res) => {
          console.log('✓ Payment response:', res);
          if (res.payUrl) {
            console.log('Redirecting to:', res.payUrl);
            window.location.href = res.payUrl;
          } else {
            alert('Không nhận được URL thanh toán');
            this.paymentLoading = false;
          }
        },
        error: (err) => {
          console.error('✗ Payment error:', err);
          let errorMsg = 'Không thể tạo thanh toán. ';
          if (err.error?.message) {
            errorMsg += err.error.message;
          } else if (err.status === 500) {
            errorMsg += 'Lỗi server. Vui lòng kiểm tra backend logs.';
          } else if (err.status === 404) {
            errorMsg += 'Không tìm thấy cuộc hẹn (404)';
          } else {
            errorMsg += 'Vui lòng thử lại.';
          }
          alert(errorMsg);
          this.paymentLoading = false;
        }
      });
    }
  }
}