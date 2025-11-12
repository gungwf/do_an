import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';

// ------------------------------------
// --- CÁC INTERFACE DTO (DATA TRANSFER OBJECTS) ---
// ------------------------------------

export interface BranchSimpleDto {
  id: string;
  branchName: string;
  address: string;
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

/**
 * DTO cho một lịch hẹn (được trả về từ API /appointments/patient/{patientId})
 */
export interface AppointmentResponseDto {
  id: string;
  appointmentTime: string; // "2025-10-10T03:00:00Z"
  status: 'PENDING' | 'CONFIRMED' | 'CANCELED' | 'COMPLETED';
  notes: string;
  priceAtBooking: number;
  patient: { id: string; fullName: string; email: string; };
  doctor: { id: string; fullName: string; };
  branch: { id: string; branchName: string; address: string; };
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
    return this.http.get<BranchSimpleDto[]>(`${this.BASE_URL}/branches`);
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
  // --- CÁC HÀM XỬ LÝ LỊCH HẸN VÀ THANH TOÁN ---
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

  // ------------------------------------
  // --- HÀM MỚI: LẤY LỊCH HẸN CỦA TÔI ---
  // ------------------------------------

  /**
   * Lấy danh sách lịch hẹn của bệnh nhân dựa trên ID.
   * API: /appointments/patient/{patientId}
   * @param patientId ID của bệnh nhân
   */
  getMyAppointments(patientId: string): Observable<AppointmentResponseDto[]> {
    // Gọi API bạn đã cung cấp (qua Gateway)
    return this.http.get<AppointmentResponseDto[]>(`${this.BASE_URL}/appointments/patient/${patientId}`);
  }
  
  // ------------------------------------
  // --- HÀM TODO: HỦY LỊCH HẸN ---
  // ------------------------------------
  
  /**
   * TODO: Triển khai API hủy lịch hẹn nếu có
   * Ví dụ: return this.http.delete(`${this.BASE_URL}/appointments/${appointmentId}`);
   */
  // cancelAppointment(appointmentId: string): Observable<any> {
  //   // return this.http.post(`${this.BASE_URL}/appointments/${appointmentId}/cancel`, {});
  // }
}