import { Component, OnInit } from '@angular/core';
import { CommonModule, formatDate } from '@angular/common'; 
import { Observable, forkJoin, map, startWith, of, tap, catchError, switchMap, finalize } from 'rxjs'; 
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { AuthService, UserDto } from '../../../core/services/auth';
import { AppointmentService, BranchSimpleDto, DoctorDto, SpecialtyDto } from '../../../core/services/AppointmentService'; 
import { ToastrService } from 'ngx-toastr';

// ✅ DANH SÁCH CÁC CA LÀM VIỆC CỐ ĐỊNH
const ALL_TIME_SLOTS = [
  '08:00:00',
  '08:15:00',
  '08:30:00',
  '08:45:00',
  '09:00:00',
  '09:15:00',
  '09:30:00',
  '09:45:00',
  '10:00:00',
  '10:15:00',
  '13:00:00',
  '13:15:00',
  '13:30:00',
  '13:45:00',
  '14:00:00',
  '14:15:00',
  '14:30:00',
  '14:45:00',
  '15:00:00',
  '15:15:00'
];

// ✅ Helper function: Format time để hiển thị (HH:mm)
function formatTimeSlot(time: string): string {
  return time.substring(0, 5); // '08:00:00' → '08:00'
}

// ✅ Interface cho slot với trạng thái
interface TimeSlot {
  time: string;           // '08:00:00' - Giá trị gốc
  displayTime: string;    // '08:00' - Hiển thị UI
  isAvailable: boolean;   // true = trống, false = đã đặt
}

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
  
  // ✅ Danh sách slots đầy đủ với trạng thái
  timeSlots: TimeSlot[] = [];
  selectedTime: string | null = null;

  // Trạng thái dialog
  isConfirmModalOpen: boolean = false;
  isBooking: boolean = false;

  constructor(
    private authService: AuthService,
    private appointmentService: AppointmentService,
    private toastr: ToastrService
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
        console.error("❌ Lỗi trong ngOnInit:", err);
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
    this.timeSlots = []; 
    this.calendarDays = this.generateCalendarDays(7);
    console.log("✅ Đã chọn bác sĩ:", doctor);
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

  /**
   * ✅ Chọn ngày và load slots
   * - Gọi API getAvailableSlots
   * - So sánh với ALL_TIME_SLOTS để tạo danh sách đầy đủ
   */
  selectDate(date: string) {
    if (!this.selectedDoctor || this.selectedDate === date) return;

    this.selectedDate = date;
    this.selectedTime = null;
    this.timeSlots = [];
    this.isLoadingSlots = true;

    console.log(`✅ Đã chọn ngày ${formatDate(date, 'dd/MM/yyyy', 'en-US')}`);
    console.log(`🔄 Gọi API: /slots/available?doctorId=${this.selectedDoctor.id}&date=${date}`);

    this.appointmentService.getAvailableSlots(this.selectedDoctor.id, date).pipe(
      finalize(() => {
        this.isLoadingSlots = false;
        console.log('✅ Hoàn tất load slots');
      }),
      catchError(err => {
        this.toastr.error(`Không thể tải giờ trống cho ngày ${formatDate(date, 'dd/MM/yyyy', 'en-US')}.`);
        console.error("❌ Lỗi load slots:", err);
        return of([]);
      })
    ).subscribe((availableSlots: string[]) => {
      console.log('✅ API Response - Available slots:', availableSlots);

      // Tạo Set để check nhanh
      const availableSet = new Set(availableSlots);
      console.log('✅ Available Set:', Array.from(availableSet));

      // Map ALL_TIME_SLOTS với trạng thái isAvailable
      this.timeSlots = ALL_TIME_SLOTS.map(time => {
        const isAvailable = availableSet.has(time);
        return {
          time,
          displayTime: formatTimeSlot(time),
          isAvailable
        };
      });

      const availableCount = this.timeSlots.filter(s => s.isAvailable).length;
      const bookedCount = this.timeSlots.length - availableCount;

      console.log('✅ Processed slots:', this.timeSlots);
      console.log(`📊 Tổng: ${this.timeSlots.length} ca | Trống: ${availableCount} ca | Đã đặt: ${bookedCount} ca`);

      if (this.timeSlots.length === 0) {
        console.warn('⚠️ Không có ca làm việc trong ngày này');
      } else if (availableCount === 0) {
        console.warn('⚠️ Tất cả các ca đã được đặt hết');
        this.toastr.warning('Tất cả các ca trong ngày này đã đầy', 'Thông báo');
      }
    });
  }

  /**
   * ✅ Lấy slots buổi sáng (08:00 - 10:15)
   */
  getMorningSlots(): TimeSlot[] {
    return this.timeSlots.filter(slot => {
      const hour = parseInt(slot.time.split(':')[0]);
      return hour >= 8 && hour < 13;
    });
  }

  /**
   * ✅ Lấy slots buổi chiều (13:00 - 15:15)
   */
  getAfternoonSlots(): TimeSlot[] {
    return this.timeSlots.filter(slot => {
      const hour = parseInt(slot.time.split(':')[0]);
      return hour >= 13;
    });
  }

  /**
   * ✅ Chọn giờ khám
   * - Chỉ cho phép chọn slot available
   */
  selectTime(time: string, isAvailable: boolean) {
    if (!isAvailable) {
      this.toastr.warning('Khung giờ này không khả dụng. Vui lòng chọn giờ khác.', 'Thông báo');
      return;
    }
    this.selectedTime = time;
    console.log('✅ Đã chọn giờ:', time, '→ Display:', formatTimeSlot(time));
  }

  /**
   * ✅ Đếm số ca còn trống
   */
  getAvailableSlotsCount(): number {
    return this.timeSlots.filter(slot => slot.isAvailable).length;
  }

  /**
   * ✅ Hiển thị giờ đã chọn (HH:mm)
   */
  getDisplaySelectedTime(): string {
    return this.selectedTime ? formatTimeSlot(this.selectedTime) : '';
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
    console.log("✅ Mở modal xác nhận");
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

    console.log('✅ Payload đặt lịch:', payload);

    this.appointmentService.bookAppointment(payload).pipe(
      tap(bookResponse => {
        console.log('✅ Đặt lịch thành công:', bookResponse);
        this.toastr.info('Đang tạo link thanh toán...');
      }),
      switchMap(bookResponse => {
        const appointmentId = bookResponse.id;
        if (!appointmentId) {
          throw new Error('Không nhận được ID lịch hẹn từ backend.');
        }
        console.log(`✅ Gọi API createPayment với ID: ${appointmentId}`);
        return this.appointmentService.createPayment(appointmentId); 
      }),
      catchError(err => {
        this.isBooking = false;
        
        if (err.status === 200 && err.error?.text && err.statusText === 'OK') {
           console.warn('⚠️ Lỗi parsing (API trả về text), lấy URL từ text...');
           return of(err.error.text);
        }

        const errorMsg = err.error?.message || err.error?.error || err.message || 'Lỗi không xác định';
        this.toastr.error(`Xử lý thất bại: ${errorMsg}`);
        console.error('❌ Lỗi:', err);
        return of(null);
      })
    ).subscribe((paymentUrl: string | null) => {
      this.isBooking = false;
      
      if (paymentUrl && typeof paymentUrl === 'string' && paymentUrl.startsWith('http')) {
        console.log('✅ Nhận được link thanh toán, chuyển hướng...');
        this.toastr.success('Đã tạo lịch, đang chuyển đến trang thanh toán.');
        window.location.href = paymentUrl;
      } else if (paymentUrl) {
        console.error('❌ API không trả về URL hợp lệ:', paymentUrl);
        this.toastr.error('Không thể lấy link thanh toán, vui lòng thử lại.');
      }
    });
  }
}