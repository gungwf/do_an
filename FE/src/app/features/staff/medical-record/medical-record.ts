import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  MedicalRecordService,
  StaffAppointmentSearchDto,
  AppointmentResponseDto,
  SpringPage
} from '../../../core/services/medical-record.service';
import { MedicalRecordViewDialogComponent } from '../../../shared/components/medical-record/medical-record';

interface ViewAppointment extends AppointmentResponseDto {
  patientDisplayName: string;
  doctorDisplayName: string;
}

@Component({
  selector: 'app-medical-records',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, MedicalRecordViewDialogComponent],
  templateUrl: './medical-record.html',
  styleUrls: ['./medical-record.scss']
})
export class MedicalRecord implements OnInit {
  loading = false;
  searchDto: StaffAppointmentSearchDto = {
    page: 0, size: 10, sort: 'appointmentTime,desc',
    startTime: '', endTime: '', searchText: '', status: ''
  };
  appointments: ViewAppointment[] = [];
  totalPages = 0;
  totalElements = 0;
  viewingAppointmentId: string | null = null;

  constructor(private service: MedicalRecordService) {}

  ngOnInit(): void { this.loadData(); }

  loadData(): void {
    this.loading = true;
    this.service.getStaffAppointments(this.searchDto).subscribe({
      next: res => {
        this.appointments = res.content.map(a => ({
          ...a,
          patientDisplayName: a.patient?.fullName || '',
          doctorDisplayName: a.doctor?.fullName || ''
        }));
        this.totalPages = res.totalPages;
        this.totalElements = res.totalElements;
        this.searchDto.page = res.number;
        this.loading = false;
      },
      error: () => {
        this.appointments = [];
        this.loading = false;
      }
    });
  }

  onSearch(): void { this.searchDto.page = 0; this.loadData(); }
  resetFilters(): void {
    this.searchDto = { page: 0, size: 10, sort: 'appointmentTime,desc',
      startTime: '', endTime: '', searchText: '', status: '' };
    this.loadData();
  }
  goPage(p: number): void { if (p<0 || p>=this.totalPages) return; this.searchDto.page = p; this.loadData(); }
  reload(): void { this.loadData(); }

  badgeClass(status: string | undefined): string {
  // ✅ Xử lý undefined
  if (!status) {
    return 'bg-secondary';
  }

  const map: { [key: string]: string } = {
    'PENDING': 'bg-warning',
    'CONFIRMED': 'bg-info',
    'PENDING_BILLING': 'bg-danger',
    'PAID_SERVICE': 'bg-success',
    'COMPLETED': 'bg-success',
    'PAID': 'bg-success',
    'CANCELLED': 'bg-secondary',
    'CANCELED': 'bg-secondary'
  };
  
  return map[status] || 'bg-secondary';
}

  openRecord(appointmentId: string): void { this.viewingAppointmentId = appointmentId; }
  closeDialog(): void { this.viewingAppointmentId = null; }
  getStatusText(status: string | undefined): string {
  // ✅ Xử lý undefined
  if (!status) {
    return 'Không rõ';
  }

  const statusMap: { [key: string]: string } = {
    'PENDING': 'Chờ xác nhận',
    'CONFIRMED': 'Đã xác nhận',
    'PENDING_BILLING': 'Chờ thanh toán',
    'PAID_SERVICE': 'Đã thanh toán',
    'COMPLETED': 'Hoàn thành',
    'PAID': 'Đã thanh toán',
    'CANCELLED': 'Đã hủy',
    'CANCELED': 'Đã hủy'
  };
  
  return statusMap[status] || status;
}
}