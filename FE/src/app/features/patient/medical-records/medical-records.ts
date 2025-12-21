import { Component, OnInit, AfterViewInit } from '@angular/core';
import { MedicalRecordService, MedicalRecordListItem, MedicalRecordDetail } from '../../../core/services/medical-record.service';
import { AuthService } from '../../../core/services/auth';
import { DatePipe, CommonModule } from '@angular/common';
import { MedicalRecordDetailDialog } from '../../../shared/components/medical-record-detail-dialog/medical-record-detail-dialog';
import * as AOS from 'aos';

@Component({
  selector: 'app-medical-records',
  templateUrl: './medical-records.html',
  styleUrls: ['./medical-records.scss'],
  imports: [CommonModule, DatePipe, MedicalRecordDetailDialog]
})
export class MedicalRecords implements OnInit {
  records: MedicalRecordListItem[] = [];
  selectedRecord: MedicalRecordDetail | null = null;
  isDialogOpen = false;
  isLoading = false;
  page = 0;
  size = 20; // Luôn để 5 bệnh án 1 trang
  totalPages = 1;
  totalElements = 0;

  constructor(private medicalRecordService: MedicalRecordService) {}

  ngOnInit() {
    
    AOS.init({ once: true });
    this.fetchRecords();
  }

  ngAfterViewInit() {
  AOS.refresh();
  }

  fetchRecords() {
    this.isLoading = true;
    this.medicalRecordService.getMedicalRecordsOfCurrentPatient(this.page, this.size)
      .subscribe({
        next: res => {
          console.log('API response:', res); // Kiểm tra dữ liệu trả về
          this.records = res.content;
          this.totalPages = res.totalPages;
          this.totalElements = res.totalElements;
          this.isLoading = false;
        },
        error: () => { this.isLoading = false; }
      });
  }

  goToPage(p: number) {
    if (p >= 0 && p < this.totalPages) {
      this.page = p;
      this.fetchRecords();
    }
  }

  viewDetail(recordId: string) {
    this.isLoading = true;
    // Tìm bản ghi trong danh sách để lấy appointmentTime
    const found = this.records.find(r => r.medicalRecordId === recordId);
    this.medicalRecordService.getMedicalRecordDetailById(recordId)
      .subscribe({
        next: res => {
          // Gắn thêm appointmentTime nếu có
          this.selectedRecord = found ? { ...res, appointmentTime: found.appointment.appointmentTime } : res;
          this.isDialogOpen = true;
          this.isLoading = false;
        },
        error: () => { this.isLoading = false; }
      });
  }

  closeDetailDialog() {
    this.isDialogOpen = false;
    this.selectedRecord = null;
  }

  
  badgeClass(status: string): string {
  switch (status) {
    case 'PENDING': return 'bg-warning';
    case 'CONFIRMED': return 'bg-success';
    case 'PENDING_BILLING': return 'bg-info';
    case 'PAID_SERVICE': return 'bg-info';
    case 'COMPLETED': return 'bg-primary';
    case 'PAID': return 'bg-success';
    default: return 'bg-secondary';
  }
}
statusIcon(status: string): string {
  switch (status) {
    case 'PENDING': return 'bi-hourglass-split';
    case 'CONFIRMED': return 'bi-check-circle-fill';
    case 'PENDING_BILLING': return 'bi-cash-coin';
    case 'PAID_SERVICE': return 'bi-cash-coin';
    case 'COMPLETED': return 'bi-clipboard-check-fill';
    case 'PAID': return 'bi-credit-card-fill';
    default: return 'bi-question-circle';
  }
}
getStatusText(status: string): string {
  const statusMap: { [key: string]: string } = {
    'PENDING': 'Chờ xác nhận',
    'CONFIRMED': 'Đã xác nhận',
    'PENDING_BILLING': 'Chờ thanh toán',
    'PAID_SERVICE': 'Đã thanh toán',
    'COMPLETED': 'Hoàn thành',
    'PAID': 'Đã thanh toán'
  };
  return statusMap[status] || status;
}
reload() {
  this.fetchRecords();
}
}