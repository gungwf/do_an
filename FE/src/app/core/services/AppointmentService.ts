import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';

// ------------------------------------
// --- CÁC INTERFACE DTO (DATA TRANSFER OBJECTS) ---
// ------------------------------------

export interface BranchSimpleDto {
  id: string;
  branchName: string;
  address?: string;
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

// ✅ CẬP NHẬT: Thêm 3 field mới cho prescription dialog
export interface AppointmentResponseDto {
  id: string;
  appointmentTime: string; 
  status: 'PENDING' | 'CONFIRMED' | 'CANCELED' | 'COMPLETED' | 'PENDING_PAYMENT' | 'CREATED_MEDICAL_RECORD' | 'PENDING_BILLING' | 'PAID_SERVICE';
  notes: string;
  priceAtBooking: number;
  medicalRecordId?: string;  // ✅ THÊM MỚI - ID của bệnh án
  diagnosis?: string;          // ✅ THÊM MỚI - Chẩn đoán hiện tại
  icd10Code?: string;          // ✅ THÊM MỚI - Mã ICD-10 hiện tại
  patient: { id: string; fullName: string; email: string; };
  doctor: { id: string; fullName: string; };
  branch: { id: string; branchName: string; address: string; };
}

// --- INTERFACE CHO ADMIN SEARCH ---
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

// --- INTERFACE CHO ADMIN SEARCH RESPONSE ---
export interface AppointmentSearchResponse {
  content: AppointmentResponseDto[]; 
  totalElements: number;
  totalPages: number;
  // ... các trường phân trang khác
}

// === 1. DTO CHO DOCTOR SEARCH REQUEST ===
export interface DoctorAppointmentSearchDto {
  page: number;
  size: number;
  sort: string;
  searchText: string | null;
  status: string | null;
}

// === 2. DTO CHO DOCTOR SEARCH RESPONSE (Page) ===
export interface PagedAppointmentResponse {
  content: AppointmentResponseDto[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number; // Trang hiện tại (bắt đầu từ 0)
  // (Bạn có thể thêm 'first', 'last', 'empty' nếu cần)
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
  // --- HÀM CỦA BÁC SĨ ---
  // ------------------------------------

  /**
   * [DOCTOR] Lấy danh sách lịch hẹn (Phân trang, Lọc, Sắp xếp)
   * API: POST /appointments/doctor/{doctorId}/appointments
   */
  getDoctorAppointments(
    doctorId: string, 
    payload: DoctorAppointmentSearchDto
  ): Observable<PagedAppointmentResponse> {
    return this.http.post<PagedAppointmentResponse>(
      `${this.BASE_URL}/appointments/doctor/${doctorId}/appointments`, 
      payload
    );
  }

  // ------------------------------------
  // --- HÀM CỦA ADMIN ---
  // ------------------------------------
  
  /**
   * [ADMIN] Tìm kiếm, lọc và phân trang tất cả cuộc hẹn
   * API: POST /appointments/search
   */
  searchAppointments(filters: AppointmentSearchDto): Observable<AppointmentSearchResponse> {
    return this.http.post<AppointmentSearchResponse>(`${this.BASE_URL}/appointments/search`, filters);
  }

  // ------------------------------------
  // --- HÀM CHUNG ---
  // ------------------------------------
  
  getAppointmentById(appointmentId: string): Observable<AppointmentResponseDto> {
    return this.http.get<AppointmentResponseDto>(`${this.BASE_URL}/appointments/${appointmentId}`);
  }
}