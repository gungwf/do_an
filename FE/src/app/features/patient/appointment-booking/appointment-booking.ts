import { Component, OnInit } from '@angular/core';
import { CommonModule, formatDate } from '@angular/common'; 
// (MỚI) Thêm switchMap để nối chuỗi API
import { Observable, forkJoin, map, startWith, of, tap, catchError, switchMap } from 'rxjs'; 
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { AuthService, UserDto } from '../../../core/services/auth';
// (SỬA) Sửa lại tên file import (AppointmentService -> appointment)
import { AppointmentService, BranchSimpleDto, DoctorDto, SpecialtyDto } from '../../../core/services/AppointmentService'; 
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-appointment-booking',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './appointment-booking.html',
  styleUrl: './appointment-booking.scss'
})
export class AppointmentBooking implements OnInit {

  currentUser: UserDto | null = null;
  allBranchesList: BranchSimpleDto[] = [];
  branches$: Observable<BranchSimpleDto[]> = of([]);
  allDoctors: DoctorDto[] = [];
  filteredDoctors$: Observable<DoctorDto[]> = of([]);
  
  // (MỚI) Thêm Observable cho chuyên khoa
  specialties$: Observable<SpecialtyDto[]> = of([]); 

  bookingForm = new FormGroup({
    patientName: new FormControl({ value: '', disabled: true }, Validators.required),
    reason: new FormControl('', Validators.required), // notes
    doctorNameFilter: new FormControl(''),
    branchFilter: new FormControl(''),
    specialtyFilter: new FormControl(''), // (MỚI) Thêm filter chuyên khoa
  });

  isLoading = true; // Tải ban đầu

  // Trạng thái logic lịch hẹn
  selectedDoctor: DoctorDto | null = null;
  calendarDays: { date: string, label: string, dayOfWeek: string }[] = []; // Lịch 7 ngày
  selectedDate: string | null = null; 
  isLoadingSlots = false; // Spinner khi tải giờ
  availableSlots: string[] = []; // Giờ trống cho ngày đã chọn
  selectedTime: string | null = null; 

  // Trạng thái dialog
  isConfirmModalOpen: boolean = false;
  isBooking: boolean = false; // Trạng thái "đang đặt lịch" cho spinner

  constructor(
    private authService: AuthService,
    private appointmentService: AppointmentService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    // (Giữ nguyên logic ngOnInit)
    this.isLoading = true;
    forkJoin({
      user: this.authService.getCurrentUser(),
      branches: this.appointmentService.getBranchesSimple(), 
      doctors: this.appointmentService.getDoctors(),
      specialties: this.appointmentService.getSpecialties() // (MỚI) Tải chuyên khoa
    }).subscribe({
      next: ({ user, branches, doctors, specialties }) => {
        this.currentUser = user;
        if (user) {
          this.bookingForm.patchValue({ patientName: user.fullName });
        }
        this.allBranchesList = branches;
        this.branches$ = of(branches);
        this.allDoctors = doctors;
        
        // (MỚI) Xử lý chuyên khoa (lọc trùng lặp)
        const uniqueSpecialties = this.filterUniqueSpecialties(specialties);
        this.specialties$ = of(uniqueSpecialties);

        // Lắng nghe thay đổi form để lọc bác sĩ (giờ đã bao gồm specialtyFilter)
        this.filteredDoctors$ = this.bookingForm.valueChanges.pipe(
          startWith(this.bookingForm.value),
          map(formValue => this.applyFilters(formValue))
        );

        this.isLoading = false;
      },
      error: (err) => {
        this.toastr.error('Không thể tải dữ liệu cần thiết. Vui lòng thử lại.');
        console.error("Lỗi trong ngOnInit:", err);
        this.isLoading = false;
      }
    });
  }

  
  private filterUniqueSpecialties(specialties: SpecialtyDto[]): SpecialtyDto[] {
    // (Giữ nguyên logic filterUniqueSpecialties)
    const seen = new Set<string>();
    // Dựa trên response API của bạn, 'name' là trường chúng ta muốn
    return specialties.filter(spec => {
      const isDuplicate = seen.has(spec.name);
      seen.add(spec.name);
      return !isDuplicate;
    });
  }

  
  applyFilters(filters: any): DoctorDto[] {
    // (Giữ nguyên logic applyFilters)
    let doctors = [...this.allDoctors];

    // Lọc theo tên
    if (filters.doctorNameFilter) {
      const nameLower = filters.doctorNameFilter.toLowerCase();
      doctors = doctors.filter(doc => doc.fullName.toLowerCase().includes(nameLower));
    }
    // Lọc theo chi nhánh
    if (filters.branchFilter) {
      doctors = doctors.filter(doc => doc.branchId === filters.branchFilter);
    }
    // (MỚI) Lọc theo chuyên khoa
    if (filters.specialtyFilter) {
      // Giả định API /users/doctors trả về specialty là string (ví dụ: "Nha khoa Tổng quát")
      // Và giá trị của filter là spec.name (ví dụ: "Nha khoa Tổng quát")
      doctors = doctors.filter(doc => doc.specialty === filters.specialtyFilter);
    }

    return doctors;
  }

  
  selectDoctor(doctor: DoctorDto) {
    // (Giữ nguyên logic selectDoctor)
    if (this.selectedDoctor?.id === doctor.id) return;
    this.selectedDoctor = doctor;
    this.selectedDate = null; 
    this.selectedTime = null; 
    this.availableSlots = []; 
    this.calendarDays = this.generateCalendarDays(7);
    console.log("Đã chọn bác sĩ:", doctor);
  }

  
  generateCalendarDays(numberOfDays: number): { date: string, label: string, dayOfWeek: string }[] {
    // (Giữ nguyên logic generateCalendarDays)
    const days = [];
    const today = new Date();
    const locale = 'vi-VN'; 
    for (let i = 0; i < numberOfDays; i++) {
      const date = new Date(today);
      date.setDate(today.getDate() + i);
      days.push({
        date: formatDate(date, 'yyyy-MM-dd', 'en-US'),
        label: formatDate(date, 'dd/MM', locale),
        dayOfWeek: formatDate(date, 'EEE', locale)
      });
    }
    return days;
  }

  
  selectDate(date: string) {
    // (Giữ nguyên logic selectDate)
    if (!this.selectedDoctor || this.selectedDate === date) return;

    this.selectedDate = date;
    this.selectedTime = null;
    this.availableSlots = [];
    this.isLoadingSlots = true;

    console.log(`Đã chọn ngày ${formatDate(date, 'dd/MM/yyyy', 'en-US')}, đang tải giờ trống...`);

    this.appointmentService.getAvailableSlots(this.selectedDoctor.id, date)
      .pipe(
        catchError(err => {
          this.toastr.error(`Không thể tải giờ trống cho ngày ${formatDate(date, 'dd/MM/yyyy', 'en-US')}.`);
          console.error("Lỗi getAvailableSlots:", err);
          return of([]);
        })
      )
      .subscribe(slots => {
        this.availableSlots = slots; 
        this.isLoadingSlots = false;
        console.log(`Giờ trống cho ngày ${formatDate(date, 'dd/MM/yyyy', 'en-US')}:`, slots);
      });
  }

  
  selectTime(time: string) {
    // (Giữ nguyên logic selectTime)
    this.selectedTime = time;
  }

  
  getBranchDetails(branchId: string): BranchSimpleDto | undefined {
    // (Giữ nguyên logic getBranchDetails)
    return this.allBranchesList.find(b => b.id === branchId);
  }
  
  
  openConfirmModal() {
    // (Giữ nguyên logic openConfirmModal)
    if (!this.currentUser || !this.selectedDoctor || !this.selectedDate || !this.selectedTime) {
      this.toastr.error('Vui lòng chọn đầy đủ thông tin bác sĩ, ngày và giờ khám.');
      return;
    }
    if (this.bookingForm.get('reason')?.invalid) {
      this.toastr.warning('Vui lòng nhập lý do khám / triệu chứng.');
      this.bookingForm.get('reason')?.markAsTouched();
      return;
    }
    this.isConfirmModalOpen = true;
    console.log("Mở dialog tóm tắt.");
  }

  
  closeConfirmModal() {
    // (Giữ nguyên logic closeConfirmModal)
    if (!this.isBooking) { 
      this.isConfirmModalOpen = false;
    }
  }

  /**
   * (ĐÃ CẬP NHẬT) Gọi API đặt lịch VÀ API thanh toán
   */
  onBookAppointment() {
    if (!this.selectedDoctor || !this.selectedDate || !this.selectedTime) return;

    this.isBooking = true; 
    this.toastr.info('Đang tạo lịch hẹn...');

    const appointmentTimeLocal = `${this.selectedDate}T${this.selectedTime}+07:00`;

    const payload = {
      doctorId: this.selectedDoctor.id,
      branchId: this.selectedDoctor.branchId,
      appointmentTime: appointmentTimeLocal,
      notes: this.bookingForm.get('reason')?.value || ''
    };

    console.log('Bước 1: Chuẩn bị đặt lịch PENDING:', payload);

    // --- (LOGIC MỚI) ---
    // Bước 1: Gọi API tạo lịch hẹn (PENDING)
    this.appointmentService.bookAppointment(payload).pipe(
      tap(bookResponse => {
        console.log('Bước 1: Đặt lịch PENDING thành công:', bookResponse);
        this.toastr.info('Đang tạo link thanh toán...');
      }),
      // Chuyển sang Bước 2: Dùng ID từ Bước 1 để gọi API thanh toán
      switchMap(bookResponse => {
        const appointmentId = bookResponse.id; // Lấy ID từ phản hồi
        if (!appointmentId) {
          // Ném lỗi để catchError bắt
          throw new Error('Không nhận được ID lịch hẹn từ backend.');
        }
        console.log(`Bước 2: Gọi API createPayment với ID: ${appointmentId}`);
        // Gọi hàm createPayment (đã thêm ở service)
        return this.appointmentService.createPayment(appointmentId); 
      }),
      // Xử lý lỗi (cho cả 2 bước)
      catchError(err => {
        this.isBooking = false; // Tắt spinner
        
        // (SỬA LỖI PARSING) Xử lý trường hợp API trả về text (URL) nhưng HttpClient cố parse JSON
        // (Đây là lỗi 'SyntaxError: Unexpected token 'h'...' từ screenshot image_528dfe.png)
        if (err.status === 200 && err.error?.text && err.statusText === 'OK') {
           console.warn('Lỗi parsing đã được xử lý (API trả về text), đang lấy URL từ text...');
           return of(err.error.text); // Trả về chuỗi text (URL)
        }

        // Nếu là lỗi thật (400, 500...)
        const errorMsg = err.error?.message || err.error?.error || err.message || 'Lỗi không xác định';
        this.toastr.error(`Xử lý thất bại: ${errorMsg}`);
        console.error('Lỗi trong chuỗi đặt lịch/thanh toán:', err);
        return of(null); // Dừng chuỗi, trả về null
      })
    ).subscribe((paymentUrl: string | null) => { // (SỬA) Nhận về string hoặc null
      // (CHỈ CHẠY NẾU CẢ 2 BƯỚC THÀNH CÔNG)
      this.isBooking = false;
      
      if (paymentUrl && typeof paymentUrl === 'string' && paymentUrl.startsWith('http')) {
        // Bước 3: Chuyển hướng đến trang thanh toán
        console.log('Bước 3: Nhận được link thanh toán. Đang chuyển hướng...');
        this.toastr.success('Đã tạo lịch, đang chuyển đến trang thanh toán.');
        window.location.href = paymentUrl; // Chuyển hướng
      } else if (paymentUrl) {
         // Vẫn thành công nhưng không có URL (lỗi logic)
        console.error('Lỗi: API createPayment thành công nhưng không trả về URL hợp lệ.', paymentUrl);
        this.toastr.error('Không thể lấy link thanh toán, vui lòng thử lại.');
      }
      // Nếu là null (do catchError), không làm gì cả (lỗi đã được hiển thị)
    });
    // --- (KẾT THÚC LOGIC MỚI) ---
  }
}