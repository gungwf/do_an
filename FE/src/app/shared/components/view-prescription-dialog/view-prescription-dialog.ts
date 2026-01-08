import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MedicalRecordService, MedicalRecordDetail } from '../../../core/services/medical-record.service';

@Component({
  selector: 'app-view-prescription-dialog',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './view-prescription-dialog.html',
  styleUrls: ['./view-prescription-dialog.scss']
})
export class ViewPrescriptionDialog implements OnChanges {
  @Input() medicalRecordId: string | null = null;
  @Input() open: boolean = false;
  @Output() close = new EventEmitter<void>();

  private medicalRecordService = inject(MedicalRecordService);
  
  public record: MedicalRecordDetail | null = null;
  public isLoading = false;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['medicalRecordId'] && this.medicalRecordId && this.open) {
      this.loadPrescription();
    }
  }

  loadPrescription(): void {
    if (!this.medicalRecordId) return;
    
    this.isLoading = true;
    this.medicalRecordService.getMedicalRecordDetailById(this.medicalRecordId).subscribe({
      next: (data) => {
        this.record = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Lỗi khi tải đơn thuốc:', err);
        this.isLoading = false;
      }
    });
  }

  onClose(): void {
    this.close.emit();
  }

  onBackdrop(event: MouseEvent): void {
    this.onClose();
  }

  get totalPrescriptionCost(): number {
    if (!this.record?.prescriptionItems) return 0;
    return this.record.prescriptionItems.reduce((sum, item) => sum + ((item.price ?? 0) * item.quantity), 0);
  }
}
