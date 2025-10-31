import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';

// --- CÁC INTERFACE DTO (Giữ nguyên) ---
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

@Injectable({
  providedIn: 'root'
})
export class AppointmentService {

  // Gateway
  private BASE_URL = 'http://localhost:8080';

  constructor(private http: HttpClient) { }

  // --- CÁC HÀM GET (Giữ nguyên) ---
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

  // --- CÁC HÀM POST/GET CHO LUỒNG ĐẶT LỊCH ---

  /**
   * (Bước 1) API TẠO LỊCH HẸN MỚI
   */
  bookAppointment(payload: any): Observable<any> { 
    return this.http.post(`${this.BASE_URL}/appointments`, payload);
  }

  /**
   * (ĐÃ SỬA) (Bước 2) Tạo link thanh toán (mong đợi TEXT)
   */
  createPayment(appointmentId: string): Observable<string> { // <-- SỬA 1: Kiểu trả về là string
    return this.http.post(
      `${this.BASE_URL}/api/v1/payment/create-payment/${appointmentId}`,
      {},
      { responseType: 'text' } // <-- SỬA 2: Báo cho Angular biết đây là text
    );
  }

  /**
   * (MỚI - Bước 4) Gửi tham số VNPay về backend để xác thực
   * @param allParams Tất cả tham số từ URL VNPay trả về
   */
  confirmVnPayReturn(allParams: any): Observable<any> {
    // Chuyển object params thành HttpParams
    let params = new HttpParams();
    for (const key in allParams) {
      if (allParams.hasOwnProperty(key)) {
        params = params.set(key, allParams[key]);
      }
    }
    
    // Gọi GET /api/v1/payment/vnpay-return với các tham số
    // (Interceptor sẽ tự đính kèm token)
    // (API này trả về JSON, không phải text)
    return this.http.get(`${this.BASE_URL}/api/v1/payment/vnpay-return`, { params });
    
  }
}