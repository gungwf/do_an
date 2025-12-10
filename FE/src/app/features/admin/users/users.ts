import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';

interface PaginationResponse<T> {
  totalElements: number;
  totalPages: number;
  size: number;
  content: T[];
  number: number;
  sort: {
    empty: boolean;
    unsorted: boolean;
    sorted: boolean;
  };
  first: boolean;
  last: boolean;
  numberOfElements: number;
  pageable: {
    offset: number;
    sort: any;
    pageNumber: number;
    pageSize: number;
    unpaged: boolean;
    paged: boolean;
  };
  empty: boolean;
}

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './users.html',
  styleUrl: './users.scss',
})
export class AdminUsers implements OnInit, OnDestroy {
  // Tabs
  activeTab: 'patients' | 'staff' = 'patients';
  activeStaffType: 'doctors' | 'clinic-staff' | null = null;

  // Data
  patients: any[] = [];
  doctors: any[] = [];
  clinicStaff: any[] = [];
  branches: any[] = [];
  isLoading = true;
  isLoadingBranches = false;

  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;

  // Modals
  showPatientModal = false;
  showStaffModal = false;
  showDetailModal = false;
  selectedUser: any = null;
  isLoadingDetail = false;

  // Search Forms
  searchForm = new FormGroup({
    fullName: new FormControl(''),
    email: new FormControl(''),
    phoneNumber: new FormControl(''),
    active: new FormControl(''),
    branchId: new FormControl(''),
    specialty: new FormControl(''),
  });

  private destroy$ = new Subject<void>();

  // Forms
  patientForm = new FormGroup({
    fullName: new FormControl('', [Validators.required]),
    email: new FormControl('', [Validators.required, Validators.email]),
    password: new FormControl('', [Validators.required, Validators.minLength(6)]),
    phoneNumber: new FormControl('', [Validators.required]),
  });

  staffForm = new FormGroup({
    fullName: new FormControl('', [Validators.required]),
    email: new FormControl('', [Validators.required, Validators.email]),
    password: new FormControl('', [Validators.required, Validators.minLength(6)]),
    phoneNumber: new FormControl('', [Validators.required]),
    branchId: new FormControl('', [Validators.required]),
    specialty: new FormControl(''), // Chỉ cho bác sĩ
    certificate: new FormControl(''), // Chỉ cho bác sĩ
    role: new FormControl('', [Validators.required]), // DOCTOR hoặc CLINIC_STAFF
  });

  private apiUrl = 'http://localhost:8080';

  constructor(
    private http: HttpClient,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.loadBranches();
    this.loadPatients();

    // Listen to search form changes with debounce
    this.searchForm.valueChanges
      .pipe(
        debounceTime(500), // Wait 500ms after user stops typing
        distinctUntilChanged(), // Only emit if value actually changed
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        this.currentPage = 0; // Reset to first page on search
        this.onSearch();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadBranches(): void {
    this.isLoadingBranches = true;
    this.http.get<any[]>(`${this.apiUrl}/branches`).subscribe({
      next: (data) => {
        this.branches = data;
        this.isLoadingBranches = false;
      },
      error: (err) => {
        console.error('Error loading branches:', err);
        this.isLoadingBranches = false;
      },
    });
  }

  buildSearchBody(): any {
    const body: any = {
      page: this.currentPage,
      size: this.pageSize,
    };

    const search = this.searchForm.value;

    if (search.fullName) {
      body.fullName = search.fullName;
    }
    if (search.email) {
      body.email = search.email;
    }
    if (search.phoneNumber) {
      body.phoneNumber = search.phoneNumber;
    }
    if (search.active !== null && search.active !== '') {
      body.active = search.active === 'true'; // boolean true/false
    }

    if (search.branchId) {
      body.branchId = search.branchId;
    }
    if (search.specialty) {
      body.specialty = search.specialty;
    }

    return body;
  }

  loadPatients(): void {
    this.isLoading = true;
    const body = this.buildSearchBody();
    
    this.http.post<PaginationResponse<any>>(`${this.apiUrl}/users/patients/search`, body).subscribe({
      next: (response) => {
        this.patients = response.content || [];
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.currentPage = response.number;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading patients:', err);
        this.patients = [];
        this.isLoading = false;
      },
    });
  }

  loadDoctors(): void {
    this.isLoading = true;
    const body = this.buildSearchBody();
    
    this.http.post<PaginationResponse<any>>(`${this.apiUrl}/users/doctors/search`, body).subscribe({
      next: (response) => {
        this.doctors = response.content || [];
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.currentPage = response.number;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading doctors:', err);
        this.doctors = [];
        this.isLoading = false;
      },
    });
  }

  loadClinicStaff(): void {
    this.isLoading = true;
    const body = this.buildSearchBody();
    
    this.http.post<PaginationResponse<any>>(`${this.apiUrl}/users/staffs/search`, body).subscribe({
      next: (response) => {
        this.clinicStaff = response.content || [];
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.currentPage = response.number;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading clinic staff:', err);
        this.clinicStaff = [];
        this.isLoading = false;
      },
    });
  }

  onTabChange(tab: 'patients' | 'staff'): void {
    this.activeTab = tab;
    this.currentPage = 0;
    this.searchForm.reset();
    if (tab === 'patients') {
      this.activeStaffType = null;
      this.loadPatients();
    } else if (tab === 'staff') {
      this.activeStaffType = 'doctors'; // Luôn chọn Bác sĩ khi chuyển sang tab nhân viên
      this.loadDoctors();
    }
}

  onStaffTypeChange(type: 'doctors' | 'clinic-staff'): void {
    this.activeStaffType = type;
    this.currentPage = 0;
    this.searchForm.patchValue({ specialty: '', branchId: '' });
    if (type === 'doctors') {
      this.loadDoctors();
    } else {
      this.loadClinicStaff();
    }
  }

  onSearch(): void {
    this.currentPage = 0;
    if (this.activeTab === 'patients') {
      this.loadPatients();
    } else if (this.activeStaffType === 'doctors') {
      this.loadDoctors();
    } else if (this.activeStaffType === 'clinic-staff') {
      this.loadClinicStaff();
    }
  }

  resetSearch(): void {
    this.searchForm.reset();
    this.currentPage = 0;
    if (this.activeTab === 'patients') {
      this.loadPatients();
    } else if (this.activeStaffType === 'doctors') {
      this.loadDoctors();
    } else if (this.activeStaffType === 'clinic-staff') {
      this.loadClinicStaff();
    }
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    if (this.activeTab === 'patients') {
      this.loadPatients();
    } else if (this.activeStaffType === 'doctors') {
      this.loadDoctors();
    } else if (this.activeStaffType === 'clinic-staff') {
      this.loadClinicStaff();
    }
  }

  openDetailModal(userId: string): void {
    this.isLoadingDetail = true;
    this.showDetailModal = true;
    this.selectedUser = null;

    let apiUrl = '';
    if (this.activeTab === 'patients') {
      apiUrl = `${this.apiUrl}/users/${userId}`;
    } else if (this.activeStaffType === 'doctors') {
      apiUrl = `${this.apiUrl}/users/doctors/${userId}`;
    } else if (this.activeStaffType === 'clinic-staff') {
      apiUrl = `${this.apiUrl}/users/staffs/${userId}`;
    }

    this.http.get<any>(apiUrl).subscribe({
      next: (data) => {
        this.selectedUser = data;
        this.isLoadingDetail = false;
      },
      error: (err) => {
        console.error('Error loading user details:', err);
        this.toastr.error('Không thể tải thông tin chi tiết người dùng.');
        this.isLoadingDetail = false;
      },
    });
  }

  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedUser = null;
  }

  openPatientModal(): void {
    this.patientForm.reset();
    this.showPatientModal = true;
  }

  closePatientModal(): void {
    this.showPatientModal = false;
    this.patientForm.reset();
  }

  openStaffModal(type: 'doctors' | 'clinic-staff'): void {
    this.staffForm.reset();
    this.staffForm.patchValue({
      role: type === 'doctors' ? 'DOCTOR' : 'CLINIC_STAFF',
    });
    this.showStaffModal = true;
  }

  closeStaffModal(): void {
    this.showStaffModal = false;
    this.staffForm.reset();
  }

  onSubmitPatient(): void {
    if (this.patientForm.valid) {
      const formData = this.patientForm.value;
      this.http.post(`${this.apiUrl}/auth/register/patient`, formData).subscribe({
        next: () => {
          this.toastr.success('Thêm bệnh nhân thành công!');
          this.closePatientModal();
          this.loadPatients();
        },
        error: (err) => {
          console.error('Error adding patient:', err);
          this.toastr.error('Không thể thêm bệnh nhân. Vui lòng thử lại.');
        },
      });
    } else {
      this.patientForm.markAllAsTouched();
    }
  }

  onSubmitStaff(): void {
    if (this.staffForm.valid) {
      const formData = this.staffForm.value;
      this.http.post(`${this.apiUrl}/auth/register/staff`, formData).subscribe({
        next: () => {
          this.toastr.success('Thêm nhân viên thành công!');
          this.closeStaffModal();
          if (this.activeStaffType === 'doctors') {
            this.loadDoctors();
          } else {
            this.loadClinicStaff();
          }
        },
        error: (err) => {
          console.error('Error adding staff:', err);
          this.toastr.error('Không thể thêm nhân viên. Vui lòng thử lại.');
        },
      });
    } else {
      this.staffForm.markAllAsTouched();
    }
  }

  deleteUser(userId: string): void {
    if (confirm('Bạn có chắc muốn xóa người dùng này?')) {
      this.http.delete(`${this.apiUrl}/users/${userId}`).subscribe({
        next: () => {
          this.toastr.success('Xóa người dùng thành công!');
          if (this.activeTab === 'patients') {
            this.loadPatients();
          } else if (this.activeStaffType === 'doctors') {
            this.loadDoctors();
          } else {
            this.loadClinicStaff();
          }
        },
        error: (err) => {
          console.error('Error deleting user:', err);
          this.toastr.error('Không thể xóa người dùng. Vui lòng thử lại.');
        },
      });
    }
  }

  toggleUserStatus(user: any): void {
    const newStatus = !user.active;
    this.http.patch(`${this.apiUrl}/users/${user.id}/status`, { active: newStatus }).subscribe({
      next: () => {
        user.active = newStatus;
        this.toastr.success('Cập nhật trạng thái thành công!');
      },
      error: (err) => {
        console.error('Error updating status:', err);
        this.toastr.error('Không thể cập nhật trạng thái. Vui lòng thử lại.');
      },
    });
  }

  getBranchName(branchId: string): string {
    const branch = this.branches.find((b) => b.id === branchId);
    return branch ? branch.branchName || branch.name : 'N/A';
  }

  getCurrentUsers(): any[] {
    if (this.activeTab === 'patients') {
      return this.patients;
    }
    if (this.activeStaffType === 'doctors') {
      return this.doctors;
    }
    if (this.activeStaffType === 'clinic-staff') {
      return this.clinicStaff;
    }
    return [];
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxPagesToShow = 5;
    let startPage = Math.max(0, this.currentPage - Math.floor(maxPagesToShow / 2));
    let endPage = Math.min(this.totalPages - 1, startPage + maxPagesToShow - 1);
    
    if (endPage - startPage < maxPagesToShow - 1) {
      startPage = Math.max(0, endPage - maxPagesToShow + 1);
    }

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    return pages;
  }
}
