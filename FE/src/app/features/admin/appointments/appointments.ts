import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { CommonModule, DatePipe } from '@angular/common';
import { finalize } from 'rxjs';

// Import service và các interface TỪ service
import { 
  AppointmentService, 
  AppointmentSearchDto, 
  AppointmentSearchResponse,
  AppointmentResponseDto, // Đây là kiểu 'Appointment' của chúng ta
  BranchSimpleDto,
  DoctorDto
} from '../../../core/services/AppointmentService'; // Sửa lại đường dẫn nếu cần

@Component({
  selector: 'app-appointments',
  standalone: true,
  imports: [ CommonModule, ReactiveFormsModule, DatePipe ],
  templateUrl: './appointments.html',
  styleUrls: ['./appointments.scss']
})
// *** THAY ĐỔI 1: Sửa tên Class (để sửa lỗi build) ***
export class AdminAppointments implements OnInit { 
  // Services
  private fb = inject(FormBuilder);
  private appointmentService = inject(AppointmentService);

  // State
  public searchForm: FormGroup;
  public appointments: AppointmentResponseDto[] = []; 
  public branches: BranchSimpleDto[] = [];
  
  // State cho filter động
  private allDoctors: DoctorDto[] = []; 
  public filteredDoctors: DoctorDto[] = []; 
  
  // Loading
  public isLoading = false;
  public isLoadingDoctors = false; 
  
  // Pagination
  public totalElements = 0;
  public currentPage = 0;
  public pageSize = 10;
  public totalPages = 0;

  constructor() {
    this.searchForm = this.fb.group({
      branchId: [''],
      // Logic dropdown đã sửa
      doctorId: [''], 
      status: [''],
      patientName: [''], 
      doctorName: [''], 
      startTime: [''],
      endTime: ['']
    });
  }

  ngOnInit(): void {
    this.loadAppointments();
    this.loadFilterData();
  }

  loadFilterData(): void {
    this.appointmentService.getBranchesSimple().subscribe(data => {
      this.branches = data;
    });
    
    this.isLoadingDoctors = true;
    this.appointmentService.getDoctors()
      .pipe(finalize(() => this.isLoadingDoctors = false))
      .subscribe(data => {
        this.allDoctors = data;
        // Hiển thị tất cả bác sĩ khi tải xong
        this.filteredDoctors = data; 
      });
  }

  // Logic filter động đã sửa
  onBranchChange(): void {
    const branchId = this.searchForm.get('branchId')?.value;
    const doctorControl = this.searchForm.get('doctorId');

    if (branchId) {
      this.filteredDoctors = this.allDoctors.filter(d => d.branchId === branchId);
    } else {
      this.filteredDoctors = this.allDoctors;
    }
    
    doctorControl?.reset('', { onlySelf: true });
  }

  // Logic Reset đã sửa
  resetSearch(): void {
    this.searchForm.reset({
      branchId: '',
      doctorId: '', 
      status: '',
      patientName: '',
      doctorName: '',
      startTime: '',
      endTime: ''
    });
    this.filteredDoctors = this.allDoctors; 
    this.currentPage = 0;
    this.loadAppointments();
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.loadAppointments();
  }

  // Hàm gọi API chính
  loadAppointments(): void {
    this.isLoading = true;
    const formValues = this.searchForm.value;

    let doctorNameFromDropdown: string | null = null;
    if (formValues.doctorId) {
      const selectedDoctor = this.allDoctors.find(d => d.id === formValues.doctorId);
      doctorNameFromDropdown = selectedDoctor ? selectedDoctor.fullName : null;
    }

    const finalDoctorName = formValues.doctorName || doctorNameFromDropdown;

    const searchBody: AppointmentSearchDto = {
      page: this.currentPage,
      size: this.pageSize,
      branchId: formValues.branchId || null,
      status: formValues.status || null,
      patientName: formValues.patientName || null,
      doctorName: finalDoctorName,
      startTime: this.formatDateToISO(formValues.startTime, 'start'),
      endTime: this.formatDateToISO(formValues.endTime, 'end'),

      // *** THAY ĐỔI 2: Thêm sắp xếp (yêu cầu của bạn) ***
      sort: "appointmentTime,desc"
    };

    this.appointmentService.searchAppointments(searchBody)
      .pipe(finalize(() => this.isLoading = false))
      .subscribe({
        next: (response) => {
          this.appointments = response.content;
          this.totalElements = response.totalElements;
          this.totalPages = response.totalPages;
        },
        error: (err) => {
          console.error('Lỗi khi tìm kiếm cuộc hẹn:', err);
          this.appointments = [];
          this.totalElements = 0;
        }
      });
  }
  
  // (Hàm helper và phân trang...)
  private formatDateToISO(dateString: string | null, type: 'start' | 'end'): string | null {
    if (!dateString) return null;
    try {
      const date = new Date(dateString);
      if (type === 'start') {
        date.setHours(0, 0, 0, 0);
      } else {
        date.setHours(23, 59, 59, 999);
      }
      return date.toISOString();
    } catch {
      return null;
    }
  }

  onPageChange(page: number): void {
    if (page < 0 || page >= this.totalPages) return; 
    this.currentPage = page;
    this.loadAppointments();
  }
  
  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxPagesToShow = 5;
    const halfPages = Math.floor(maxPagesToShow / 2);

    let startPage = Math.max(this.currentPage - halfPages, 0);
    let endPage = Math.min(this.currentPage + halfPages, this.totalPages - 1);

    if (this.currentPage - halfPages < 0) {
      endPage = Math.min(maxPagesToShow - 1, this.totalPages - 1);
    }

    if (this.currentPage + halfPages >= this.totalPages) {
      startPage = Math.max(this.totalPages - maxPagesToShow, 0);
    }

    endPage = Math.min(endPage, this.totalPages - 1);

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    return pages;
  }
}