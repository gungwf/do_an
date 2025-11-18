import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';

// ------------------------------------
// --- CÁC INTERFACE DTO (DATA TRANSFER OBJECTS) ---
// ------------------------------------

export interface BranchSimpleDto {
  id: string;
  branchName: string;
  address?: string; // Dữ liệu từ API /branches/simple có thể không có
  phoneNumber?: string;
}

export interface DoctorDto {
  id: string;
  fullName: string;
  email?: string;
  phoneNumber?: string;
  role?: string;
  branchId: string;
  active?: boolean;
  specialty?: string;
  price?: number;
}

export interface SpecialtyDto {
  id: string;
  name: string;
}

export interface PaymentResponse {
  paymentUrl: string;
}

export interface AppointmentResponseDto {
  id: string;
  appointmentTime: string; 
  status: 'PENDING' | 'CONFIRMED' | 'CANCELED' | 'COMPLETED' | 'PENDING_PAYMENT'; // Thêm status
  notes: string;
  priceAtBooking: number;
  patient: { id: string; fullName: string; email: string; };
  doctor: { id: string; fullName: string; };
  branch: { id: string; branchName: string; address: string; };
}

// --- INTERFACE MỚI CHO ADMIN SEARCH ---
export interface AppointmentSearchDto {
  page: number;
  size: number;
  patientName?: string | null;
  doctorName?: string | null;
  status?: string | null;
  branchId?: string | null;
  startTime?: string | null;
  endTime?: string | null;
  sort?: string | null;
}

// --- INTERFACE MỚI CHO ADMIN SEARCH RESPONSE ---
export interface AppointmentSearchResponse {
  content: AppointmentResponseDto[]; 
  totalElements: number;
  totalPages: number;
  // ... các trường phân trang khác
}

// ------------------------------------
// --- SERVICE CHÍNH ---
// ------------------------------------

@Injectable({
  providedIn: 'root'
})
export class AppointmentService {

  // Gateway
  private BASE_URL = 'http://localhost:8080';

  constructor(private http: HttpClient) { }

  // ------------------------------------
  // --- CÁC HÀM GET DỮ LIỆU CHUNG ---
  // ------------------------------------

  getBranchesSimple(): Observable<BranchSimpleDto[]> {
    // *** ĐÃ SỬA: Sửa API path thành /branches/simple ***
    return this.http.get<BranchSimpleDto[]>(`${this.BASE_URL}/branches/simple`);
  }

  getDoctors(): Observable<DoctorDto[]> {
    return this.http.get<DoctorDto[]>(`${this.BASE_URL}/users/doctors`);
  }

  getSpecialties(): Observable<SpecialtyDto[]> {
    return this.http.get<SpecialtyDto[]>(`${this.BASE_URL}/doctor-profiles/specialties`);
  }

  getAvailableSlots(doctorId: string, date: string): Observable<string[]> {
    let params = new HttpParams()
      .set('doctorId', doctorId)
      .set('date', date);
    return this.http.get<string[]>(`${this.BASE_URL}/slots/available`, { params });
  }

  // ------------------------------------
  // --- CÁC HÀM CỦA BỆNH NHÂN ---
  // ------------------------------------

  bookAppointment(payload: any): Observable<any> { 
    return this.http.post(`${this.BASE_URL}/appointments`, payload);
  }

  createPayment(appointmentId: string): Observable<string> { 
    return this.http.post(
      `${this.BASE_URL}/api/v1/payment/create-payment/${appointmentId}`,
      {},
      { responseType: 'text' }
    );
  }

  confirmVnPayReturn(allParams: any): Observable<any> {
    let params = new HttpParams();
    for (const key in allParams) {
      if (allParams.hasOwnProperty(key)) {
        params = params.set(key, allParams[key]);
      }
    }
    return this.http.get(`${this.BASE_URL}/api/v1/payment/vnpay-return`, { params });
  }

  getMyAppointments(patientId: string): Observable<AppointmentResponseDto[]> {
    return this.http.get<AppointmentResponseDto[]>(`${this.BASE_URL}/appointments/patient/${patientId}`);
  }
  
  // ------------------------------------
  // --- HÀM MỚI CHO BÁC SĨ ---
  // ------------------------------------

  /**
   * [DOCTOR] Lấy danh sách lịch hẹn của một bác sĩ cụ thể
   * API: GET /appointments/doctor/{doctorId}
   */
  getDoctorAppointments(doctorId: string): Observable<AppointmentResponseDto[]> {
    // Hàm này sẽ sử dụng AppointmentResponseDto vì cấu trúc JSON trả về là tương tự
    return this.http.get<AppointmentResponseDto[]>(`${this.BASE_URL}/appointments/doctor/${doctorId}`);
  }

  // ------------------------------------
  // --- HÀM MỚI CHO ADMIN ---
  // ------------------------------------
  
  /**
   * [ADMIN] Tìm kiếm, lọc và phân trang tất cả cuộc hẹn
   * API: POST /appointments/search
   */
  searchAppointments(filters: AppointmentSearchDto): Observable<AppointmentSearchResponse> {
    return this.http.post<AppointmentSearchResponse>(`${this.BASE_URL}/appointments/search`, filters);
  }

  getAppointmentById(appointmentId: string): Observable<AppointmentResponseDto> {
    return this.http.get<AppointmentResponseDto>(`${this.BASE_URL}/appointments/${appointmentId}`);
  }
}