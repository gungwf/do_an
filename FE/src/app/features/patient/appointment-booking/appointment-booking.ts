import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { CommonModule, formatDate } from '@angular/common'; 
import { Observable, forkJoin, map, startWith, of, tap, catchError, switchMap, finalize } from 'rxjs'; 
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { AuthService, UserDto } from '../../../core/services/auth';
import { AppointmentService, BranchSimpleDto, DoctorDto, SpecialtyDto } from '../../../core/services/AppointmentService'; 
import { ToastrService } from 'ngx-toastr';
import { ChatService } from '../../../core/services/chat.service';

// ✅ THÊM MỚI: Interface cho slot với trạng thái booked
interface TimeSlot {
  time: string;
  isBooked: boolean;
}

@Component({
  selector: 'app-appointment-booking',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './appointment-booking.html',
  styleUrl: './appointment-booking.scss'
})
export class AppointmentBooking implements OnInit {
  @Output() chatWithDoctor = new EventEmitter<string>();

  currentUser: UserDto | null = null;
  allBranchesList: BranchSimpleDto[] = [];
  branches$: Observable<BranchSimpleDto[]> = of([]);
  allDoctors: DoctorDto[] = [];
  filteredDoctors$: Observable<DoctorDto[]> = of([]);
  specialties$: Observable<SpecialtyDto[]> = of([]); 

  bookingForm = new FormGroup({
    patientName: new FormControl({ value: '', disabled: true }, Validators.required),
    reason: new FormControl('', Validators.required),
    doctorNameFilter: new FormControl(''),
    branchFilter: new FormControl(''),
    specialtyFilter: new FormControl(''),
  });

  isLoading = true;

  // Trạng thái logic lịch hẹn
  selectedDoctor: DoctorDto | null = null;
  calendarDays: { date: string, label: string, dayOfWeek: string }[] = [];
  selectedDate: string | null = null; 
  isLoadingSlots = false;
  
  // ✅ ĐỔI: Từ string[] sang TimeSlot[]
  availableSlots: TimeSlot[] = [];
  selectedTime: string | null = null; 

  // Trạng thái dialog
  isConfirmModalOpen: boolean = false;
  isBooking: boolean = false;

  constructor(
    private authService: AuthService,
    private appointmentService: AppointmentService,
    private toastr: ToastrService,
    private chatService: ChatService
  ) {}

  ngOnInit(): void {
    this.isLoading = true;
    forkJoin({
      user: this.authService.getCurrentUser(),
      branches: this.appointmentService.getBranchesSimple(), 
      doctors: this.appointmentService.getDoctors(),
      specialties: this.appointmentService.getSpecialties()
    }).subscribe({
      next: ({ user, branches, doctors, specialties }) => {
        this.currentUser = user;
        if (user) {
          this.bookingForm.patchValue({ patientName: user.fullName });
        }
        this.allBranchesList = branches;
        this.branches$ = of(branches);
        this.allDoctors = doctors;
        
        const uniqueSpecialties = this.filterUniqueSpecialties(specialties);
        this.specialties$ = of(uniqueSpecialties);

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
    const seen = new Set<string>();
    return specialties.filter(spec => {
      const isDuplicate = seen.has(spec.name);
      seen.add(spec.name);
      return !isDuplicate;
    });
  }

  applyFilters(filters: any): DoctorDto[] {
    let doctors = [...this.allDoctors];

    if (filters.doctorNameFilter) {
      const nameLower = filters.doctorNameFilter.toLowerCase();
      doctors = doctors.filter(doc => doc.fullName.toLowerCase().includes(nameLower));
    }
    if (filters.branchFilter) {
      doctors = doctors.filter(doc => doc.branchId === filters.branchFilter);
    }
    if (filters.specialtyFilter) {
      doctors = doctors.filter(doc => doc.specialty === filters.specialtyFilter);
    }

    return doctors;
  }

  selectDoctor(doctor: DoctorDto) {
    if (this.selectedDoctor?.id === doctor.id) return;
    this.selectedDoctor = doctor;
    this.selectedDate = null; 
    this.selectedTime = null; 
    this.availableSlots = []; 
    this.calendarDays = this.generateCalendarDays(7);
    console.log("Đã chọn bác sĩ:", doctor);
  }

  generateCalendarDays(numberOfDays: number): { date: string, label: string, dayOfWeek: string }[] {
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

  // ✅ CẬP NHẬT: Chỉ load available slots (bỏ gọi booked)
  selectDate(date: string) {
    if (!this.selectedDoctor || this.selectedDate === date) return;

    this.selectedDate = date;
    this.selectedTime = null;
    this.availableSlots = [];
    this.isLoadingSlots = true;

    console.log(`Đã chọn ngày ${formatDate(date, 'dd/MM/yyyy', 'en-US')}, đang tải giờ...`);

    this.appointmentService.getAvailableSlots(this.selectedDoctor.id, date).pipe(
      catchError(err => {
        console.error('Lỗi getAvailableSlots:', err);
        this.toastr.error(`Không thể tải giờ trống cho ngày ${formatDate(date, 'dd/MM/yyyy', 'en-US')}.`);
        return of([] as string[]);
      }),
      finalize(() => this.isLoadingSlots = false)
    ).subscribe((available: string[]) => {
      console.log('Available slots:', available);
      // Khi chỉ có available, đánh dấu tất cả là chưa booked
      this.availableSlots = (available || []).map(time => ({ time, isBooked: false }));
      console.log('Processed slots:', this.availableSlots);
    });
  }

  // ✅ THÊM MỚI: Lấy slots buổi sáng (7:00 - 12:59)
  getMorningSlots(): TimeSlot[] {
    return this.availableSlots.filter(slot => {
      const hour = parseInt(slot.time.split(':')[0]);
      return hour >= 7 && hour < 13;
    });
  }

  // ✅ THÊM MỚI: Lấy slots buổi chiều (13:00 - 18:00)
  getAfternoonSlots(): TimeSlot[] {
    return this.availableSlots.filter(slot => {
      const hour = parseInt(slot.time.split(':')[0]);
      return hour >= 13 && hour <= 18;
    });
  }

  // ✅ CẬP NHẬT: Chỉ cho phép chọn slot chưa bị book
  selectTime(time: string, isBooked: boolean) {
    if (isBooked) {
      this.toastr.warning('Khung giờ này đã có người đặt. Vui lòng chọn giờ khác.', 'Thông báo');
      return;
    }
    this.selectedTime = time;
  }

  getBranchDetails(branchId: string): BranchSimpleDto | undefined {
    return this.allBranchesList.find(b => b.id === branchId);
  }

  openConfirmModal() {
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
    if (!this.isBooking) { 
      this.isConfirmModalOpen = false;
    }
  }

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

    this.appointmentService.bookAppointment(payload).pipe(
      tap(bookResponse => {
        console.log('Bước 1: Đặt lịch PENDING thành công:', bookResponse);
        this.toastr.info('Đang tạo link thanh toán...');
      }),
      switchMap(bookResponse => {
        const appointmentId = bookResponse.id;
        if (!appointmentId) {
          throw new Error('Không nhận được ID lịch hẹn từ backend.');
        }
        console.log(`Bước 2: Gọi API createPayment với ID: ${appointmentId}`);
        return this.appointmentService.createPayment(appointmentId); 
      }),
      catchError(err => {
        this.isBooking = false;
        
        if (err.status === 200 && err.error?.text && err.statusText === 'OK') {
           console.warn('Lỗi parsing đã được xử lý (API trả về text), đang lấy URL từ text...');
           return of(err.error.text);
        }

        const errorMsg = err.error?.message || err.error?.error || err.message || 'Lỗi không xác định';
        this.toastr.error(`Xử lý thất bại: ${errorMsg}`);
        console.error('Lỗi trong chuỗi đặt lịch/thanh toán:', err);
        return of(null);
      })
    ).subscribe((paymentUrl: string | null) => {
      this.isBooking = false;
      
      if (paymentUrl && typeof paymentUrl === 'string' && paymentUrl.startsWith('http')) {
        console.log('Bước 3: Nhận được link thanh toán. Đang chuyển hướng...');
        this.toastr.success('Đã tạo lịch, đang chuyển đến trang thanh toán.');
        window.location.href = paymentUrl;
      } else if (paymentUrl) {
        console.error('Lỗi: API createPayment thành công nhưng không trả về URL hợp lệ.', paymentUrl);
        this.toastr.error('Không thể lấy link thanh toán, vui lòng thử lại.');
      }
    });
  }

  /**
   * Bắt đầu chat với bác sĩ
   */
  startChat(doctorId: string): void {
    // Trigger mở chat bubble và tạo room với bác sĩ
    this.chatService.triggerOpenChatWith(doctorId);
    this.toastr.success('Đang mở cửa sổ chat...');
  }
}