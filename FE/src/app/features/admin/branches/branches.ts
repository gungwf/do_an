// src/app/features/admin/branches/branches.ts

import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule, DatePipe } from '@angular/common';
import { finalize } from 'rxjs';

// Service
import { BranchService } from '../../../core/services/branch.service'; 
import { AuthService } from '../../../core/services/auth'; 

// *** THAY ĐỔI: ĐỊNH NGHĨA MODEL TẠI ĐÂY (THAY VÌ IMPORT) ***
export interface Branch {
  id: string;
  branchName: string;
  address: string;
  phoneNumber: string;
  createdAt: string;
  updatedAt: string;
  active: boolean;
}
export type CreateBranchDto = Pick<Branch, 'branchName' | 'address' | 'phoneNumber'>;
// *** KẾT THÚC THAY ĐỔI ***

@Component({
  selector: 'app-branches',
  standalone: true,
  imports: [
    CommonModule, 
    ReactiveFormsModule, 
    DatePipe
  ],
  templateUrl: './branches.html',
  styleUrls: ['./branches.scss']
})
export class BranchesComponent implements OnInit {
  
  // Services
  private fb = inject(FormBuilder);
  public authService = inject(AuthService); 
  private branchService = inject(BranchService);

  // State
  public branches: Branch[] = []; // Vẫn dùng được kiểu 'Branch'
  public isLoading = false;
  public isSubmitting = false; 

  // Modal Thêm/Sửa
  public showBranchModal = false;
  public isEditMode = false;
  public branchForm: FormGroup;
  private editingBranch: Branch | null = null; 

  // Modal Chi tiết
  public showDetailModal = false;
  public isLoadingDetail = false;
  public selectedBranch: Branch | null = null;

  constructor() {
    this.branchForm = this.fb.group({
      branchName: ['', [Validators.required, Validators.minLength(3)]],
      address: ['', [Validators.required]],
      phoneNumber: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]], 
      id: [null],
      active: [true]
    });
  }

  ngOnInit(): void {
    this.loadBranches();
  }

  loadBranches(): void {
    this.isLoading = true;
    this.branchService.getBranches()
      .pipe(finalize(() => this.isLoading = false))
      .subscribe({
        next: (data) => {
          this.branches = data;
        },
        error: (err) => {
          console.error('Lỗi khi tải danh sách chi nhánh:', err);
        }
      });
  }

  // --- Logic Modal Thêm/Sửa ---

  openBranchModal(branch: Branch | null): void { // Vẫn dùng được kiểu 'Branch'
    if (branch) {
      this.isEditMode = true;
      this.editingBranch = branch; 
      this.branchForm.patchValue(branch); 
    } else {
      this.isEditMode = false;
      this.editingBranch = null;
      this.branchForm.reset({ active: true });
    }
    this.showBranchModal = true;
  }

  closeBranchModal(): void {
    this.showBranchModal = false;
    this.editingBranch = null;
  }

  onSubmitBranch(): void {
    if (this.branchForm.invalid) {
      this.branchForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;

    if (this.isEditMode && this.editingBranch) {
      const updatedBranch: Branch = { // Vẫn dùng được kiểu 'Branch'
        ...this.editingBranch, 
        ...this.branchForm.value 
      };
      
      this.branchService.updateBranch(updatedBranch)
        .pipe(finalize(() => this.isSubmitting = false))
        .subscribe({
          next: () => {
            this.loadBranches(); 
            this.closeBranchModal();
          },
          error: (err) => console.error('Lỗi cập nhật:', err)
        });

    } else {
      const createDto: CreateBranchDto = { // Vẫn dùng được kiểu 'CreateBranchDto'
        branchName: this.branchForm.value.branchName,
        address: this.branchForm.value.address,
        phoneNumber: this.branchForm.value.phoneNumber
      };
      
      this.branchService.createBranch(createDto)
        .pipe(finalize(() => this.isSubmitting = false))
        .subscribe({
          next: () => {
            this.loadBranches();
            this.closeBranchModal();
          },
          error: (err) => console.error('Lỗi thêm mới:', err)
        });
    }
  }

  // --- Logic Modal Chi tiết ---
  
  openDetailModal(branch: Branch): void { // Vẫn dùng được kiểu 'Branch'
    this.selectedBranch = null; 
    this.showDetailModal = true;
    this.isLoadingDetail = true;

    this.branchService.getBranchById(branch.id)
      .pipe(finalize(() => this.isLoadingDetail = false))
      .subscribe({
        next: (data) => {
          this.selectedBranch = data;
        },
        error: (err) => {
          console.error('Lỗi lấy chi tiết:', err);
          this.closeDetailModal();
        }
      });
  }

  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedBranch = null;
  }

  // --- Logic Soft-Delete (Toggle Status) ---

  toggleBranchStatus(branch: Branch): void { // Vẫn dùng được kiểu 'Branch'
    const updatedBranch: Branch = {
      ...branch,
      active: !branch.active 
    };

    this.branchService.updateBranch(updatedBranch).subscribe({
      next: (updatedData) => {
        const index = this.branches.findIndex(b => b.id === updatedData.id);
        if (index !== -1) {
          this.branches[index] = updatedData;
        }
      },
      error: (err) => {
        console.error('Lỗi khi đổi trạng thái:', err);
      }
    });
  }
}